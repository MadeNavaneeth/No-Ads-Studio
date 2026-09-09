package com.example.lightapp.games.binairo

/**
 * Rules for `binairo(10)`. Pure functions — no Android, no Compose — so every one
 * of these is unit-testable on the JVM.
 *
 * The board is a 10×10 grid the player fills with two states — ONE and ZERO — so
 * that three constraints hold: no three consecutive cells in any row or column
 * agree, every row and column holds exactly five of each, and no two rows (or
 * columns) are identical. It is the studio's only game that is *monochrome by
 * definition*: the puzzle's own subject is a two-state grid, which is exactly
 * what this design language draws.
 *
 * **Cell vocabulary** — one array, decision D7's boring shape:
 *
 * | value | meaning |
 * |---|---|
 * | [EMPTY] | undecided — a player toggle target, unless the cell is a given |
 * | [ONE] | a filled cell — drawn as a solid dot |
 * | [ZERO] | an empty cell — drawn as a hollow ring |
 *
 * **Zero red, refusal structural (parity A5, method §6).** A tap on a given does
 * nothing — the refusal is structural, akari's walls exactly. A move that breaks
 * a constraint is *legal to place*: three dots in a row are visible on the board,
 * and the win check refuses the board until the contradiction resolves. Nothing
 * announces an error because nothing is an error; an unsolved board is simply
 * unsolved.
 */
object BinairoRules {

    const val SIZE = 10
    const val CELLS = SIZE * SIZE

    /** Each line holds SIZE / 2 of each state. The board size is even by construction. */
    const val HALF = SIZE / 2

    const val EMPTY = 0
    const val ONE = 1
    const val ZERO = 2

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE

    /** The cell indices of one row or column line, by line index. Rows 0–9, columns 10–19. */
    const val LINE_COUNT = SIZE * 2

    fun lineCells(line: Int): IntArray {
        val out = IntArray(SIZE)
        if (line < SIZE) {
            for (c in 0 until SIZE) out[c] = line * SIZE + c
        } else {
            val col = line - SIZE
            for (r in 0 until SIZE) out[r] = r * SIZE + col
        }
        return out
    }

    // ─── per-line violations ──────────────────────────────────────────────────

    /**
     * Cells belonging to a run of three or more consecutive equal values in some
     * row or column. The board's visible contradiction — three dots in a row —
     * found by looking, never announced.
     */
    fun runViolationCells(grid: List<Int>): Set<Int> {
        val out = mutableSetOf<Int>()
        for (line in 0 until LINE_COUNT) {
            val cells = lineCells(line)
            var runStart = 0
            while (runStart < SIZE) {
                val v = grid[cells[runStart]]
                if (v == EMPTY) { runStart++; continue }
                var end = runStart + 1
                while (end < SIZE && grid[cells[end]] == v) end++
                if (end - runStart >= 3) {
                    for (k in runStart until end) out += cells[k]
                }
                runStart = end
            }
        }
        return out
    }

    /**
     * Lines holding more than [HALF] of one state. Over-balance is legal play —
     * the player resolves it by flipping cells back — but it blocks the win.
     */
    fun overBalancedLines(grid: List<Int>): Set<Int> {
        val out = mutableSetOf<Int>()
        for (line in 0 until LINE_COUNT) {
            val cells = lineCells(line)
            var ones = 0
            var zeros = 0
            for (i in cells) {
                if (grid[i] == ONE) ones++
                if (grid[i] == ZERO) zeros++
            }
            if (ones > HALF || zeros > HALF) out += line
        }
        return out
    }

    /**
     * The indices of the first line identical to another *decided* line — rows
     * among rows, columns among columns. The third binairo constraint, checked
     * only when both lines involved are fully decided; a partial line cannot yet
     * be called a duplicate.
     */
    fun duplicateLine(grid: List<Int>): Pair<Int, Int>? {
        for (a in 0 until LINE_COUNT) {
            for (b in a + 1 until LINE_COUNT) {
                if (sameKind(a, b) && linesIdentical(grid, a, b)) return a to b
            }
        }
        return null
    }

    private fun sameKind(a: Int, b: Int) = (a < SIZE) == (b < SIZE)

    private fun linesIdentical(grid: List<Int>, a: Int, b: Int): Boolean {
        val ca = lineCells(a)
        val cb = lineCells(b)
        for (k in 0 until SIZE) {
            val va = grid[ca[k]]
            val vb = grid[cb[k]]
            if (va == EMPTY || vb == EMPTY) return false
            if (va != vb) return false
        }
        return true
    }

    // ─── input ────────────────────────────────────────────────────────────────

    /**
     * The tap cycle: an undecided cell gains a ONE, a ONE flips to a ZERO, a
     * ZERO returns to undecided. The caller treats a given as outside the cycle —
     * the structural refusal — exactly as akari's walls refuse.
     */
    fun cycle(value: Int): Int = when (value) {
        EMPTY -> ONE
        ONE -> ZERO
        else -> EMPTY
    }

    // ─── the win ──────────────────────────────────────────────────────────────

    /**
     * The whole game in one predicate: every cell decided, no run of three
     * anywhere, every line balanced, no two decided rows or columns identical.
     * All of it checked here — a saved board can still be mid-contradiction, and
     * only this predicate says when it is done.
     */
    fun isWin(grid: List<Int>): Boolean {
        if (grid.size != CELLS) return false
        if (grid.any { it == EMPTY }) return false
        if (runViolationCells(grid).isNotEmpty()) return false
        if (overBalancedLines(grid).isNotEmpty()) return false
        return duplicateLine(grid) == null
    }

    /**
     * Rows and columns that are fully decided and free of a run violation — the
     * LINE readout's count, the nonogram LINE readout's direct analogue. Balance
     * follows from a full decide plus the run check's neighbour, so it is not
     * double-counted here; distinctness is a whole-board property no single line
     * can own.
     */
    fun linesSolved(grid: List<Int>): Int =
        linesSolved(grid, runViolationCells(grid))

    /**
     * The solved-line count over a caller-supplied violation set, so a screen that
     * already computed the board's violations never recomputes them.
     */
    fun linesSolved(grid: List<Int>, violations: Set<Int>): Int {
        var count = 0
        for (line in 0 until LINE_COUNT) {
            val cells = lineCells(line)
            if (cells.any { grid[it] == EMPTY }) continue
            if (cells.any { it in violations }) continue
            count++
        }
        return count
    }

    /** The one quantity (method §2): decided cells over all cells. */
    fun completion(grid: List<Int>): Float {
        val decided = grid.count { it != EMPTY }
        return decided.toFloat() / CELLS
    }
}