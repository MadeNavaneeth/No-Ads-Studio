package com.example.lightapp.games.minesweeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightapp.core.GameResult
import com.example.lightapp.studio.persistence.MinesweeperSessionStore
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
 * impossible combination — generating *and* complete — cannot be represented.
 *
 * Minesweeper has no `Generating` state: the board is known the instant the difficulty
 * is picked, because the layout is only placed at first tap. That is one fewer wait
 * screen than sudoku and nonogram need, and it is honest — there is genuinely nothing
 * to compute yet.
 */
sealed interface MinesweeperUi {
    data class Playing(val state: MinesweeperState) : MinesweeperUi

    /**
     * The difficulty picker, and what to go back to if it is dismissed.
     *
     * [returnTo] is never null-to-generate the way the other games need: a fresh board
     * is already `Playing`, so dismissing the picker always has somewhere to go back to.
     */
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: MinesweeperUi?,
    ) : MinesweeperUi

    data class Complete(val state: MinesweeperState) : MinesweeperUi
}

/** The difficulty carried by whichever variant has one, for the picker to show as current. */
internal fun MinesweeperUi.difficultyOrNull(): String? = when (this) {
    is MinesweeperUi.Playing -> state.difficulty
    is MinesweeperUi.Complete -> state.difficulty
    is MinesweeperUi.ChoosingDifficulty -> currentDifficulty
}

/** The picker's two transitions, as pure functions — asserted directly, see the tests. */
internal object MinesweeperDifficultyTransition {

    fun open(from: MinesweeperUi): MinesweeperUi.ChoosingDifficulty = MinesweeperUi.ChoosingDifficulty(
        currentDifficulty = from.difficultyOrNull(),
        returnTo = from.takeUnless { it is MinesweeperUi.ChoosingDifficulty },
    )

    /** Where dismissing [picker] should land. */
    fun dismiss(picker: MinesweeperUi.ChoosingDifficulty): MinesweeperUi? = picker.returnTo
}

/**
 * `minesweeper(10)`.
 *
 * The mine layout is placed on the player's first reveal, off the main thread, so
 * first-tap safety is structural (see MinesweeperGenerator). Undo history is in memory
 * only (decision D17). Everything else persists.
 */
