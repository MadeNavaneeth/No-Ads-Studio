package com.example.lightapp.games.blockpuzzle

/**
 * Undo history for `blockpuzzle(8)` — whole-state snapshots, in memory only
 * (decision D17; market parity G4, the one aid this genre is allowed).
 *
 * A snapshot rather than a delta is the honest encoding: a placement commits land
 * + clear + score + tray removal + possible refill as one atomic step, and
 * reverting it piecemeal would re-derive the clear logic for no gain. Restored
 * sessions start with no history — the save carries the *current* state, and G4's
 * bound is same-session only; [clear] on a new run tightens it to same-run.
 *
 * A terminal position is never recorded. `[over]` is terminal by [BlockState]'s
 * contract and the genre's: a finished run has been emitted and its slot cleared,
 * so resurrecting it through undo would farm stats.
 */
class BlockHistory(private val bound: Int = DEFAULT_BOUND) {

    private val stack = ArrayDeque<BlockState>()

    /** Whether a placement can be reverted — the screen reads this to enable UNDO. */
    val canUndo: Boolean get() = stack.isNotEmpty()

    /** Records the position *before* a placement, unless it is terminal. */
    fun record(state: BlockState) {
        if (state.over) return
        stack.addLast(state)
        while (stack.size > bound) stack.removeFirst()
    }

    /** Pops the most recent recorded position, or null when history is empty. */
    fun revert(): BlockState? = stack.removeLastOrNull()

    /** A new run has no past: undo never reaches across runs. */
    fun clear() = stack.clear()

    companion object {
        /** Bounded at 50 — decision D17, shared with sudoku. */
        const val DEFAULT_BOUND = 50
    }
}
