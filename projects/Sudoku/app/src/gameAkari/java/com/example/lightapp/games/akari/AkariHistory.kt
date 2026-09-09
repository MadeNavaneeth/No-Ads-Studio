package com.example.lightapp.games.akari

/**
 * Undo history for `akari(10)` — one-cell deltas, in memory only (decision D17;
 * market parity G4). A tap cycle touches exactly one cell, so the delta *is* the
 * whole change: there is no atomic multi-step commit to snapshot, and reverting is
 * writing [AkariMove.previousValue] back.
 *
 * Restored sessions start with no history — the save carries the *current* state,
 * and G4's bound is same-session only; [clear] on a new run tightens it to
 * same-run. There is no terminal state to guard here (the win ends the run and
 * leaves the Playing UI, so undo cannot reach across it), unlike blockpuzzle's
 * endless board.
 */
class AkariHistory(private val bound: Int = DEFAULT_BOUND) {

    /** One reversible tap: the cell touched and the value it held before. */
    data class Move(val index: Int, val previousValue: Int)

    private val stack = ArrayDeque<Move>()

    /** Whether a tap can be reverted — the screen reads this to enable UNDO. */
    val canUndo: Boolean get() = stack.isNotEmpty()

    /** Records the change made by a tap. Call only when the value actually changed. */
    fun record(move: Move) {
        stack.addLast(move)
        while (stack.size > bound) stack.removeFirst()
    }

    /** Pops the most recent change, or null when history is empty. */
    fun revert(): Move? = stack.removeLastOrNull()

    /** A new run has no past: undo never reaches across runs. */
    fun clear() = stack.clear()

    companion object {
        /** Bounded at 50 — decision D17, shared with sudoku and blockpuzzle. */
        const val DEFAULT_BOUND = 50
    }
}