class MinesweeperViewModel(
    private val store: MinesweeperSessionStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<MinesweeperUi>(MinesweeperUi.Playing(MinesweeperState.fresh(DEFAULT_DIFFICULTY)))
    val ui: StateFlow<MinesweeperUi> = _ui.asStateFlow()

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

    private val history = ArrayDeque<MineMove>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch { restoreOrGenerate() }
    }

    // ─── lifecycle ────────────────────────────────────────────────────────────

    /** Called when the game screen resumes. Resumes the clock if a game is in progress. */
    fun onScreenResumed() {
        screenResumed = true
        if (_ui.value is MinesweeperUi.Playing) startTimer()
    }

    /** Called when the game screen pauses. Stops the clock and saves. */
    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        val saved = store.session().first()
        val state = saved?.let(MinesweeperRestore::stateOrNull)

        when {
            state == null -> newGame(DEFAULT_DIFFICULTY)

            // A finished board — won or detonated — should not be restored as
            // in-progress; the completion already recorded its result.
            state.isComplete || state.detonated -> {
                viewModelScope.launch { store.clearSession() }
                newGame(state.difficulty)
            }

            else -> _ui.value = MinesweeperUi.Playing(state)
        }
    }

    /**
     * Starts a fresh board at [difficultyName] — one of [DIFFICULTIES], [DAILY_NAME]
     * for the day's puzzle (decision D32), anything else falling back to the default.
     * There is nothing to generate: the layout arrives at first tap.
     */
    fun newGame(difficultyName: String) {
        timerJob?.cancel()
        history.clear()
        moveCount = 0

        if (difficultyName == DAILY_NAME) {
            startDaily()
            return
        }

        val difficulty = DIFFICULTIES.firstOrNull { it == difficultyName } ?: DEFAULT_DIFFICULTY
        val fresh = MinesweeperState.fresh(difficulty)
        _ui.value = MinesweeperUi.Playing(fresh)
        persist(fresh)
    }

    /**
     * Starts, or resumes, the day's board — sudoku's D31 daily, generalised by
     * decision D32, with one minesweeper-specific twist: **the layout cannot be
     * deterministic from the day alone.** First-tap safety means mines are placed
     * only when the first reveal lands, so the seed is [day, first tap]: the same
     * day and the same opening always produce the same board, which is the honest
     * limit of determinism for this game. The stored daily board only ever exists
     * after a first reveal (or as a fresh board with no layout yet).
     *
     * The day is UTC (D26's amendment); today's unfinished daily resumes, an earlier
     * day's leftovers are rejected by the store's day check, and the regular session
     * slot is never touched.
     */
    private fun startDaily() {
        val day = dailyUtcDay()
        viewModelScope.launch {
            val saved = store.daily(day).first()
            if (saved != null) {
                // A fresh daily board (no layout yet) is valid — mines arrive at first
                // tap — and MinesweeperRestore would reject it for the empty mine list,
                // so it is handled here rather than through the restore step.
                if (!saved.mines.any { it == 1 }) {
                    _ui.value = MinesweeperUi.Playing(
                        MinesweeperState.fresh(DAILY_NAME, dailyDay = day)
                    )
                    return@launch
                }
                val restored = MinesweeperRestore.stateOrNull(saved)?.copy(dailyDay = day)
                if (restored != null) {
                    _ui.value = MinesweeperUi.Playing(restored)
                    return@launch
                }
            }
            val fresh = MinesweeperState.fresh(DAILY_NAME, dailyDay = day)
            _ui.value = MinesweeperUi.Playing(fresh)
            persist(fresh)
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
                val playing = _ui.value as? MinesweeperUi.Playing ?: continue
                _ui.value = MinesweeperUi.Playing(
                    playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS)
                )
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    fun select(index: Int) {
        val playing = _ui.value as? MinesweeperUi.Playing ?: return
        _ui.value = MinesweeperUi.Playing(playing.state.copy(selected = index))
    }

    /**
     * Tap: reveal. On a fresh board this first places the mine layout around the tap
     * (first-tap-safe by construction), then reveals — through the flood fill when
     * the opening lands on a zero region.
     *
     * @return true when the reveal was the mine, so the screen can fire the distinct
     *         conflict haptic. The board itself turns red only on that one cell.
     */
    fun press(index: Int): Boolean {
        val playing = _ui.value as? MinesweeperUi.Playing ?: return false
        var state = playing.state.copy(selected = index)

        // The layout does not exist until the first reveal asks for it. A daily run
        // seeds it with day and tap (see startDaily) so the day's board is stable.
        if (!state.layoutPlaced) {
            state = state.copy(mines = placeLayout(index, state.difficulty, state.dailyDay))
        }
        val revealed = MinesweeperRules.reveal(state.cells, state.mines, index) ?: return false

        record(state, index)
        moveCount++
        val detonated = revealed[index] == MinesweeperRules.DETONATED
        finish(
            if (detonated) state.copy(cells = revealed, mistakes = state.mistakes + 1)
            else state.copy(cells = revealed)
        )
        return detonated
    }

    /**
     * Long-press: toggle the flag. Silent on revealed cells — flagging is reasoning
     * about unknown ground, never a rewrite of what is already open.
     */
    fun mark(index: Int) {
        val playing = _ui.value as? MinesweeperUi.Playing ?: return
        val state = playing.state.copy(selected = index)
        // A flag before the first reveal is still a legal move (the layout just does
        // not exist yet), so no placement happens here.
        val flagged = MinesweeperRules.toggleFlag(state.cells, index) ?: return
        record(state, index)
        moveCount++
        finish(state.copy(cells = flagged))
    }

    fun undo() {
        val playing = _ui.value as? MinesweeperUi.Playing ?: return
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

    /**
     * Places the layout for a first tap at [index].
     *
     * No dispatcher ceremony: the placement is one 81-element shuffle, which costs
     * microseconds. The bounded-retry loops the other games run are what the
     * off-main-thread rule exists for; a single pass has nothing to bound.
     */
    private fun placeLayout(index: Int, difficulty: String, dailyDay: Long? = null): List<Boolean> =
        MinesweeperGenerator.layoutFor(
            index,
            difficulty,
            // [day, tap] determinism for the daily (decision D32): the same opening
            // on the same day always meets the same board.
            seed = dailyDay?.let { it * MinesweeperRules.CELLS + index },
        )
            ?: List(MinesweeperRules.CELLS) { false }

    // ─── the picker ───────────────────────────────────────────────────────────

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is MinesweeperUi.ChoosingDifficulty) return

        // The clock does not run behind a modal.
        timerJob?.cancel()
        _ui.value = MinesweeperDifficultyTransition.open(previous)
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? MinesweeperUi.ChoosingDifficulty ?: return
        when (val destination = MinesweeperDifficultyTransition.dismiss(picker)) {
            is MinesweeperUi.Playing -> {
                _ui.value = destination
                startTimer()
            }
            is MinesweeperUi.Complete -> _ui.value = destination
            // open() never stores null or a nested picker as the return destination;
            // these branches only keep the when exhaustive without inventing a
            // destination that was never there.
            else -> return
        }
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    private fun record(state: MinesweeperState, index: Int) {
        history.addLast(
            MineMove(
                index = index,
                previousCell = state.cells[index],
                previousMistakes = state.mistakes,
                previousSelected = state.selected,
            )
        )
        moveCount++
        // Bounded at 50 — decision D17.
        while (history.size > MineMove.MAX_HISTORY) history.removeFirst()
    }

    /**
     * Commits [next], promoting it to a finished run on a detonation or a win.
     *
     * Detonation is checked first: the mistake limit has no separate life here —
     * in this game a mistake *is* the detonation, and it ends the run outright.
     * A lost run emits its result too, so the stats page counts it played but
     * not won.
     */
    private fun finish(next: MinesweeperState) {
        when {
            next.detonated -> complete(next, won = false)
            next.isComplete -> complete(next, won = true)
            else -> {
                _ui.value = MinesweeperUi.Playing(next)
                persist(next)
            }
        }
    }

    private fun complete(next: MinesweeperState, won: Boolean) {
        timerJob?.cancel()
        _ui.value = MinesweeperUi.Complete(next)
        viewModelScope.launch {
            clearSlot(next)
            _completions.emit(
                GameResult(
                    gameId = MinesweeperDefinition.ID,
                    won = won,
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
    private suspend fun clearSlot(state: MinesweeperState) {
        if (state.dailyDay != null) store.clearDaily() else store.clearSession()
    }

    /**
     * Routes [state] to its slot. A daily run (decision D32) never touches the
     * regular session: it saves into `daily.<gameId>.*` under its own day, so the
     * home resume cards — which scan the `session.` prefix — never offer it.
     */
    private fun persist(state: MinesweeperState) {
        viewModelScope.launch {
            val day = state.dailyDay
            if (day != null) store.saveDaily(day, state.toSession())
            else store.saveSession(state.toSession())
        }
    }

    /** Saves current progress. Called when the screen is left. */
    fun persistNow() {
        (_ui.value as? MinesweeperUi.Playing)?.let { persist(it.state) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    companion object {
        val DEFAULT_DIFFICULTY = MinesweeperGenerator.DEFAULT_DIFFICULTY
        val DIFFICULTIES = MinesweeperGenerator.DIFFICULTIES

        /** The day's puzzle (decision D32) — a mode, listed first in the picker. */
        const val DAILY_NAME = "Daily"

        private const val TICK_MS = 1_000L
    }
}
