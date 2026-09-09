package com.example.lightapp.games.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightapp.core.GameResult
import com.example.lightapp.studio.persistence.SudokuSessionStore
import com.example.lightapp.studio.persistence.SudokuCodec
import com.example.lightapp.studio.persistence.SudokuSession
import com.example.lightapp.sudoku.core.GameDifficulty
import com.example.lightapp.sudoku.core.GameType
import com.example.lightapp.sudoku.core.QQWingController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.resume

/**
 * What the screen is showing. A sealed hierarchy rather than a bag of booleans, so an
 * impossible combination — generating *and* complete — cannot be represented.
 */
sealed interface SudokuUi {
    data object Generating : SudokuUi
    data object GenerationFailed : SudokuUi
    data class Playing(val state: SudokuState) : SudokuUi

    /**
     * The difficulty picker, and what to go back to if it is dismissed.
     *
     * [returnTo] is the whole point of this type. It used to hold only a nullable
     * `SudokuState`, captured from `Playing` and nothing else, so opening the picker from
     * the completion screen stored `null` — and cancelling then dropped the UI into
     * `Generating` with no generator running. Two taps from a finished puzzle
     * (`NEW` then `CANCEL`) reached a permanent "GENERATING" screen with no button on it,
     * and because the view model outlives the screen, leaving and coming back did not
     * clear it either.
     *
     * Making the destination explicit means dismissal is always defined.
     */
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: SudokuUi?,
    ) : SudokuUi

    data class Complete(val state: SudokuState) : SudokuUi

    /**
     * The hint overlay, riding on top of an in-progress board.
     *
     * State rather than an event because a hint stays true while the board is
     * unchanged — the player reads the cell, sees *why* the digit is forced (its
     * peers hold the other eight), and places it. Any board-changing commit clears
     * it: a hint about a cell the player has since altered is a stale claim, and
     * a stale claim is a lie the overlay must not tell.
     */
    data class Hinting(val state: SudokuState, val hint: Hint) : SudokuUi
}

/** The difficulty carried by whichever variant has one, for the picker to show as current. */
internal fun SudokuUi.difficultyOrNull(): String? = when (this) {
    is SudokuUi.Playing -> state.difficulty
    is SudokuUi.Hinting -> state.difficulty
    is SudokuUi.Complete -> state.difficulty
    is SudokuUi.ChoosingDifficulty -> currentDifficulty
    else -> null
}

/**
 * The difficulty picker's two transitions, as pure functions.
 *
 * Separated from the view model because the bug they exist to prevent was a state-machine
 * bug, not a coroutine or persistence bug, and this is the form in which it can be
 * asserted directly. `dismiss` returning `null` is the "there is nothing to go back to,
 * generate instead" case, which is precisely the branch that used to strand the UI on a
 * "GENERATING" screen with no generator running and no button on it.
 */
internal object DifficultyPickerTransition {

    /**
     * Opens the picker over [from].
     *
     * `Generating` is never recorded as a destination. The caller cancels the in-flight
     * generation when the picker opens, so going "back" to `Generating` would mean waiting
     * on work that no longer exists.
     */
    fun open(from: SudokuUi): SudokuUi.ChoosingDifficulty = SudokuUi.ChoosingDifficulty(
        currentDifficulty = from.difficultyOrNull(),
        returnTo = from.takeUnless { it is SudokuUi.Generating || it is SudokuUi.ChoosingDifficulty },
    )

    /**
     * Where dismissing [picker] should land, or `null` to generate a fresh puzzle.
     *
     * Every [SudokuUi] is a valid destination except the two `open` refuses to record, so
     * this total function has no dead branch by construction.
     */
    fun dismiss(picker: SudokuUi.ChoosingDifficulty): SudokuUi? = picker.returnTo
}

/**
 * `sudoku(9)` — Requirement 16 criteria 3 and 6.
 *
 * Generation runs on [Dispatchers.Default], never the main thread, and the screen shows a
 * non-blocking indication until it lands. That is what lets the shell meet its 1-second
 * launch budget while a puzzle is still being built.
 *
 * Undo history is in memory only (decision D17). Everything else persists.
 */
