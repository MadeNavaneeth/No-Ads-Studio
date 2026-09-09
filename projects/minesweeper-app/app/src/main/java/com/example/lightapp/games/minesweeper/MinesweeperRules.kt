package com.example.lightapp.games.minesweeper

/**
 * Rules for `minesweeper(10)`. Pure functions — no Android, no Compose — so every
 * one of these is unit-testable on the JVM.
 *
 * Cell values are quad-state (parity M1): [UNKNOWN] un-revealed, [REVEALED] open,
 * [FLAGGED] marked as a mine, [DETONATED] the one mine that ended the run. Mine
 * placement and adjacency counts live in the state; the win condition is "every
 * safe cell revealed", which cannot be checked without knowing the layout, so —
 * like `nonogram(10)` and unlike `sudoku(9)` — the game holds its mine positions
 * in memory.
 *
 * **There is no red in the rules layer.** The detonated cell is the one red element
 * on the screen (method §6, the game's single red budget), and it is terminal: red
 * never appears during normal play, only at the instant a run is lost.
 */
object MinesweeperRules {

    const val SIZE = 9
    const val CELLS = SIZE * SIZE
    const val MINE_COUNT = 10

    const val UNKNOWN = 0
    const val REVEALED = 1
    const val FLAGGED = 2
    const val DETONATED = 3

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE

    /** The eight neighbours of a cell, edge-clipped — 3 at a corner, 8 in the middle. */
    fun neighbours(index: Int): List<Int> {
        val r = rowOf(index)
        val c = colOf(index)
        val out = mutableListOf<Int>()
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until SIZE && nc in 0 until SIZE) out.add(nr * SIZE + nc)
            }
        }
        return out
    }

    /** Adjacency count for every cell — computed once at generation, never per frame. */
    fun adjacency(mineAt: List<Boolean>): List<Int> = List(CELLS) { i ->
        neighbours(i).count { mineAt[it] }
    }

    /**
     * Reveal [index] on [cells], returning the new board, or `null` when the move
     * is illegal (a revealed or flagged cell rejects the tap; the caller records
     * no history and fires no haptic on null).
     *
     * A zero-adjacency reveal flood-fills its connected zero region plus the ring
     * of numbered cells around it — the one mechanic Minesweeper contributes that
     * no shipped game has.
     */
    fun reveal(cells: List<Int>, mineAt: List<Boolean>, index: Int): List<Int>? {
        when (cells[index]) {
            REVEALED, DETONATED -> return null
            FLAGGED -> return null
        }
        if (mineAt[index]) {
            // Terminal: the tapped mine detonates, and every other mine is shown
            // so the board reads as finished rather than half-open.
            return cells.toMutableList().also { out ->
                out[index] = DETONATED
                for (i in 0 until CELLS) {
                    if (i != index && mineAt[i] && out[i] != FLAGGED) out[i] = REVEALED
                }
            }
        }
        val next = cells.toMutableList()
        // Iterative flood fill — a recursive one is fine at 81 cells, but the
        // iterative form cannot be misread as unbounded.
        val frontier = ArrayDeque(listOf(index))
        while (frontier.isNotEmpty()) {
            val cell = frontier.removeFirst()
            if (next[cell] == REVEALED) continue
            next[cell] = REVEALED
            if (adjacencyOf(mineAt, cell) == 0) {
                for (n in neighbours(cell)) {
                    if (next[n] == UNKNOWN) frontier.addLast(n)
                }
            }
        }
        return next
    }

    /** Adjacency of one cell, for callers that hold the board rather than the cache. */
    fun adjacencyOf(mineAt: List<Boolean>, index: Int): Int =
        neighbours(index).count { mineAt[it] }

    /**
     * Long-press toggles the flag. A revealed cell rejects it (null) — flagging is
     * how a player reasons about *unknown* ground, and overwriting a reveal would
     * destroy information as a side effect.
     */
    fun toggleFlag(cells: List<Int>, index: Int): List<Int>? {
        if (cells[index] == REVEALED || cells[index] == DETONATED) return null
        return cells.toMutableList().also {
            it[index] = if (it[index] == FLAGGED) UNKNOWN else FLAGGED
        }
    }

    /**
     * Win: every safe cell revealed. Flags are *not* required — a player who
     * revealed everything and flagged nothing has won, and refusing that would
     * make the flag a chore rather than a tool.
     */
    fun isWin(cells: List<Int>, mineAt: List<Boolean>): Boolean =
        cells.indices.all { mineAt[it] || cells[it] == REVEALED }

    /**
     * The MINE readout: mines not yet flagged. Counts flags on non-mines too —
     * a wrong flag spends the number honestly, which is the readout's job.
     */
    fun minesRemaining(mineAt: List<Boolean>, cells: List<Int>): Int =
        MINE_COUNT - cells.count { it == FLAGGED }

    /**
     * Progress, 0..1 — the quantity from `game-design-method.md` §2. Revealed safe
     * cells over total safe cells, so a fresh board reads 0% and a won one 100%.
     * The detonated cell is not safe, so a lost run never reads as progress.
     */
    fun completion(cells: List<Int>, mineAt: List<Boolean>): Float {
        val safe = CELLS - MINE_COUNT
        val revealed = cells.indices.count { !mineAt[it] && cells[it] == REVEALED }
        return revealed.toFloat() / safe
    }
}
