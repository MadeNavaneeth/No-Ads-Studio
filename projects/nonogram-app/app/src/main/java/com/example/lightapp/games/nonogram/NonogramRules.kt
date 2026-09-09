package com.example.lightapp.games.nonogram

/**
 * Rules for `nonogram(10)`. Pure functions — no Android, no Compose — so every
 * one of these is unit-testable on the JVM.
 *
 * Cells are tri-state (parity N1): [UNKNOWN], [FILLED], [MARKED]. The solution
 * is a bitmap of 0/1. Unlike `sudoku(9)` the game holds its solution in memory:
 * the win condition *is* an exact match (parity §2), so there is no branch that
 * could avoid knowing it — and a wrong fill is still never announced (method
 * §6), only counted toward the optional lives limit.
 */
object NonogramRules {

    const val SIZE = 10
    const val CELLS = SIZE * SIZE

    const val UNKNOWN = 0
    const val FILLED = 1
    const val MARKED = 2

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE

    /** Run lengths of fills in a solution line, e.g. [1,0,0,1,1,0,1] → [1, 2, 1]. */
    fun cluesForLine(line: List<Int>): List<Int> {
        val runs = mutableListOf<Int>()
        var run = 0
        for (cell in line) {
            if (cell == 1) run++
            else if (run > 0) {
                runs.add(run)
                run = 0
            }
        }
        if (run > 0) runs.add(run)
        return runs
    }

    /** Every way [runs] fits in a line of [length], as coverage masks. */
    fun placements(length: Int, runs: List<Int>): List<BooleanArray> {
        if (runs.isEmpty()) return listOf(BooleanArray(length))
        val out = mutableListOf<BooleanArray>()
        fun place(runIdx: Int, pos: Int, mask: BooleanArray) {
            if (runIdx == runs.size) {
                out.add(mask.copyOf())
                return
            }
            val remainingMin = runs.drop(runIdx).sum() + (runs.size - runIdx - 1)
            var start = pos
            while (start + remainingMin <= length) {
                val next = mask.copyOf()
                for (i in 0 until runs[runIdx]) next[start + i] = true
                place(runIdx + 1, start + runs[runIdx] + 1, next)
                start++
            }
        }
        place(0, 0, BooleanArray(length))
        return out
    }

    /**
     * True when [solution] is solvable by propagation alone: every line's
     * clue-consistent placements are intersected, forced cells are filled, and
     * the loop repeats to a fixpoint.
     *
     * Sound: knowledge only ever moves toward the solution, so reaching a full
     * grid proves deduction-solvability *and* uniqueness in one pass. This is
     * the admission-criterion solver run (roadmap §3, puzzle validity).
     */
    fun isDeducible(solution: List<Int>): Boolean {
        require(solution.size == CELLS)
        val clues = deriveClues(solution)
        // 1 = known filled, -1 = known empty, 0 = unknown.
        val known = IntArray(CELLS)
        val rowPlace = List(SIZE) { r -> placements(SIZE, clues.rows[r]) }
        val colPlace = List(SIZE) { c -> placements(SIZE, clues.cols[c]) }
        while (true) {
            var progress = false
            for (r in 0 until SIZE) {
                val idx = IntArray(SIZE) { c -> r * SIZE + c }
                progress = propagate(idx, rowPlace[r], known) ?: return false
            }
            for (c in 0 until SIZE) {
                val idx = IntArray(SIZE) { r -> r * SIZE + c }
                progress = propagate(idx, colPlace[c], known) ?: return false
            }
            if (!progress) break
        }
        return known.indices.all { (known[it] == 1) == (solution[it] == 1) }
    }

