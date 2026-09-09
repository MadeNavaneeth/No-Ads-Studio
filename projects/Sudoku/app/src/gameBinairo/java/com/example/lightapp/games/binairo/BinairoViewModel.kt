package com.example.lightapp.games.binairo

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
sealed interface BinairoUi {
    data object Generating : BinairoUi
    data object GenerationFailed : BinairoUi
    data class Playing(val state: BinairoState) : BinairoUi

    /**
     * The difficulty picker, and what to go back to if it is dismissed.
     *
     * [returnTo] is never `Generating`: opening the picker cancels the in-flight
     * generation, so going "back" to it would mean waiting on work that no longer
     * exists — the two-tap dead end this shape exists to prevent.
     */
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: BinairoUi?,
    ) : BinairoUi

    data class Complete(val state: BinairoState) : BinairoUi
}

/** The difficulty carried by whichever variant has one, for the picker to show as current. */
internal fun BinairoUi.difficultyOrNull(): String? = when (this) {
    is BinairoUi.Playing -> state.difficulty
    is BinairoUi.Complete -> state.difficulty
    is BinairoUi.ChoosingDifficulty -> currentDifficulty
    else -> null
}

/** The picker's two transitions, as pure functions — asserted directly, see the tests. */
internal object BinairoDifficultyTransition {

    fun open(from: BinairoUi): BinairoUi.ChoosingDifficulty = BinairoUi.ChoosingDifficulty(
        currentDifficulty = from.difficultyOrNull(),
        returnTo = from.takeUnless { it is BinairoUi.Generating || it is BinairoUi.ChoosingDifficulty },
    )

    /** Where dismissing [picker] should land, or `null` to generate a fresh puzzle. */
    fun dismiss(picker: BinairoUi.ChoosingDifficulty): BinairoUi? = picker.returnTo
}

/**
 * `binairo(10)`.
 *
 * Generation runs on [Dispatchers.Default] — the deduction proof is the expensive
 * part, and the screen shows a non-blocking pulse until it lands. Undo history is
 * in memory only (decision D17); everything else persists.
 */
