package com.example.lightapp.games.wordsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightapp.core.GameResult
import com.example.lightapp.studio.persistence.WordsearchSessionStore
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
sealed interface WordsearchUi {
    data object Generating : WordsearchUi
    data object GenerationFailed : WordsearchUi
    data class Playing(val state: WordsearchState) : WordsearchUi

    /**
     * The difficulty picker, and what to go back to if it is dismissed.
     *
     * [returnTo] is never `Generating`: opening the picker cancels the in-flight
     * generation, so going "back" to it would mean waiting on work that no longer
     * exists — the two-tap dead end this shape exists to prevent.
     */
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: WordsearchUi?,
    ) : WordsearchUi

    data class Complete(val state: WordsearchState) : WordsearchUi
}

/** The difficulty carried by whichever variant has one, for the picker to show as current. */
internal fun WordsearchUi.difficultyOrNull(): String? = when (this) {
    is WordsearchUi.Playing -> state.difficulty
    is WordsearchUi.Complete -> state.difficulty
    is WordsearchUi.ChoosingDifficulty -> currentDifficulty
    else -> null
}

/** The picker's two transitions, as pure functions — asserted directly, see the tests. */
internal object WordsearchDifficultyTransition {

    fun open(from: WordsearchUi): WordsearchUi.ChoosingDifficulty = WordsearchUi.ChoosingDifficulty(
        currentDifficulty = from.difficultyOrNull(),
        returnTo = from.takeUnless { it is WordsearchUi.Generating || it is WordsearchUi.ChoosingDifficulty },
    )

    /** Where dismissing [picker] should land, or `null` to generate a fresh puzzle. */
    fun dismiss(picker: WordsearchUi.ChoosingDifficulty): WordsearchUi? = picker.returnTo
}

/**
 * `wordsearch(12)`.
 *
 * The selection is a gesture, not a decision: it lives here as [selection], never
 * reaches the state, and the board only changes when the drag releases and
 * [WordsearchRules.evaluate] rules the line a word. Undo history is in memory
 * only (decision D17). Everything else persists.
 */
