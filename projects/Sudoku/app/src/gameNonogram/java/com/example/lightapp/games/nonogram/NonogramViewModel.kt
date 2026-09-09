package com.example.lightapp.games.nonogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightapp.core.GameResult
import com.example.lightapp.studio.persistence.SessionStore
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What the screen is showing. A sealed hierarchy rather than a bag of booleans, so an
 * impossible combination — generating *and* complete — cannot be represented.
 */
sealed interface NonogramUi {
    data object Generating : NonogramUi
    data object GenerationFailed : NonogramUi
    data class Playing(val state: NonogramState) : NonogramUi

    /**
     * The difficulty picker, and what to go back to if it is dismissed.
     *
     * [returnTo] is never `Generating`: opening the picker cancels the in-flight
     * generation, so going "back" to it would mean waiting on work that no longer
     * exists — the two-tap dead end this shape exists to prevent.
     */
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: NonogramUi?,
    ) : NonogramUi

    data class Complete(val state: NonogramState) : NonogramUi
}

/** The difficulty carried by whichever variant has one, for the picker to show as current. */
internal fun NonogramUi.difficultyOrNull(): String? = when (this) {
    is NonogramUi.Playing -> state.difficulty
    is NonogramUi.Complete -> state.difficulty
    is NonogramUi.ChoosingDifficulty -> currentDifficulty
    else -> null
}

/** The picker's two transitions, as pure functions — asserted directly, see the tests. */
internal object NonogramDifficultyTransition {

    fun open(from: NonogramUi): NonogramUi.ChoosingDifficulty = NonogramUi.ChoosingDifficulty(
        currentDifficulty = from.difficultyOrNull(),
        returnTo = from.takeUnless { it is NonogramUi.Generating || it is NonogramUi.ChoosingDifficulty },
    )

    /** Where dismissing [picker] should land, or `null` to generate a fresh puzzle. */
    fun dismiss(picker: NonogramUi.ChoosingDifficulty): NonogramUi? = picker.returnTo
}

/**
 * `nonogram(10)`.
 *
 * Generation runs on [Dispatchers.Default], never the main thread, and the screen shows a
 * non-blocking indication until it lands. Undo history is in memory only (decision D17).
 * Everything else persists.
 */
