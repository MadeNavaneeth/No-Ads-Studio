package com.example.lightapp.games.blockpuzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightapp.core.GameResult
import com.example.lightapp.studio.persistence.BlockSessionStore
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

/**
 * What the screen is showing. A sealed hierarchy rather than a bag of booleans, so an
 * impossible combination — dealing *and* over — cannot be represented.
 */
sealed interface BlockUi {
    data object Dealing : BlockUi

    /**
     * A live run. Unlike the puzzle games there is no generation-failure state: the
     * deal is a bounded sweep against the live board and its only null is "the board
     * is over by any honest reading", which becomes a finished run instead.
     */
    data class Playing(val state: BlockState) : BlockUi
    data class Complete(val state: BlockState) : BlockUi
}

/**
 * `blockpuzzle(8)` — the studio's first endless game.
 *
 * The loop: drag a tray piece onto the board; it lands where it fits (D33: the piece
 * follows the finger, then resolves by opacity — the app never moves it). Full rows
 * and columns clear together. When the tray empties, three more are dealt against the
 * live board under the D33 placeable-deal contract. When *no* remaining piece fits,
 * the run is over and the score is the record.
 *
 * The clock runs only while the screen is resumed; undo history is in memory only
 * (decision D17) — and deliberately small, since a placement is not reversible by
 * design in most catalogue builds of this genre, but our rules layer is pure enough
 * that undo stays honest: it reverts the board, tray, and score snapshot.
 */