    /** One line's overlap step. Null when no placement fits the knowledge so far. */
    private fun propagate(idx: IntArray, options: List<BooleanArray>, known: IntArray): Boolean? {
        val fitting = options.filter { opt ->
            idx.indices.all { k ->
                val cell = idx[k]
                (known[cell] != 1 || opt[k]) && (known[cell] != -1 || !opt[k])
            }
        }
        if (fitting.isEmpty()) return null
        var progress = false
        for (k in idx.indices) {
            val cell = idx[k]
            if (known[cell] != 0) continue
            when {
                fitting.all { it[k] } -> {
                    known[cell] = 1
                    progress = true
                }
                fitting.none { it[k] } -> {
                    known[cell] = -1
                    progress = true
                }
            }
        }
        return progress
    }

    /** Clues for every row and column, derived from the solution. No bundled data. */
    fun deriveClues(solution: List<Int>): NonogramClues {
        require(solution.size == CELLS)
        val rows = List(SIZE) { r -> cluesForLine(List(SIZE) { c -> solution[r * SIZE + c] }) }
        val cols = List(SIZE) { c -> cluesForLine(List(SIZE) { r -> solution[r * SIZE + c] }) }
        return NonogramClues(rows, cols)
    }

    /** Tap fills: unknown → filled; anything decided clears back to unknown. Always a change. */
    fun tap(cells: List<Int>, index: Int): List<Int> = cells.toMutableList().also {
        it[index] = if (it[index] == UNKNOWN) FILLED else UNKNOWN
    }

    /**
     * Long-press marks: unknown ↔ marked. A filled cell rejects the mark (null) —
     * unfill it first. The caller records no history on null.
     */
    fun toggleMark(cells: List<Int>, index: Int): List<Int>? {
        if (cells[index] == FILLED) return null
        return cells.toMutableList().also {
            it[index] = if (it[index] == MARKED) UNKNOWN else MARKED
        }
    }

    /** Drag paints [value]; null when the cell already holds it (no-op runs record nothing). */
    fun paintCell(cells: List<Int>, index: Int, value: Int): List<Int>? {
        if (cells[index] == value) return null
        // A paint run never overwrites a decided cell of the other kind: a fill run
        // clears unknowns only, a mark run marks unknowns only. Decided cells need
        // an explicit tap, so a fast drag cannot destroy reasoning.
        if (cells[index] != UNKNOWN) return null
        return cells.toMutableList().also { it[index] = value }
    }

    /** A line counts as solved only when its fills match the solution exactly. */
    fun lineSolved(cells: List<Int>, solution: List<Int>, line: Int, isRow: Boolean): Boolean {
        for (k in 0 until SIZE) {
            val i = if (isRow) line * SIZE + k else k * SIZE + line
            if ((cells[i] == FILLED) != (solution[i] == 1)) return false
        }
        return true
    }

    /** The LINE readout: solved rows + solved columns, of 20. */
    fun solvedLineCount(cells: List<Int>, solution: List<Int>): Int {
        var count = 0
        for (line in 0 until SIZE) {
            if (lineSolved(cells, solution, line, true)) count++
            if (lineSolved(cells, solution, line, false)) count++
        }
        return count
    }

    /** Win is an exact match — and the empty board never counts (guard the 0/0 case). */
    fun isWin(cells: List<Int>, solution: List<Int>): Boolean {
        if (solution.none { it == 1 }) return false
        return cells.indices.all { (cells[it] == FILLED) == (solution[it] == 1) }
    }

    /**
     * Progress, 0..1 — the quantity from `game-design-method.md` §2. Correct
     * fills over solution fills, so a fresh board reads 0% and a solved one 100%.
     */
    fun completion(cells: List<Int>, solution: List<Int>): Float {
        val askedFor = solution.count { it == 1 }
        if (askedFor == 0) return 0f
        val correct = cells.indices.count { cells[it] == FILLED && solution[it] == 1 }
        return correct.toFloat() / askedFor
    }
}

/** Row and column clues, derived from the solution at generation time. */
data class NonogramClues(
    val rows: List<List<Int>>,
    val cols: List<List<Int>>,
)
