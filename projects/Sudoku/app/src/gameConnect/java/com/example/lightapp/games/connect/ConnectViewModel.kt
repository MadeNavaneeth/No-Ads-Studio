package com.example.lightapp.games.connect

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
sealed interface ConnectUi {
    data object Generating : ConnectUi
    data object GenerationFailed : ConnectUi
    data class Playing(val state: ConnectState) : ConnectUi

    /**
     * The difficulty picker, and what to go back to if it is dismissed.
     *
     * [returnTo] is never `Generating`: opening the picker cancels the in-flight
     * generation, so going "back" to it would mean waiting on work that no longer
     * exists — the two-tap dead end this shape exists to prevent.
     */
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: ConnectUi?,
    ) : ConnectUi

    data class Complete(val state: ConnectState) : ConnectUi
}

/** The difficulty carried by whichever variant has one, for the picker to show as current. */
internal fun ConnectUi.difficultyOrNull(): String? = when (this) {
    is ConnectUi.Playing -> state.difficulty
    is ConnectUi.Complete -> state.difficulty
    is ConnectUi.ChoosingDifficulty -> currentDifficulty
    else -> null
}

/** The picker's two transitions, as pure functions — asserted directly, see the tests. */
internal object ConnectDifficultyTransition {

    fun open(from: ConnectUi): ConnectUi.ChoosingDifficulty = ConnectUi.ChoosingDifficulty(
        currentDifficulty = from.difficultyOrNull(),
        returnTo = from.takeUnless { it is ConnectUi.Generating || it is ConnectUi.ChoosingDifficulty },
    )

    /** Where dismissing [picker] should land, or `null` to generate a fresh puzzle. */
    fun dismiss(picker: ConnectUi.ChoosingDifficulty): ConnectUi? = picker.returnTo
}

/**
 * `connect(7)`.
 *
 * Generation runs on [Dispatchers.Default] — the Hamiltonian growth plus the uniqueness
 * proof is the most expensive generation in the studio, and the screen shows a
 * non-blocking pulse until it lands. The live walk is input state: it never persists,
 * and undo history is in memory only (decision D17). Everything else persists.
 */