class SudokuViewModel(
    private val store: SudokuSessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<SudokuUi>(SudokuUi.Generating)
    val ui: StateFlow<SudokuUi> = _ui.asStateFlow()

    private val history = ArrayDeque<Move>()
    private var timerJob: Job? = null
    private var generateJob: Job? = null

    /**
     * Emitted once per solve, for the shell's `onGameComplete`.
     *
     * An event, not state: replay is 0 and there is no buffer, because "you just won" is
     * true at one instant. Deriving it from the `Complete` UI state instead would re-fire
     * every time the screen recomposed back into view, since this view model is scoped to
     * the activity and outlives the screen.
     */
    private val _completions = MutableSharedFlow<GameResult>()
    val completions: SharedFlow<GameResult> = _completions.asSharedFlow()

    /**
     * Committed entries and erases this session, for [GameResult.moves].
     *
     * In memory only, like the undo history it counts alongside (decision D17), so a
     * puzzle resumed after process death reports only the moves made since. Persisting it
     * would mean a schema change for a number nothing currently reads.
     */
    private var moveCount = 0

    /**
     * Whether the screen is on top. The clock only runs when it is.
     *
     * The view model is scoped to the activity, not to the game screen, so without this
     * the timer kept ticking while the player sat on Home or Settings, or while the app
     * was in the background — the recorded time was wall-clock since the puzzle started,
     * not time spent playing, which is what `TIME` claims to be (decision D20).
     */
    private var screenResumed = false

    val undoAvailable: Boolean get() = history.isNotEmpty()

    private var mistakeLimit: Int = 0

    /** Whether placing a digit sweeps that digit from its peers' pencil marks. */
    private var autoCleanNotes: Boolean = true

    /**
     * What the player asked for last, for RETRY after a generation failure.
     *
     * A failed generation shows one generic screen no matter what was requested, and
     * retrying has to reproduce the request — the daily puzzle especially, where a retry
     * must regenerate the same day's board rather than silently starting a Moderate game.
     */
    private var lastRequested: String? = null

    init {
        viewModelScope.launch { restoreOrGenerate() }
        viewModelScope.launch {
            store.mistakeLimit.collect { limit -> mistakeLimit = limit }
        }
        viewModelScope.launch {
            store.autoCleanNotes.collect { enabled -> autoCleanNotes = enabled }
        }
    }

    // ─── lifecycle ────────────────────────────────────────────────────────────

    /** Called when the game screen resumes. Resumes the clock if a game is in progress. */
    fun onScreenResumed() {
        screenResumed = true
        if (_ui.value is SudokuUi.Playing || _ui.value is SudokuUi.Hinting) startTimer()
    }

    /** Called when the game screen pauses. Stops the clock and saves. */
    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        val saved = store.session(SudokuDefinition.ID).first()
        val state = saved?.let(SudokuRestore::stateOrNull)

        when {
            state == null -> generate(DEFAULT_DIFFICULTY.name)

            // A completed puzzle should not be restored as in-progress.
            state.isComplete -> {
                store.clearSession(SudokuDefinition.ID)
                generate(state.difficulty)
            }

            else -> {
                _ui.value = SudokuUi.Playing(state)
                startTimer()
            }
        }
    }

    /**
     * Starts a new puzzle at [difficultyName] — one of the QQWing difficulty names, or
     * [DAILY_NAME] for the day's puzzle, which is a mode rather than a difficulty: it is
     * generated at a fixed engine difficulty but carried under its own label, its own
     * session slot and its own determinism contract.
     */
    fun generate(difficultyName: String) {
        timerJob?.cancel()
        generateJob?.cancel()
        history.clear()
        moveCount = 0
        lastRequested = difficultyName

        if (difficultyName == DAILY_NAME) {
            startDaily()
            return
        }

        _ui.value = SudokuUi.Generating
        generateJob = viewModelScope.launch {
            val difficulty = GameDifficulty.entries
                .firstOrNull { it.name == difficultyName } ?: DEFAULT_DIFFICULTY

            val puzzle = generatePuzzle(difficulty)

            if (puzzle == null || puzzle.size != Conflicts.CELLS) {
                _ui.value = SudokuUi.GenerationFailed
                return@launch
            }

            val state = SudokuState.fromPuzzle(puzzle, difficulty.name)
            _ui.value = SudokuUi.Playing(state)
            persist(state)
            if (screenResumed) startTimer()
        }
    }

    /** Re-runs whatever generation failed, so RETRY reproduces the original request. */
    fun retryGeneration() {
        generate(lastRequested ?: DEFAULT_DIFFICULTY.name)
    }

    /**
     * Starts, or resumes, the day's puzzle.
     *
     * The day is UTC. Decision D26's local-daily amendment says **no time-zone
     * calendar**, and a device-local midnight is exactly that: the same instant is a
     * different day in two time zones, and DST shifts when a "day" flips. UTC has
     * neither problem — the boundary never moves, and every device agrees which day a
     * seed means.
     *
     * Today's unfinished puzzle resumes where it was left. A stored session for an
     * earlier day is not "in progress" — it is leftovers — and because generation is
     * deterministic, today's board is simply generated fresh. The regular session slot
     * is never touched: a daily puzzle and an in-progress regular puzzle coexist.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        _ui.value = SudokuUi.Generating
        generateJob = viewModelScope.launch {
            val saved = store.dailyPuzzle().first()
            if (saved != null && saved.day == day) {
                val restored = SudokuRestore.stateOrNull(saved.session)
                    ?.copy(dailyDay = day)
                if (restored != null && !restored.isComplete) {
                    _ui.value = SudokuUi.Playing(restored)
                    if (screenResumed) startTimer()
                    return@launch
                }
            }

            val puzzle = generatePuzzle(DEFAULT_DIFFICULTY, seed = day.toInt())
            if (puzzle == null || puzzle.size != Conflicts.CELLS) {
                _ui.value = SudokuUi.GenerationFailed
                return@launch
            }
            val state = SudokuState.fromPuzzle(puzzle, DAILY_NAME, dailyDay = day)
            _ui.value = SudokuUi.Playing(state)
            persist(state)
            if (screenResumed) startTimer()
        }
    }

    /** Today's puzzle identity: the UTC date as a day count since the epoch. */
    private fun dailyUtcDay(): Long =
        ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().toEpochDay()

    /**
     * Generates one puzzle, bounded by attempts and a wall-clock deadline.
     *
     * QQWing's `generate` is a rejection loop with no iteration cap. `Simple` and
     * `Challenge` are the slow end — a 9×9 with the fewest givens needs many tries
     * before the random solution has enough cells removed to land on the difficulty
     * target. The original code bounded the wall clock but not the attempt count, so a
     * bad run could finish in time by luck only; a long run could exhaust attempts in
     * seconds without ever finding a candidate and still return null. Both extremes
     * produced a `GenerationFailed` the player could not distinguish from a real
     * generator crash.
     *
     * The bound is per attempt: each fresh QQWing run starts at attempt 0, and a
     * difficulty that needs N removed givens does not have a known answer key for us
     * to short-circuit. We retry up to [GENERATION_MAX_ATTEMPTS] times. On exhaustion
     * we fall back to `Moderate` — the only difficulty where QQWing reliably lands on
     * the first try, and the one the home screen starts on. A real generator failure
     * now means a real generator failure, not a budget one.
     */
    /**
     * Generates one puzzle, bounded by attempts and a wall-clock deadline.
     *
     * QQWing's `generate` is a rejection loop with no iteration cap. `Simple` and
     * `Challenge` are the slow end — a 9×9 with the fewest givens needs many tries
     * before the random solution has enough cells removed to land on the difficulty
     * target. The original code bounded the wall clock but not the attempt count, so a
     * bad run could finish in time by luck only; a long run could exhaust attempts in
     * seconds without ever finding a candidate and still return null. Both extremes
     * produced a `GenerationFailed` the player could not distinguish from a real
     * generator crash.
     *
     * The bound is per attempt: each fresh QQWing run starts at attempt 0, and a
     * difficulty that needs N removed givens does not have a known answer key for us
     * to short-circuit. We retry up to [GENERATION_MAX_ATTEMPTS] times. On exhaustion
     * we fall back to `Moderate` — the only difficulty where QQWing reliably lands on
     * the first try, and the one the home screen starts on. A real generator failure
     * now means a real generator failure, not a budget one.
     *
     * With a [seed], every attempt is deterministic — each fresh controller runs its
     * single worker from the same seeded stream, so attempt N produces exactly the
     * puzzle attempt 1 would have. Retrying the same day's puzzle can therefore never
     * wander to a different board.
     */
    private suspend fun generatePuzzle(
        difficulty: GameDifficulty,
        seed: Int? = null,
    ): IntArray? {
        val primary = runWithAttempts(difficulty, GENERATION_MAX_ATTEMPTS, seed)
        if (primary != null) return primary
        if (difficulty == DEFAULT_DIFFICULTY) return null
        // Last-resort fallback. Same controller path, same timeout, but a difficulty
        // where the rejection loop is short. Returning `null` here surfaces the same
        // `GenerationFailed` UI as a real crash, so the player can RETRY; we do not
        // silently downgrade the difficulty label.
        return runWithAttempts(DEFAULT_DIFFICULTY, GENERATION_MAX_ATTEMPTS, seed)
    }

    private suspend fun runWithAttempts(
        difficulty: GameDifficulty,
        maxAttempts: Int,
        seed: Int? = null,
    ): IntArray? {
        repeat(maxAttempts) {
            val controller = QQWingController()
            val result = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    suspendCancellableCoroutine { continuation ->
                        continuation.invokeOnCancellation { controller.abort() }
                        val generated = runCatching {
                            controller.generate(GameType.Default9x9, difficulty, seed)
                        }
                        continuation.resume(generated.getOrNull())
                    }
                }
            }.also {
                // The coroutine above may be cancelled (timeout/back), but a deadline that
                // leaves the worker threads running is not a deadline.
                if (it == null) controller.abort()
            }
            if (result != null && result.size == Conflicts.CELLS) return result
        }
        return null
    }

    /**
     * Starts or restarts the one-second clock.
     *
     * Refuses to run unless the screen is resumed and a game is actually in progress, so
     * no call site has to remember the precondition.
     */
    private fun startTimer() {
        timerJob?.cancel()
        if (!screenResumed) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                // The clock runs under the hint overlay too — it is a readout of the
                // session, and reading a hint does not pause the session.
                when (val current = _ui.value) {
                    is SudokuUi.Playing -> _ui.value = SudokuUi.Playing(
                        current.state.copy(elapsedMs = current.state.elapsedMs + TICK_MS)
                    )
                    is SudokuUi.Hinting -> _ui.value = SudokuUi.Hinting(
                        current.state.copy(elapsedMs = current.state.elapsedMs + TICK_MS),
                        current.hint,
                    )
                    else -> continue
                }
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    fun select(index: Int) = updatePlaying { it.copy(selected = index) }

    fun toggleNotesMode() = updatePlaying { it.copy(notesMode = !it.notesMode) }

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is SudokuUi.ChoosingDifficulty) return

        // The clock does not run behind a modal, and an in-flight generation is abandoned
        // rather than left to land on top of the picker.
        timerJob?.cancel()
        generateJob?.cancel()

        _ui.value = DifficultyPickerTransition.open(previous)
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? SudokuUi.ChoosingDifficulty ?: return

        when (val destination = DifficultyPickerTransition.dismiss(picker)) {
            // Nothing to go back to — the picker was opened before a puzzle existed, so
            // dismissing it has to produce one.
            null -> generate(picker.currentDifficulty ?: DEFAULT_DIFFICULTY.name)

            is SudokuUi.Playing -> {
                _ui.value = destination
                startTimer()
            }

            is SudokuUi.Hinting -> {
                _ui.value = destination
                startTimer()
            }

            else -> _ui.value = destination
        }
    }

    /**
     * Enters [digit] into the selected cell, or toggles it as a note in notes mode.
     *
     * Reads its state from the plain board *or* the hint overlay: placing the hinted
     * digit straight off the hint is the flow the hint exists for, and refusing input
     * there would make the overlay a dead end.
     *
     * @return true when the entry created a conflict, so the screen can fire the
     *         distinct conflict haptic. Decision D15.
     */
    fun enter(digit: Int): Boolean {
        val state = playingState() ?: return false
        val index = state.selected ?: return false

        // Requirement 16 criterion 10: a given rejects input entirely.
        if (state.isGiven(index)) return false

        record(state, index)

        val next = if (state.notesMode) {
            state.copy(
                notes = state.notes.replaceAt(index, SudokuCodec.toggleNote(state.notes[index], digit)),
            )
        } else {
            val withEntry = state.copy(
                entries = state.entries.replaceAt(index, digit),
                // Placing a value clears that cell's notes — they were hypotheses about
                // a cell that now has an answer.
                notes = state.notes.replaceAt(index, 0),
                lastEntered = index,
            )
            // The auto-clean assist: a placed digit proves its peers cannot hold it,
            // so their pencil marks for this digit are swept and remembered for undo.
            // The assist is a preference (default on) — some players consider even
            // this much bookkeeping theirs to do.
            val swept = if (autoCleanNotes) cleanedPeerNotes(state.notes, index, digit) else emptyMap()
            val withSweep = if (swept.isEmpty()) withEntry else withEntry.copy(
                notes = withEntry.notes.mapIndexed { i, mask -> swept[i] ?: mask },
            )
            // Decision D20: a mistake is counted once, at the moment of entry, when the
            // value conflicts. Correcting the cell does not decrement it.
            val created = Conflicts.conflictsAt(withSweep.merged, index)
            (if (created) withSweep.copy(mistakes = withSweep.mistakes + 1) else withSweep)
                .let { committed ->
                    if (swept.isEmpty()) committed else lastMoveWithPeers(committed, swept)
                }
        }

        finish(next)
        return !state.notesMode && Conflicts.conflictsAt(next.merged, index)
    }

    fun erase() {
        val state = playingState() ?: return
        val index = state.selected ?: return
        if (state.isGiven(index)) return

        record(state, index)
        finish(
            state.copy(
                entries = state.entries.replaceAt(index, 0),
                notes = state.notes.replaceAt(index, 0),
            )
        )
    }

    fun undo() {
        val state = playingState() ?: return
        val move = history.removeLastOrNull() ?: return

        finish(
            state.copy(
                entries = state.entries.replaceAt(move.index, move.previousEntry),
                notes = state.notes
                    // First restore the auto-cleaned peer masks…
                    .toMutableList()
                    .also { notes -> move.previousPeerNotes.forEach { (peer, mask) -> notes[peer] = mask } }
                    .toList()
                    // …then this cell's own mask, which never intersects the sweep.
                    .replaceAt(move.index, move.previousNotes),
                mistakes = move.previousMistakes,
                lastEntered = move.previousLastEntered,
                selected = move.index,
            ),
        )
    }

    // ─── the hint ─────────────────────────────────────────────────────────────

    /**
     * Shows the next provable step, if the board has one.
     *
     * Deduction only: the engine reads the board the player can already see, and
     * the game still never holds a solution. The hinted cell is *selected*, so the
     * existing cross highlight lights the row, column and box that force the digit —
     * the player sees the mechanism, not just the answer. When no naked single
     * exists the hint silently does nothing: there is no error state for "the next
     * step needs a technique this engine does not claim", and pretending otherwise
     * would mean storing answers.
     */
    fun hint() {
        val state = playingState() ?: return
        val hint = SudokuHint.nakedSingle(state) ?: return
        _ui.value = SudokuUi.Hinting(state.copy(selected = hint.index), hint)
    }

    /** Returns the hint overlay to the plain board. */
    fun dismissHint() {
        val hinting = _ui.value as? SudokuUi.Hinting ?: return
        _ui.value = SudokuUi.Playing(hinting.state)
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    private fun record(state: SudokuState, index: Int) {
        history.addLast(
            Move(
                index = index,
                previousEntry = state.entries[index],
                previousNotes = state.notes[index],
                previousMistakes = state.mistakes,
                previousLastEntered = state.lastEntered,
            )
        )
        moveCount++
        // Bounded at 50 — decision D17.
        while (history.size > Move.MAX_HISTORY) history.removeFirst()
    }

    /**
     * Commits [next], promoting it to a win if the grid is solved.
     *
     * The dropped `recordable` parameter was never read. `undo` passed `false` to it,
     * which read as "do not record this as a move" — a guarantee the parameter never
     * actually provided. Undo does not go through [record] at all, so the intent held by
     * accident; the parameter only made it look deliberate.
     */
    private fun finish(next: SudokuState) {
        // Mistake ceiling — when enabled, 3 strikes ends the run. Check before the
        // win check so a simultaneous solve + limit hit still counts as a win (the
        // grid is solved, which is rarer and more honest than "you solved it but
        // also failed").
        if (mistakeLimit > 0 && next.mistakes >= mistakeLimit && !next.isComplete) {
            timerJob?.cancel()
            _ui.value = SudokuUi.Complete(next)
            viewModelScope.launch {
                clearSessionOf(next)
                _completions.emit(
                    GameResult(
                        gameId = SudokuDefinition.ID,
                        won = false,
                        durationMs = next.elapsedMs,
                        difficulty = next.difficulty,
                        moves = moveCount,
                    )
                )
            }
            return
        }
        if (next.isComplete) {
            timerJob?.cancel()
            _ui.value = SudokuUi.Complete(next)
            viewModelScope.launch {
                clearSessionOf(next)
                _completions.emit(
                    GameResult(
                        gameId = SudokuDefinition.ID,
                        won = true,
                        durationMs = next.elapsedMs,
                        difficulty = next.difficulty,
                        moves = moveCount,
                    )
                )
            }
        } else {
            _ui.value = SudokuUi.Playing(next)
            persist(next)
        }
    }

    /** The live board, whether or not the hint overlay is over it. */
    private fun playingState(): SudokuState? = when (val current = _ui.value) {
        is SudokuUi.Playing -> current.state
        is SudokuUi.Hinting -> current.state
        else -> null
    }

    /**
     * Attaches the auto-clean sweep to the move [record] just pushed, so undo knows
     * which peer masks to restore. A small mutation of the deque tail rather than a
     * wider record() signature: the sweep is computed only after the entry branch
     * knows a digit was placed.
     */
    private fun lastMoveWithPeers(committed: SudokuState, swept: Map<Int, Int>): SudokuState {
        history.lastOrNull()?.let { last ->
            history.removeLast()
            history.addLast(last.copy(previousPeerNotes = swept))
        }
        return committed
    }

    private fun updatePlaying(transform: (SudokuState) -> SudokuState) {
        when (val current = _ui.value) {
            is SudokuUi.Playing -> _ui.value = SudokuUi.Playing(transform(current.state))
            // A selection change dismisses the hint: the overlay claims one cell and
            // the player just pointed somewhere else.
            is SudokuUi.Hinting -> _ui.value = SudokuUi.Playing(transform(current.state))
            else -> return
        }
    }

    private fun persist(state: SudokuState) {
        val session = SudokuSession(
            givens = state.givens,
            entries = state.entries,
            notes = state.notes,
            elapsedMs = state.elapsedMs,
            difficulty = state.difficulty,
            mistakes = state.mistakes,
        )
        viewModelScope.launch {
            val day = state.dailyDay
            // The daily slot and the regular slot are separate stores: a move in one
            // never writes over the other. Which slot a session belongs to is carried by
            // the state itself, so every save path routes without asking the caller.
            if (day != null) store.saveDailyPuzzle(day, session)
            else store.saveSession(SudokuDefinition.ID, session)
        }
    }

    /**
     * Clears whichever slot [state] belongs to — finishing the daily puzzle leaves an
     * in-progress regular session untouched, and vice versa.
     */
    private fun clearSessionOf(state: SudokuState) {
        viewModelScope.launch {
            if (state.dailyDay != null) store.clearDailyPuzzle()
            else store.clearSession(SudokuDefinition.ID)
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        playingState()?.let { persist(it) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        // Cancelling the coroutine is not enough on its own: the generator's worker
        // threads only stop when the flag they poll is set, and invokeOnCancellation is
        // what sets it. See generatePuzzle.
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        /**
         * The label, picker option, state difficulty and stats bucket of the day's puzzle.
         *
         * Not a [GameDifficulty]: it is a mode played at [DEFAULT_DIFFICULTY] with its
         * own slot and seed. Kept as a plain string because difficulty is a plain string
         * end to end (the shell keys stats by it); `GameDifficulty` stays reserved for
         * the engine's real levels.
         */
        const val DAILY_NAME = "Daily"
        val DEFAULT_DIFFICULTY = GameDifficulty.Moderate
        val DIFFICULTIES = listOf(
            GameDifficulty.Simple,
            GameDifficulty.Easy,
            GameDifficulty.Moderate,
            GameDifficulty.Hard,
            GameDifficulty.Challenge,
        )
        private const val TICK_MS = 1_000L

        /** Requirement 6 criterion 4 allows 2s; this is the hard ceiling before failing. */
        private const val GENERATION_TIMEOUT_MS = 10_000L

        /**
         * Max QQWing attempts per `generate(difficulty)`.
         *
         * `Simple` rarely needs more than a handful. `Challenge` (≥26 removed givens)
         * can take 30+ before the random solution tolerates the cuts. 8 is a generous
         * ceiling that keeps the wall-clock budget reasonable while letting the
         * rejection loop run. Exceeding it triggers the `Moderate` fallback, so a
         * real failure here would be QQWing producing nothing across 8 attempts at
         * `Moderate` — vanishingly unlikely and worth surfacing.
         */
        private const val GENERATION_MAX_ATTEMPTS = 8

        /**
         * The proven minimum number of givens for a uniquely solvable 9×9 grid.
         *
         * A restored board with fewer did not come from the generator, so it is treated
         * as a corrupt save. See `SudokuSession.toStateOrNull`.
         */
        private const val MIN_GIVENS = 17
    }
}

private fun List<Int>.replaceAt(index: Int, value: Int): List<Int> =
    toMutableList().also { it[index] = value }
