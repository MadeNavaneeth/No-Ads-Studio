package com.example.lightapp.games.sudoku

/**
 * Conflict detection — decision D20.
 *
 * A conflict is a duplicate within a row, column, or 3×3 box. Note what this
 * deliberately does **not** do: compare against the solution.
 *
 * That has a real consequence. The app never holds the solved grid in memory, so there
 * is no way to cheat by reading it, and no branch that could leak it into the UI. The
 * tradeoff is that a value which is wrong but not yet conflicting goes unflagged — which
 * is the honest behaviour of a Sudoku that refuses to know the answer.
 *
 * Pure functions on a flat 81-cell list. No Android, no allocation per frame: peer sets
 * are precomputed once at class load rather than derived on every check.
 */
object Conflicts {

    const val SIZE = 9
    const val CELLS = SIZE * SIZE
    const val BOX = 3

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE
    fun boxOf(index: Int) = (rowOf(index) / BOX) * BOX + (colOf(index) / BOX)

    /**
     * The 20 cells sharing a row, column, or box with [index], excluding itself.
     * Precomputed — this is called for every cell on every conflict pass.
     */
    private val PEERS: Array<IntArray> = Array(CELLS) { i ->
        val peers = ArrayList<Int>(20)
        for (j in 0 until CELLS) {
            if (j == i) continue
            if (rowOf(j) == rowOf(i) || colOf(j) == colOf(i) || boxOf(j) == boxOf(i)) {
                peers += j
            }
        }
        peers.toIntArray()
    }

    fun peersOf(index: Int): IntArray = PEERS[index]

    /**
     * True when the value at [index] duplicates a peer. An empty cell never conflicts.
     */
    fun conflictsAt(grid: List<Int>, index: Int): Boolean {
        val value = grid[index]
        if (value == 0) return false
        for (peer in PEERS[index]) {
            if (grid[peer] == value) return true
        }
        return false
    }

    /** Every index currently in conflict. */
    fun allConflicts(grid: List<Int>): Set<Int> {
        val out = HashSet<Int>()
        for (i in 0 until CELLS) {
            if (conflictsAt(grid, i)) out += i
        }
        return out
    }

    /**
     * True when every cell is filled and nothing conflicts.
     *
     * Both halves matter: a full grid with a duplicate is not a win, and an empty grid
     * with no duplicates is not either.
     */
    fun isSolved(grid: List<Int>): Boolean =
        grid.none { it == 0 } && allConflicts(grid).isEmpty()

    /**
     * How many of each digit are still to be placed — `1..9` mapped to `0..9`.
     *
     * This is the number the pad shows beneath each key, and it is the most useful piece of
     * information a Sudoku interface can give away for free: it turns "which digit should I
     * try next" from a counting exercise into a glance. Exactly the kind of exposed mechanism
     * `nothing-study.md` §1 argues for — the machine's state, composed rather than dumped.
     *
     * Clamped at zero on purpose. A grid mid-mistake can hold ten of a digit, because
     * conflicts are permitted rather than blocked, and "-1 left" is not a thing to tell
     * someone. One pass over the grid rather than nine.
     */
    fun remainingCounts(grid: List<Int>): Map<Int, Int> {
        val placed = IntArray(SIZE + 1)
        for (value in grid) {
            if (value in 1..SIZE) placed[value]++
        }
        return (1..SIZE).associateWith { digit -> (SIZE - placed[digit]).coerceAtLeast(0) }
    }

    /**
     * Digits that already appear nine times, so the number pad can dim them.
     * Dimmed, not disabled — a dimmed digit already communicates the fact.
     */
    fun exhaustedDigits(grid: List<Int>): Set<Int> =
        remainingCounts(grid).filterValues { it == 0 }.keys
}