class BinairoViewModel(
    private val store: SessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<BinairoUi>(BinairoUi.Generating)
    val ui: StateFlow<BinairoUi> = _ui.asStateFlow()

    /**
     * Emitted once per finished run, for the shell's `onGameComplete`.
     *
     * An event, not state: replay is 0 and there is no buffer, because a finished run
     * is true at one instant. This view model is scoped to the activity and outlives
     * the screen, so deriving it from the `Complete` state would re-fire on every return.
     */
    private val _completions = MutableSharedFlow<GameResult>()
    val completions: SharedFlow<GameResult> = _completions.asSharedFlow()

    /** Committed board changes this session, for [GameResult.moves]. In memory only. */
    private var moveCount = 0

    /**
     * Whether the screen is on top. The clock only runs when it is: without this the
     * timer would count Home, Settings, and background time as play time, which is not
     * what `TIME` claims to be.
     */
    private var screenResumed = false

    val undoAvailable: Boolean get() = history.canUndo

    private val history = BinairoHistory()
    private var timerJob: Job? = null
    private var generateJob: Job? = null

    /** What the player asked for last, for RETRY after a generation failure. */
    private var lastRequested: String? = null

    init {
        viewModelScope.launch { restoreOrGenerate() }
    }

    // ─── lifecycle ────────────────────────────────────────────────────────────

    /** Called when the game screen resumes. Resumes the clock if a game is in progress. */
    fun onScreenResumed() {
        screenResumed = true
        if (_ui.value is BinairoUi.Playing) startTimer()
    }

    /** Called when the game screen pauses. Stops the clock and saves. */
    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        val saved = store.binairoSession().first()
        val state = saved?.let(BinairoRestore::stateOrNull)

        when {
            state == null -> generate(DEFAULT_DIFFICULTY)

            // A completed board should not be restored as in-progress.
            state.isComplete -> {
                viewModelScope.launch { store.clearBinairoSession() }
                generate(state.difficulty)
            }

            else -> {
                _ui.value = BinairoUi.Playing(state)
                startTimer()
            }
        }
    }

    /** Starts a new puzzle at [difficultyName] — one of [DIFFICULTIES], or [DAILY_NAME]
     * for the day's puzzle (decision D32). Anything else falls back to the default. */
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

        _ui.value = BinairoUi.Generating
        generateJob = viewModelScope.launch {
            val difficulty = DIFFICULTIES.firstOrNull { it == difficultyName } ?: DEFAULT_DIFFICULTY

            // The generation loop is milliseconds per attempt with its own budget;
            // this timeout is a backstop so a pathological seed can only ever cost
            // the player a retry, not a frozen screen.
            val puzzle = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    BinairoGenerator.generate(difficulty)
                }
            }

            if (puzzle == null || puzzle.size != BinairoRules.CELLS) {
                _ui.value = BinairoUi.GenerationFailed
                return@launch
            }

            val state = BinairoState.start(givens = puzzle, difficulty = difficulty)
            _ui.value = BinairoUi.Playing(state)
            persist(state)
            if (screenResumed) startTimer()
        }
    }

    /** Re-runs whatever generation failed, so RETRY reproduces the original request. */
    fun retryGeneration() {
        generate(lastRequested ?: DEFAULT_DIFFICULTY)
    }

    /**
     * Starts, or resumes, the day's puzzle — decision D32, the studio-wide daily
     * contract. The day is UTC: the boundary never moves and every device agrees
     * which day a seed means. The board is the generator seeded with the day
     * number, so every device generates the same puzzle until midnight UTC.
     * Today's unfinished daily resumes; an earlier day's leftovers are rejected by
     * the day check in the store's daily reader and simply regenerated. The regular
     * session slot is never touched.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        _ui.value = BinairoUi.Generating
        generateJob = viewModelScope.launch {
            val saved = store.dailyBinairo(day).first()
            if (saved != null) {
                val restored = BinairoRestore.stateOrNull(saved)?.copy(dailyDay = day)
                if (restored != null && !restored.isComplete) {
                    _ui.value = BinairoUi.Playing(restored)
                    if (screenResumed) startTimer()
                    return@launch
                }
            }

            val puzzle = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    BinairoGenerator.generate(DAILY_DIFFICULTY, seed = day)
                }
            }

            if (puzzle == null || puzzle.size != BinairoRules.CELLS) {
                _ui.value = BinairoUi.GenerationFailed
                return@launch
            }

            val state = BinairoState.start(
                givens = puzzle,
                difficulty = DAILY_NAME,
                dailyDay = day,
            )
            _ui.value = BinairoUi.Playing(state)
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
                val playing = _ui.value as? BinairoUi.Playing ?: continue
                _ui.value = BinairoUi.Playing(
                    playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS)
                )
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    /**
     * Tap: undecided gains a ONE, a ONE flips to a ZERO, a ZERO returns to undecided.
     * A given refuses outright — rule M8; there is no code path by which a player
     * value overwrites one. A tap that completes the board ends the run in the same
     * breath — the win is the board's property, not a screen event.
     */
    fun tap(index: Int) {
        val playing = _ui.value as? BinairoUi.Playing ?: return
        val state = playing.state

        // Rule M8. A given is not in the cycle — the refusal is structural.
        if (state.isGiven(index)) return

        val nextValue = BinairoRules.cycle(state.entries[index])

        history.record(BinairoHistory.Move(index = index, previousValue = state.entries[index]))
        moveCount++

        val next = state.copy(
            entries = state.entries.toMutableList().also { it[index] = nextValue },
        )
        finish(next)
    }

    fun undo() {
        val playing = _ui.value as? BinairoUi.Playing ?: return
        val move = history.revert() ?: return
        val state = playing.state
        finish(
            state.copy(
                entries = state.entries.toMutableList().also { it[move.index] = move.previousValue },
            ),
        )
    }

    // ─── the picker ───────────────────────────────────────────────────────────

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is BinairoUi.ChoosingDifficulty) return

        // The clock does not run behind a modal, and an in-flight generation is
        // abandoned rather than left to land on top of the picker.
        timerJob?.cancel()
        generateJob?.cancel()

        _ui.value = BinairoDifficultyTransition.open(previous)
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? BinairoUi.ChoosingDifficulty ?: return

        when (val destination = BinairoDifficultyTransition.dismiss(picker)) {
            // Nothing to go back to — the picker was opened before a puzzle existed,
            // so dismissing it has to produce one.
            null -> generate(picker.currentDifficulty ?: DEFAULT_DIFFICULTY)

            is BinairoUi.Playing -> {
                _ui.value = destination
                startTimer()
            }

            else -> _ui.value = destination
        }
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    /**
     * Commits [next], promoting it to a finished run when the board is solved.
     *
     * There is no loss condition — every tap is reversible — so the only completion
     * is the win, and it is the only way this run ends.
     */
    private fun finish(next: BinairoState) {
        if (next.isComplete) {
            timerJob?.cancel()
            _ui.value = BinairoUi.Complete(next)
            viewModelScope.launch {
                clearSlot(next)
                _completions.emit(
                    GameResult(
                        gameId = BinairoDefinition.ID,
                        won = true,
                        durationMs = next.elapsedMs,
                        difficulty = next.difficulty,
                        moves = moveCount,
                    )
                )
            }
        } else {
            _ui.value = BinairoUi.Playing(next)
            persist(next)
        }
    }

    /**
     * Clears whichever slot [state] belongs to — finishing the daily puzzle leaves an
     * in-progress regular puzzle exactly where it was, and vice versa (D31's rule).
     */
    private suspend fun clearSlot(state: BinairoState) {
        if (state.dailyDay != null) store.clearDailyBinairo() else store.clearBinairoSession()
    }

    /**
     * Routes [state] to its slot. A daily run (decision D32) never touches the
     * regular session: it saves into `daily.<gameId>.*` under its own day, so the
     * home resume cards — which scan the `session.` prefix — never offer it.
     */
    private fun persist(state: BinairoState) {
        viewModelScope.launch {
            val day = state.dailyDay
            if (day != null) store.saveDailyBinairo(day, state.toSession())
            else store.saveBinairoSession(state.toSession())
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        (_ui.value as? BinairoUi.Playing)?.let { persist(it.state) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        val DEFAULT_DIFFICULTY = BinairoGenerator.DEFAULT_DIFFICULTY
        val DIFFICULTIES = BinairoGenerator.DIFFICULTIES

        /** The day's puzzle (decision D32) — a mode, listed first in the picker. */
        const val DAILY_NAME = "Daily"

        /** The difficulty the daily is generated at: a fixed, honest engine setting. */
        val DAILY_DIFFICULTY = BinairoGenerator.DEFAULT_DIFFICULTY

        private const val TICK_MS = 1_000L

        /** The generator loop is milliseconds with its own budget; this is a backstop. */
        private const val GENERATION_TIMEOUT_MS = 10_000L
    }
}