class ConnectViewModel(
    private val store: SessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<ConnectUi>(ConnectUi.Generating)
    val ui: StateFlow<ConnectUi> = _ui.asStateFlow()

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

    val undoAvailable: Boolean get() = history.isNotEmpty()

    private val history = ArrayDeque<ConnectMove>()
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
        if (_ui.value is ConnectUi.Playing) startTimer()
    }

    /** Called when the game screen pauses. Stops the clock, drops the walk, and saves. */
    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        // The walk is a gesture, not a decision — leaving the screen ends it.
        abandonWalk()
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        val saved = store.connectSession().first()
        val state = saved?.let(ConnectRestore::stateOrNull)

        when {
            state == null -> generate(DEFAULT_DIFFICULTY)

            // A completed board should not be restored as in-progress.
            state.isComplete -> {
                viewModelScope.launch { store.clearConnectSession() }
                generate(state.difficulty)
            }

            else -> {
                _ui.value = ConnectUi.Playing(state)
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

        _ui.value = ConnectUi.Generating
        generateJob = viewModelScope.launch {
            val difficulty = DIFFICULTIES.firstOrNull { it == difficultyName } ?: DEFAULT_DIFFICULTY

            // The Hamiltonian growth is milliseconds; the uniqueness proof is the
            // expensive part. The budget inside the generator is the real bound —
            // this timeout is a backstop so a pathological seed can only ever cost
            // the player a retry, not a frozen screen.
            val board = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    ConnectGenerator.generate(difficulty)
                }
            }

            if (board == null || board.size != ConnectRules.CELLS) {
                _ui.value = ConnectUi.GenerationFailed
                return@launch
            }

            val state = ConnectState.fresh(board, difficulty)
            _ui.value = ConnectUi.Playing(state)
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
     * seed means (D26's no-time-zone-calendar rule). The board is the generator
     * seeded with the day number, so every device generates the same puzzle until
     * midnight UTC. Today's unfinished daily resumes; an earlier day's leftovers are
     * rejected by the day check in the store's daily reader and simply regenerated.
     * The regular session slot is never touched.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        _ui.value = ConnectUi.Generating
        generateJob = viewModelScope.launch {
            val saved = store.dailyConnect(day).first()
            if (saved != null) {
                val restored = ConnectRestore.stateOrNull(saved)?.copy(dailyDay = day)
                if (restored != null && !restored.isComplete) {
                    _ui.value = ConnectUi.Playing(restored)
                    if (screenResumed) startTimer()
                    return@launch
                }
            }

            val board = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    ConnectGenerator.generate(DAILY_DIFFICULTY, seed = day)
                }
            }

            if (board == null || board.size != ConnectRules.CELLS) {
                _ui.value = ConnectUi.GenerationFailed
                return@launch
            }

            val state = ConnectState.fresh(board, DAILY_NAME, dailyDay = day)
            _ui.value = ConnectUi.Playing(state)
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
                val playing = _ui.value as? ConnectUi.Playing ?: continue
                _ui.value = ConnectUi.Playing(
                    playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS)
                )
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    /**
     * Tap: either starts a walk on an endpoint or ends the active one where it stands
     * (committing segments already laid). Empty ground and foreign endpoints are no-ops —
     * there is nothing to be wrong about.
     */
    fun press(index: Int) {
        val playing = _ui.value as? ConnectUi.Playing ?: return
        val state = playing.state

        if (state.walkActive) {
            // A tap during a walk commits what has been laid and ends it. The
            // segments are already on the board; this just drops the walk.
            finish(state.copy(path = emptyList()))
            return
        }
        val path = ConnectRules.start(state.cells, index) ?: return
        _ui.value = ConnectUi.Playing(state.copy(path = path))
    }

    /**
     * One step of a drag. [ConnectRules.step] handles every branch — extend, retract,
     * trim, adopt, lay — and returns null for ground that is not this walk's, which
     * the screen ignores rather than treats as an error.
     *
     * Segments are laid on the board *as the walk passes*, so a lift at any moment
     * already leaves a consistent board — commit and abandon are both just "drop the
     * walk". Reaching the partner endpoint completes the board in the same step, and
     * the run ends there rather than waiting for the finger to lift.
     */
    fun dragTo(index: Int) {
        val playing = _ui.value as? ConnectUi.Playing ?: return
        val state = playing.state
        if (!state.walkActive) return

        val step = ConnectRules.step(state.cells, state.path, index) ?: return
        if (step.changed) {
            // Each board-altering step records history: a trim records one entry per
            // cleared cell, so undo reverses the trim cell by cell.
            for (cell in state.cells.indices) {
                if (state.cells[cell] != step.cells[cell]) {
                    record(cell, state.cells[cell])
                }
            }
            moveCount++
        }
        val next = state.copy(cells = step.cells, path = step.path)
        if (ConnectRules.isWin(next.cells)) {
            // The walk reached its partner and the board is complete.
            finish(next.copy(path = emptyList()))
        } else {
            _ui.value = ConnectUi.Playing(next)
        }
    }

    /**
     * Ends the active walk, committing what it has laid. The segments are already on
     * the board, so this only drops the walk — and a one-cell walk laid nothing.
     */
    fun endWalk() {
        val playing = _ui.value as? ConnectUi.Playing ?: return
        val state = playing.state
        if (!state.walkActive) return
        finish(state.copy(path = emptyList()))
    }

    /**
     * Drops the walk, used on pause. Deliberately **keeps** the laid segments: they
     * were committed as the walk passed, and trimming them back would also erase
     * cells adopted from earlier walks — destroying work to honour a gesture the
     * player never finished is the wrong trade.
     */
    private fun abandonWalk() {
        val playing = _ui.value as? ConnectUi.Playing ?: return
        val state = playing.state
        if (!state.walkActive) return
        _ui.value = ConnectUi.Playing(state.copy(path = emptyList()))
    }

    fun undo() {
        val playing = _ui.value as? ConnectUi.Playing ?: return
        if (playing.state.walkActive) return // the walk is its own undo
        val move = history.removeLastOrNull() ?: return
        val state = playing.state
        finish(
            state.copy(
                cells = state.cells.toMutableList().also { it[move.index] = move.previousValue },
            ),
        )
    }

    // ─── the picker ───────────────────────────────────────────────────────────

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is ConnectUi.ChoosingDifficulty) return

        // The clock does not run behind a modal, and an in-flight generation is
        // abandoned rather than left to land on top of the picker.
        timerJob?.cancel()
        generateJob?.cancel()

        _ui.value = ConnectDifficultyTransition.open(previous)
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? ConnectUi.ChoosingDifficulty ?: return

        when (val destination = ConnectDifficultyTransition.dismiss(picker)) {
            // Nothing to go back to — the picker was opened before a puzzle existed,
            // so dismissing it has to produce one.
            null -> generate(picker.currentDifficulty ?: DEFAULT_DIFFICULTY)

            is ConnectUi.Playing -> {
                _ui.value = destination
                startTimer()
            }

            else -> _ui.value = destination
        }
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    private fun record(index: Int, previousValue: Int) {
        history.addLast(ConnectMove(index = index, previousValue = previousValue))
        // Bounded at 50 — decision D17.
        while (history.size > ConnectMove.MAX_HISTORY) history.removeFirst()
    }

    /**
     * Commits [next], promoting it to a finished run when the board is solved.
     *
     * There is no loss condition here — every move is retractable — so the only
     * completion is the win, and it is the only way this run ends.
     */
    private fun finish(next: ConnectState) {
        if (next.isComplete) {
            timerJob?.cancel()
            _ui.value = ConnectUi.Complete(next)
            viewModelScope.launch {
                clearSlot(next)
                _completions.emit(
                    GameResult(
                        gameId = ConnectDefinition.ID,
                        won = true,
                        durationMs = next.elapsedMs,
                        difficulty = next.difficulty,
                        moves = moveCount,
                    )
                )
            }
        } else {
            _ui.value = ConnectUi.Playing(next)
            persist(next)
        }
    }

    /**
     * Clears whichever slot [state] belongs to — finishing the daily puzzle leaves an
     * in-progress regular puzzle exactly where it was, and vice versa (D31's rule).
     */
    private suspend fun clearSlot(state: ConnectState) {
        if (state.dailyDay != null) store.clearDailyConnect() else store.clearConnectSession()
    }

    /**
     * Routes [state] to its slot. A daily run (decision D32) never touches the
     * regular session: it saves into `daily.<gameId>.*` under its own day, so the
     * home resume cards — which scan the `session.` prefix — never offer it.
     */
    private fun persist(state: ConnectState) {
        viewModelScope.launch {
            val day = state.dailyDay
            if (day != null) store.saveDailyConnect(day, state.toSession())
            else store.saveConnectSession(state.toSession())
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        (_ui.value as? ConnectUi.Playing)?.let { persist(it.state) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        val DEFAULT_DIFFICULTY = ConnectGenerator.DEFAULT_DIFFICULTY
        val DIFFICULTIES = ConnectGenerator.DIFFICULTIES

        /** The day's puzzle (decision D32) — a mode, listed first in the picker. */
        const val DAILY_NAME = "Daily"

        /** The difficulty the daily is generated at: a fixed, honest engine setting. */
        val DAILY_DIFFICULTY = ConnectGenerator.DEFAULT_DIFFICULTY

        private const val TICK_MS = 1_000L

        /** The generator loop is milliseconds with its own budget; this is a backstop. */
        private const val GENERATION_TIMEOUT_MS = 10_000L
    }
}