class WordsearchViewModel(
    private val store: WordsearchSessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<WordsearchUi>(WordsearchUi.Generating)
    val ui: StateFlow<WordsearchUi> = _ui.asStateFlow()

    /**
     * Emitted once per finished run, for the shell's `onGameComplete`.
     *
     * An event, not state: replay is 0 and there is no buffer, because a finished run
     * is true at one instant. This view model is scoped to the activity and outlives
     * the screen, so deriving it from the `Complete` state would re-fire on every return.
     */
    private val _completions = MutableSharedFlow<GameResult>()
    val completions: SharedFlow<GameResult> = _completions.asSharedFlow()

    /** Committed finds this session, for [GameResult.moves]. In memory only. */
    private var moveCount = 0

    /**
     * Whether the screen is on top. The clock only runs when it is: without this the
     * timer would count Home, Settings, and background time as play time, which is not
     * what `TIME` claims to be.
     */
    private var screenResumed = false

    /** The live drag: first cell to last cell touched, or null when no drag is under way. */
    private val _selection = MutableStateFlow<List<Int>?>(null)
    val selection: StateFlow<List<Int>?> = _selection.asStateFlow()

    val undoAvailable: Boolean get() = history.isNotEmpty()

    private val history = ArrayDeque<WordsearchMove>()
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
        if (_ui.value is WordsearchUi.Playing) startTimer()
    }

    /** Called when the game screen pauses. Stops the clock, drops the drag, and saves. */
    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        _selection.value = null
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        val saved = store.session().first()
        val state = saved?.let(WordsearchRestore::stateOrNull)

        when {
            state == null -> generate(DEFAULT_DIFFICULTY)

            // A completed grid should not be restored as in-progress.
            state.isComplete -> {
                viewModelScope.launch { store.clearSession() }
                generate(state.difficulty)
            }

            else -> {
                _ui.value = WordsearchUi.Playing(state)
                startTimer()
            }
        }
    }

    /**
     * Starts a new grid at [difficultyName] — one of [DIFFICULTIES], or [DAILY_NAME]
     * for the day's puzzle (decision D32). Anything unknown falls back to default.
     */
    fun generate(difficultyName: String) {
        timerJob?.cancel()
        generateJob?.cancel()
        history.clear()
        moveCount = 0
        _selection.value = null
        lastRequested = difficultyName

        if (difficultyName == DAILY_NAME) {
            startDaily()
            return
        }

        _ui.value = WordsearchUi.Generating
        generateJob = viewModelScope.launch {
            val difficulty = DIFFICULTIES.firstOrNull { it == difficultyName } ?: DEFAULT_DIFFICULTY

            // Placement is millisecond-scale; the timeout is a backstop, and the
            // work itself runs off the main thread like every generator in the studio.
            val board = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    WordsearchGenerator.generate(difficulty)
                }
            }

            if (board == null) {
                _ui.value = WordsearchUi.GenerationFailed
                return@launch
            }

            val state = WordsearchState.fresh(board, difficulty)
            _ui.value = WordsearchUi.Playing(state)
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
     * The day is UTC: the boundary never moves and every device agrees which day a
     * seed means (D26's no-time-zone-calendar rule). The grid is the generator seeded
     * with the day number, so every device generates the same puzzle until midnight
     * UTC. Today's unfinished daily resumes; an earlier day's leftovers are rejected
     * by the day check in the store's daily reader and simply regenerated. The regular
     * session slot is never touched.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        _ui.value = WordsearchUi.Generating
        generateJob = viewModelScope.launch {
            val saved = store.daily(day).first()
            if (saved != null) {
                val restored = WordsearchRestore.stateOrNull(saved)?.copy(dailyDay = day)
                if (restored != null && !restored.isComplete) {
                    _ui.value = WordsearchUi.Playing(restored)
                    if (screenResumed) startTimer()
                    return@launch
                }
            }

            val board = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    WordsearchGenerator.generate(DAILY_DIFFICULTY, seed = day)
                }
            }

            if (board == null) {
                _ui.value = WordsearchUi.GenerationFailed
                return@launch
            }

            val state = WordsearchState.fresh(board, DAILY_NAME, dailyDay = day)
            _ui.value = WordsearchUi.Playing(state)
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
                val playing = _ui.value as? WordsearchUi.Playing ?: continue
                _ui.value = WordsearchUi.Playing(
                    playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS)
                )
            }
        }
    }

    // ─── the selection ────────────────────────────────────────────────────────

    /**
     * Begins the live drag at [index]. The screen collects [selection] for the
     * highlight, which is why this is a flow of its own — the board state must not
     * churn while a finger wanders.
     */
    fun dragStart(index: Int) {
        _selection.value = listOf(index)
    }

    /**
     * Extends the live drag toward [index]. The selection is the straight line from
     * the origin to that cell when one exists, otherwise it snaps to just the origin
     * — a wiggling finger shows the best line it is currently tracing, not a flicker.
     */
    fun dragTo(index: Int) {
        val current = _selection.value ?: return
        val origin = current.first()
        val dr = WordsearchRules.rowOf(index) - WordsearchRules.rowOf(origin)
        val dc = WordsearchRules.colOf(index) - WordsearchRules.colOf(origin)
        val span = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
        _selection.value = WordsearchRules.line(origin, dr.signOf(), dc.signOf(), span + 1)
            ?: listOf(origin)
    }

    /**
     * Ends the drag and commits whatever it spelled. Returns the words found — the
     * screen fires the found haptic when it is non-empty — because only the rules
     * know, and only the caller should feel it.
     */
    fun dragEnd(): List<String> {
        val playing = _ui.value as? WordsearchUi.Playing ?: return emptyList()
        val walked = _selection.value
        _selection.value = null
        if (walked == null || walked.size < 2) return emptyList()

        val matched = WordsearchRules.evaluate(playing.state.placements, walked.first(), walked.last())
        if (matched.isEmpty()) return emptyList()

        // Only words not already found advance the board.
        val alreadyFound = playing.state.foundWords
        val fresh = matched.filter { it.word !in alreadyFound }
        if (fresh.isEmpty()) return emptyList()

        val nextCells = WordsearchRules.applyFound(
            playing.state.foundCells,
            fresh.map { it.cells },
        )
        history.addLast(WordsearchMove(previousCells = playing.state.foundCells))
        while (history.size > WordsearchMove.MAX_HISTORY) history.removeFirst()
        moveCount++

        val next = playing.state.copy(foundCells = nextCells)
        if (next.isComplete) {
            finish(next)
        } else {
            _ui.value = WordsearchUi.Playing(next)
            persist(next)
        }
        return fresh.map { it.word }
    }

    fun undo() {
        val playing = _ui.value as? WordsearchUi.Playing ?: return
        val move = history.removeLastOrNull() ?: return
        // Undo only darkens cells, so it can never complete a board.
        val next = playing.state.copy(foundCells = move.previousCells)
        _ui.value = WordsearchUi.Playing(next)
        persist(next)
    }

    // ─── the picker ───────────────────────────────────────────────────────────

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is WordsearchUi.ChoosingDifficulty) return

        // The clock does not run behind a modal, and an in-flight generation is
        // abandoned rather than left to land on top of the picker.
        timerJob?.cancel()
        generateJob?.cancel()

        _ui.value = WordsearchDifficultyTransition.open(previous)
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? WordsearchUi.ChoosingDifficulty ?: return

        when (val destination = WordsearchDifficultyTransition.dismiss(picker)) {
            // Nothing to go back to — the picker was opened before a grid existed,
            // so dismissing it has to produce one.
            null -> generate(picker.currentDifficulty ?: DEFAULT_DIFFICULTY)

            is WordsearchUi.Playing -> {
                _ui.value = destination
                startTimer()
            }

            else -> _ui.value = destination
        }
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    /**
     * Commits [next], promoting it to a finished run when the last word falls.
     *
     * There is no loss condition — wrong drags cost nothing but the tracing — so
     * the only completion is the win, and it is the only way this run ends.
     */
    private fun finish(next: WordsearchState) {
        timerJob?.cancel()
        _ui.value = WordsearchUi.Complete(next)
        viewModelScope.launch {
            clearSlot(next)
            _completions.emit(
                GameResult(
                    gameId = WordsearchDefinition.ID,
                    won = true,
                    durationMs = next.elapsedMs,
                    difficulty = next.difficulty,
                    moves = moveCount,
                )
            )
        }
    }

    /**
     * Clears whichever slot [state] belongs to — finishing the daily puzzle leaves an
     * in-progress regular puzzle exactly where it was, and vice versa (D31's rule).
     */
    private suspend fun clearSlot(state: WordsearchState) {
        if (state.dailyDay != null) store.clearDaily() else store.clearSession()
    }

    /**
     * Routes [state] to its slot. A daily run (decision D32) never touches the
     * regular session: it saves into `daily.<gameId>.*` under its own day, so the
     * home resume cards — which scan the `session.` prefix — never offer it.
     */
    private fun persist(state: WordsearchState) {
        viewModelScope.launch {
            val day = state.dailyDay
            if (day != null) store.saveDaily(day, state.toSession())
            else store.saveSession(state.toSession())
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        (_ui.value as? WordsearchUi.Playing)?.let { persist(it.state) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        val DEFAULT_DIFFICULTY = WordsearchGenerator.DEFAULT_DIFFICULTY
        val DIFFICULTIES = WordsearchGenerator.DIFFICULTIES

        /** The day's puzzle (decision D32) — a mode, listed first in the picker. */
        const val DAILY_NAME = "Daily"

        /** The difficulty the daily is generated at: a fixed, honest engine setting. */
        val DAILY_DIFFICULTY = WordsearchGenerator.DEFAULT_DIFFICULTY

        private const val TICK_MS = 1_000L

        /** The generator loop is milliseconds; this is a backstop, not a budget. */
        private const val GENERATION_TIMEOUT_MS = 10_000L
    }
}

/** Sign of an int for direction stepping: -1, 0, or 1. */
private fun Int.signOf(): Int = when {
    this < 0 -> -1
    this > 0 -> 1
    else -> 0
}