class BlockViewModel(
    private val store: BlockSessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<BlockUi>(BlockUi.Dealing)
    val ui: StateFlow<BlockUi> = _ui.asStateFlow()

    /**
     * Emitted once per finished run, for the shell's `onGameComplete`. An event, not
     * state: replay is 0, a finished run is true at one instant.
     */
    private val _completions = MutableSharedFlow<GameResult>()
    val completions: SharedFlow<GameResult> = _completions.asSharedFlow()

    /** Whether the screen is on top — the clock only runs when it is. */
    private var screenResumed = false

    private var timerJob: Job? = null

    /** Undo history — the bounded, pure stack in [BlockHistory]. */
    private val history = BlockHistory()

    init {
        viewModelScope.launch { restoreOrDeal() }
    }

    // ─── lifecycle ────────────────────────────────────────────────────────────

    fun onScreenResumed() {
        screenResumed = true
        if (_ui.value is BlockUi.Playing) startTimer()
    }

    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        persistNow()
    }

    private suspend fun restoreOrDeal() {
        val saved = store.session().first()
        val state = saved?.let(BlockRestore::stateOrNull)

        when {
            state == null -> newRun(BlockGenerator.DEFAULT_DIFFICULTY)

            else -> {
                _ui.value = BlockUi.Playing(state)
                if (screenResumed) startTimer()
            }
        }
    }

    /**
     * Starts a fresh run. The single difficulty today is `Classic`; [DAILY_NAME]
     * starts the day's deal (decision D32) instead — seeded, in its own slot, never
     * touching the regular run's save.
     */
    fun newRun(label: String) {
        timerJob?.cancel()
        // A new run has no past: undo never reaches across runs.
        history.clear()
        if (label == DAILY_NAME) {
            startDaily()
            return
        }
        dealFresh(label)
    }

    /** Re-runs whatever failed. This game's deal cannot fail into a dead end. */
    fun retryDealing() {
        newRun(
            (_ui.value as? BlockUi.Playing)?.state?.difficulty ?: BlockGenerator.DEFAULT_DIFFICULTY,
        )
    }

    private fun dealFresh(label: String) {
        // An empty board always admits a deal — the 1×1 piece fits in any of 64
        // cells — so the fallback cannot fail; asserted rather than a crash.
        val deal = requireNotNull(
            BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY })
                ?: BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = 1L),
        ) { "an empty board must always admit a deal" }
        val state = BlockState.fresh(
            cells = List(BlockRules.CELLS) { BlockRules.EMPTY },
            tray = deal,
            difficulty = label,
        )
        _ui.value = BlockUi.Playing(state)
        persist(state)
        if (screenResumed) startTimer()
    }

    /**
     * Starts, or resumes, the day's deal — sudoku's D31 daily, generalised by D32.
     * The day is UTC; the deal is seeded with the day number so every device deals
     * the same three pieces until midnight. Today's unfinished daily resumes; an
     * earlier day's leftovers are rejected by the store's day check. The regular
     * run's slot is never touched.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        viewModelScope.launch {
            val saved = store.daily(day).first()
            if (saved != null) {
                val restored = BlockRestore.stateOrNull(saved)
                if (restored != null) {
                    _ui.value = BlockUi.Playing(restored.copy(dailyDay = day))
                    if (screenResumed) startTimer()
                    return@launch
                }
            }
            val deal = BlockGenerator.dailyDeal(day)
            if (deal == null) {
                // Unreachable on an empty board; a fresh deal is the safe fallback.
                dealFresh(DAILY_NAME)
                return@launch
            }
            val state = BlockState.fresh(
                cells = List(BlockRules.CELLS) { BlockRules.EMPTY },
                tray = deal,
                difficulty = DAILY_NAME,
                dailyDay = day,
            )
            _ui.value = BlockUi.Playing(state)
            persist(state)
            if (screenResumed) startTimer()
        }
    }

    /** Today's puzzle identity: the UTC date as a day count since the epoch. */
    private fun dailyUtcDay(): Long =
        java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()

    private fun startTimer() {
        timerJob?.cancel()
        if (!screenResumed) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val playing = _ui.value as? BlockUi.Playing ?: continue
                _ui.value = BlockUi.Playing(playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS))
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    /**
     * Drops [trayIndex] onto the board with its top-left corner at ([row], [col]).
     *
     * Returns whether the piece landed. A piece that does not fit simply refuses —
     * the tray keeps it and the drag snaps back (D33: resolution by opacity, never
     * by travel). This is the game's only wrong move, and it costs nothing but the
     * attempt: nothing turns red, nothing is recorded.
     */
    fun drop(trayIndex: Int, row: Int, col: Int): Boolean {
        val playing = _ui.value as? BlockUi.Playing ?: return false
        val state = playing.state
        val piece = state.tray.getOrNull(trayIndex) ?: return false
        if (!BlockRules.canPlace(state.cells, piece, row, col)) return false
        // Land, clear, score, and refill — one commit.
        val landed = BlockRules.placed(state.cells, piece, row, col)
        val (cleared, lines) = BlockRules.clearFullLines(landed)
        val scored = state.score + BlockRules.scoreFor(piece.size, lines)
        val remaining = state.tray.filterIndexed { i, _ -> i != trayIndex }

        // Refill against the live board when the tray empties (D33); terminality is
        // checked for *both* branches — a run can die mid-tray, when the pieces that
        // remain no longer fit anywhere even though the tray was never refilled.
        val (nextTray, over) = if (remaining.isEmpty()) {
            val deal = BlockGenerator.deal(cleared)
            if (deal == null || !BlockRules.anyPlacement(cleared, deal)) {
                emptyList<BlockRules.Piece>() to true
            } else {
                deal to false
            }
        } else {
            remaining to !BlockRules.anyPlacement(cleared, remaining)
        }

        val next = state.copy(cells = cleared, tray = nextTray, score = scored, over = over)
        // The final placement is not recorded — the final position is terminal and
        // undo never crosses it (see [BlockHistory]).
        history.record(state)
        if (over) {
            finish(next)
        } else {
            _ui.value = BlockUi.Playing(next)
            persist(next)
        }
        return true
    }

    /** Whether a placement can be reverted — the screen reads this to enable UNDO. */
    val undoAvailable: Boolean get() = history.canUndo

    /**
     * Reverts the last landed placement: board, tray (the piece returns to its
     * slot), score, and the clock's elapsed reading — one snapshot, one commit.
     * Bounded, in-memory, same-run only (D17, parity G4).
     */
    fun undo() {
        if (_ui.value !is BlockUi.Playing) return
        val previous = history.revert() ?: return
        _ui.value = BlockUi.Playing(previous)
        persist(previous)
        if (screenResumed) startTimer()
    }

    /**
     * Whether any tray piece fits anywhere — the screen's game-over pre-check, so
     * the board can dim the unplayable tray instead of waiting for the drop that
     * reveals it. Cheap: the tray is three small shapes.
     */
    fun anyMoveAvailable(): Boolean {
        val playing = _ui.value as? BlockUi.Playing ?: return false
        return BlockRules.anyPlacement(playing.state.cells, playing.state.tray)
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    /**
     * Commits a finished run: clears its slot and emits the result. `won` is false
     * by contract — an endless game has no win, and the stats page reads the score
     * through the result's difficulty row.
     */
    private fun finish(next: BlockState) {
        timerJob?.cancel()
        _ui.value = BlockUi.Complete(next)
        viewModelScope.launch {
            clearSlot(next)
            _completions.emit(
                GameResult(
                    gameId = BlockpuzzleDefinition.ID,
                    won = false,
                    durationMs = next.elapsedMs,
                    difficulty = next.difficulty,
                    moves = next.score,
                ),
            )
        }
    }

    /**
     * Clears whichever slot [state] belongs to — finishing the daily deal leaves an
     * in-progress regular run exactly where it was, and vice versa (D31's rule).
     */
    private suspend fun clearSlot(state: BlockState) {
        if (state.dailyDay != null) store.clearDaily() else store.clearSession()
    }

    /**
     * Routes [state] to its slot. A daily run (decision D32) never touches the
     * regular run: it saves into `daily.<gameId>.*` under its own day, so the home
     * resume cards — which scan the `session.` prefix — never offer it.
     */
    private fun persist(state: BlockState) {
        viewModelScope.launch {
            val day = state.dailyDay
            if (day != null) store.saveDaily(day, state.toSession())
            else store.saveSession(state.toSession())
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        (_ui.value as? BlockUi.Playing)?.let { persist(it.state) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    companion object {
        /** The day's deal (decision D32) — a mode, listed first in the picker. */
        const val DAILY_NAME = "Daily"

        private const val TICK_MS = 1_000L
    }
}