class NonogramViewModel(
    private val store: SessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<NonogramUi>(NonogramUi.Generating)
    val ui: StateFlow<NonogramUi> = _ui.asStateFlow()

    /**
     * Emitted once per finished run, for the shell's `onGameComplete`.
     *
     * An event, not state: replay is 0 and there is no buffer, because a finished run
     * is true at one instant. This view model is scoped to the activity and outlives
     * the screen, so deriving it from the `Complete` state would re-fire on every return.
     */
    private val _completions = MutableSharedFlow<GameResult>()
    val completions: SharedFlow<GameResult> = _completions.asSharedFlow()

    /**
     * Committed cell changes this session, for [GameResult.moves]. In memory only,
     * like the undo history it counts alongside (decision D17).
     */
    private var moveCount = 0

    /**
     * Whether the screen is on top. The clock only runs when it is: without this the
     * timer would count Home, Settings, and background time as play time, which is not
     * what `TIME` claims to be.
     */
    private var screenResumed = false

    val undoAvailable: Boolean get() = history.isNotEmpty()

    private val history = ArrayDeque<NonoMove>()
    private var timerJob: Job? = null
    private var generateJob: Job? = null
    private var mistakeLimit: Int = 0

    /** What the player asked for last, for RETRY after a generation failure. */
    private var lastRequested: String? = null

    init {
        viewModelScope.launch { restoreOrGenerate() }
        viewModelScope.launch {
            store.mistakeLimit.collect { limit -> mistakeLimit = limit }
        }
    }

    // ─── lifecycle ────────────────────────────────────────────────────────────

    /** Called when the game screen resumes. Resumes the clock if a game is in progress. */
    fun onScreenResumed() {
        screenResumed = true
        if (_ui.value is NonogramUi.Playing) startTimer()
    }

    /** Called when the game screen pauses. Stops the clock and saves. */
    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        val saved = store.nonogramSession().first()
        val state = saved?.let(NonogramRestore::stateOrNull)

        when {
            state == null -> generate(DEFAULT_DIFFICULTY)

            // A completed puzzle should not be restored as in-progress.
            state.isComplete -> {
                viewModelScope.launch { store.clearNonogramSession() }
                generate(state.difficulty)
            }

            else -> {
                _ui.value = NonogramUi.Playing(state)
                startTimer()
            }
        }
    }

    /**
     * Starts a new puzzle at [difficultyName] — one of [DIFFICULTIES], or [DAILY_NAME]
     * for the day's puzzle (decision D32). Anything else falls back to the default
     * rather than failing.
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

        _ui.value = NonogramUi.Generating
        generateJob = viewModelScope.launch {
            val difficulty = DIFFICULTIES.firstOrNull { it == difficultyName } ?: DEFAULT_DIFFICULTY

            // Bounded attempts and a wall-clock deadline. The generator loop is fast
            // and finite, so no abort flag is needed — the timeout is real here.
            val puzzle = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    NonogramGenerator.generate(difficulty)
                }
            }

            if (puzzle == null || puzzle.size != NonogramRules.CELLS) {
                _ui.value = NonogramUi.GenerationFailed
                return@launch
            }

            val state = NonogramState.fresh(puzzle, difficulty)
            _ui.value = NonogramUi.Playing(state)
            persist(state)
            if (screenResumed) startTimer()
        }
    }

    /** Re-runs whatever generation failed, so RETRY reproduces the original request. */
    fun retryGeneration() {
        generate(lastRequested ?: DEFAULT_DIFFICULTY)
    }

    /**
     * Starts, or resumes, the day's puzzle — sudoku's D31 daily, generalised by
     * decision D32 to every game in the studio.
     *
     * The day is UTC: the boundary never moves, every device agrees which day a seed
     * means, and no time-zone calendar exists anywhere (D26's amendment). The board is
     * the generator seeded with the day number, so the same puzzle is generated on
     * every device until midnight UTC.
     *
     * Today's unfinished daily resumes where it was left. A stored session for an
     * earlier day is leftovers, not progress — the day check in [SessionStore]'s daily
     * reader already rejects it, so generation simply runs. The regular session slot
     * is never touched: a daily and an in-progress regular puzzle coexist.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        _ui.value = NonogramUi.Generating
        generateJob = viewModelScope.launch {
            val saved = store.dailyNonogram(day).first()
            if (saved != null) {
                val restored = NonogramRestore.stateOrNull(saved)?.copy(dailyDay = day)
                if (restored != null && !restored.isComplete) {
                    _ui.value = NonogramUi.Playing(restored)
                    if (screenResumed) startTimer()
                    return@launch
                }
            }

            val puzzle = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    NonogramGenerator.generate(DAILY_DIFFICULTY, seed = day)
                }
            }

            if (puzzle == null || puzzle.size != NonogramRules.CELLS) {
                _ui.value = NonogramUi.GenerationFailed
                return@launch
            }

            val state = NonogramState.fresh(puzzle, DAILY_NAME, dailyDay = day)
            _ui.value = NonogramUi.Playing(state)
            persist(state)
            if (screenResumed) startTimer()
        }
    }

    /** Today's puzzle identity: the UTC date as a day count since the epoch. */
    private fun dailyUtcDay(): Long =
        java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()

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
                val playing = _ui.value as? NonogramUi.Playing ?: continue
                _ui.value = NonogramUi.Playing(
                    playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS)
                )
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    fun select(index: Int) {
        val playing = _ui.value as? NonogramUi.Playing ?: return
        _ui.value = NonogramUi.Playing(playing.state.copy(selected = index))
    }

    /**
     * Tap: select and fill in one press (parity N2). A filled cell clears back to
     * unknown; a marked cell clears its mark first — decided cells need an explicit
     * tap, so nothing is ever destroyed as a side effect.
     *
     * @return true when the entry was a mistake, so the screen can fire the
     *         distinct conflict haptic. The board itself never shows it (method §6).
     */
    fun press(index: Int): Boolean {
        val playing = _ui.value as? NonogramUi.Playing ?: return false
        val state = playing.state.copy(selected = index)
        record(state, index)
        val next = state.copy(cells = NonogramRules.tap(state.cells, index))
        // A mistake is counted once, at the moment of entry, when a fill lands on
        // an empty solution cell. Correcting it does not decrement it.
        val mistake = next.cells[index] == NonogramRules.FILLED && next.solution[index] == 0
        finish(if (mistake) next.copy(mistakes = next.mistakes + 1) else next)
        return mistake
    }

    /** Long-press: select and toggle the empty-mark (parity N2). Silent on filled cells. */
    fun mark(index: Int) {
        val playing = _ui.value as? NonogramUi.Playing ?: return
        val state = playing.state.copy(selected = index)
        val marked = NonogramRules.toggleMark(state.cells, index) ?: return
        record(state, index)
        moveCount++
        finish(state.copy(cells = marked))
    }

    /**
     * One step of a drag-to-paint run (parity N2). [value] is locked by the run's
     * origin cell — a fill run never marks, a mark run never fills — and decided
     * cells are skipped, so a fast drag cannot destroy reasoning. Returns whether
     * the board changed, so the run records history only for real steps.
     */
    fun paint(index: Int, value: Int): Boolean {
        val playing = _ui.value as? NonogramUi.Playing ?: return false
        val state = playing.state
        val painted = NonogramRules.paintCell(state.cells, index, value) ?: return false
        record(state, index)
        moveCount++
        val next = state.copy(cells = painted)
        val mistake = value == NonogramRules.FILLED && state.solution[index] == 0
        finish(if (mistake) next.copy(mistakes = next.mistakes + 1) else next)
        return true
    }

    fun undo() {
        val playing = _ui.value as? NonogramUi.Playing ?: return
        val move = history.removeLastOrNull() ?: return
        val state = playing.state
        finish(
            state.copy(
                cells = state.cells.toMutableList().also { it[move.index] = move.previousCell },
                mistakes = move.previousMistakes,
                selected = move.previousSelected,
            ),
        )
    }

    // ─── the picker ───────────────────────────────────────────────────────────

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is NonogramUi.ChoosingDifficulty) return

        // The clock does not run behind a modal, and an in-flight generation is abandoned
        // rather than left to land on top of the picker.
        timerJob?.cancel()
        generateJob?.cancel()

        _ui.value = NonogramDifficultyTransition.open(previous)
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? NonogramUi.ChoosingDifficulty ?: return

        when (val destination = NonogramDifficultyTransition.dismiss(picker)) {
            // Nothing to go back to — the picker was opened before a puzzle existed, so
            // dismissing it has to produce one.
            null -> generate(picker.currentDifficulty ?: DEFAULT_DIFFICULTY)

            is NonogramUi.Playing -> {
                _ui.value = destination
                startTimer()
            }

            else -> _ui.value = destination
        }
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    private fun record(state: NonogramState, index: Int) {
        history.addLast(
            NonoMove(
                index = index,
                previousCell = state.cells[index],
                previousMistakes = state.mistakes,
                previousSelected = state.selected,
            )
        )
        moveCount++
        // Bounded at 50 — decision D17.
        while (history.size > NonoMove.MAX_HISTORY) history.removeFirst()
    }

    /**
     * Commits [next], promoting it to a finished run on a win or on the lives limit.
     *
     * The limit is checked before the win so a simultaneous solve still counts as a
     * win — the grid is solved, which is rarer and more honest. A lost run emits its
     * result too, so the stats page counts it played but not won.
     */
    private fun finish(next: NonogramState) {
        if (mistakeLimit > 0 && next.mistakes >= mistakeLimit && !next.isComplete) {
            timerJob?.cancel()
            _ui.value = NonogramUi.Complete(next)
            viewModelScope.launch {
                clearSlot(next)
                _completions.emit(
                    GameResult(
                        gameId = NonogramDefinition.ID,
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
            _ui.value = NonogramUi.Complete(next)
            viewModelScope.launch {
                clearSlot(next)
                _completions.emit(
                    GameResult(
                        gameId = NonogramDefinition.ID,
                        won = true,
                        durationMs = next.elapsedMs,
                        difficulty = next.difficulty,
                        moves = moveCount,
                    )
                )
            }
        } else {
            _ui.value = NonogramUi.Playing(next)
            persist(next)
        }
    }

    /**
     * Clears whichever slot [state] belongs to — finishing the daily puzzle leaves an
     * in-progress regular puzzle exactly where it was, and vice versa (D31's rule).
     */
    private suspend fun clearSlot(state: NonogramState) {
        if (state.dailyDay != null) store.clearDailyNonogram() else store.clearNonogramSession()
    }

    /**
     * Routes [state] to its slot. A daily run (decision D32) never touches the
     * regular session: it saves into `daily.<gameId>.*` under its own day, so the
     * home resume cards — which scan the `session.` prefix — never offer it.
     */
    private fun persist(state: NonogramState) {
        viewModelScope.launch {
            val day = state.dailyDay
            if (day != null) store.saveDailyNonogram(day, state.toSession())
            else store.saveNonogramSession(state.toSession())
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        (_ui.value as? NonogramUi.Playing)?.let { persist(it.state) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        val DEFAULT_DIFFICULTY = NonogramGenerator.DEFAULT_DIFFICULTY
        val DIFFICULTIES = NonogramGenerator.DIFFICULTIES

        /** The day's puzzle (decision D32) — a mode, listed first in the picker. */
        const val DAILY_NAME = "Daily"

        /** The difficulty the daily is generated at: a fixed, honest engine setting. */
        val DAILY_DIFFICULTY = NonogramGenerator.DEFAULT_DIFFICULTY

        private const val TICK_MS = 1_000L

        /** The generator loop is millisecond-scale; this is a backstop, not a budget. */
        private const val GENERATION_TIMEOUT_MS = 10_000L
    }
}
