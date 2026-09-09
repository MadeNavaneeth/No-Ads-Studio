package com.example.lightapp.games.binairo

import kotlin.random.Random

/**
 * The generator for `binairo(10)`. Pure JVM, seeded, no Android — every sibling's
 * shape, so the daily contract (D32) is wiring rather than new machinery.
 *
 * **Puzzle validity here is deduction-solvability, which *implies* uniqueness —
 * the sudoku bar (roadmap §3), reached the honest way for a constraint puzzle.**
 * Every deduction the solver makes is forced by a rule any solution must satisfy,
 * so a board the solver empties by deduction alone has exactly one solution. The
 * generator *proves* this per board rather than assuming it — QQWing's analogue,
 * hand-rolled, like akari's solution counter.
 *
 * The pipeline, per attempt:
 *
 * 1. **A hidden solution** — randomized backtracking cell by cell: no three
 *     consecutive equal in the row or column so far, at most [BinairoRules.HALF]
 *     of each value per line, and no row equal to a completed row. Column balance
 *     is automatic (ten balanced rows balance every column); column distinctness
 *     is checked when the last row completes, and a failed attempt is retried.
 * 2. **Clue removal** — every cell starts as a given; cells are offered for
 *     removal in random order, and a removal is kept only while the board stays
 *     deduction-solvable. Difficulty is the removal budget: fewer removals leave
 *     more givens and a shorter, gentler board.
 *
 * Difficulty is the removal budget, then — the only honest lever this genre has,
 * since a binairo is either solvable by logic or mis-set.
 */
object BinairoGenerator {

    val DIFFICULTIES = listOf("Easy", "Moderate", "Hard")
    val DEFAULT_DIFFICULTY = "Moderate"

    /** How many givens each difficulty may strip from the full 100-cell solution. */
    private val REMOVAL_BUDGET = mapOf(
        "Easy" to 40,
        "Moderate" to 60,
        "Hard" to BinairoRules.CELLS, // greedy to the end — the fewest givens this order finds
    )

    /** Attempts before the generator admits defeat — the caller regenerates or fails. */
    private const val MAX_ATTEMPTS = 60

    /** Backtracking retries for the hidden solution before the whole attempt dies. */
    private const val MAX_SOLUTION_TRIES = 200

    /**
     * A start-state board: [BinairoRules.SIZE]² cells of [BinairoRules.EMPTY] and
     * the given ONEs and ZEROs. Null when every attempt failed, which the view
     * model surfaces as a retry, the connect idiom.
     */
    fun generate(difficulty: String, seed: Long? = null): List<Int>? {
        val budget = REMOVAL_BUDGET[difficulty] ?: REMOVAL_BUDGET.getValue(DEFAULT_DIFFICULTY)
        val random = seed?.let { Random(it) } ?: Random(System.nanoTime())
        repeat(MAX_ATTEMPTS) {
            val board = attempt(budget, random) ?: return@repeat
            return board
        }
        return null
    }

    private fun attempt(budget: Int, random: Random): List<Int>? {
        val solution = fullGrid(random) ?: return null
        return stripClues(solution, budget, random)
    }

    // ─── the hidden solution ──────────────────────────────────────────────────

    /**
     * A complete valid grid, or null if [MAX_SOLUTION_TRIES] randomized tries all
     * hit column-duplicate dead ends (never observed at 10×10 — said for honesty).
     */
    private fun fullGrid(random: Random): List<Int>? {
        repeat(MAX_SOLUTION_TRIES) {
            val grid = solutionTry(random)
            if (grid != null && BinairoRules.duplicateLine(grid) == null) return grid
        }
        return null
    }

    /** One backtracking try; the grid is returned complete or null on a dead search. */
    private fun solutionTry(random: Random): List<Int>? {
        val grid = IntArray(BinairoRules.CELLS)
        val rowsSeen = mutableListOf<String>()

        fun rowKey(r: Int): String {
            val sb = StringBuilder(BinairoRules.SIZE)
            for (c in 0 until BinairoRules.SIZE) sb.append(grid[r * BinairoRules.SIZE + c] - 1)
            return sb.toString()
        }

        // Legality of placing [v] at [index] given the cells already decided.
        fun legal(index: Int, v: Int): Boolean {
            val r = BinairoRules.rowOf(index)
            val c = BinairoRules.colOf(index)
            // No three consecutive in the row: check the two pairs ending here…
            if (c >= 2 &&
                grid[index - 1] == v && grid[index - 2] == v
            ) return false
            if (c >= 1 && c <= BinairoRules.SIZE - 2 &&
                grid[index - 1] == v && grid[index + 1] == v
            ) return false
            // …and in the column, walking up from the cell above.
            if (r >= 2) {
                val a = grid[index - BinairoRules.SIZE]
                val b = grid[index - 2 * BinairoRules.SIZE]
                if (a == v && b == v) return false
            }
            // Balance: at most HALF of each value already placed in this line.
            var rowOnes = 0
            var colOnes = 0
            for (k in 0 until BinairoRules.SIZE) {
                if (grid[r * BinairoRules.SIZE + k] == BinairoRules.ONE) rowOnes++
                if (grid[k * BinairoRules.SIZE + c] == BinairoRules.ONE) colOnes++
            }
            val extra = if (v == BinairoRules.ONE) 1 else 0
            return rowOnes + extra <= BinairoRules.HALF && colOnes + extra <= BinairoRules.HALF
        }

        fun dfs(index: Int): Boolean {
            if (index == BinairoRules.CELLS) return true
            val r = BinairoRules.rowOf(index)
            // A completed row that duplicates an earlier one kills the whole branch.
            if (index % BinairoRules.SIZE == 0 && r > 0) {
                val key = rowKey(r - 1)
                if (key in rowsSeen) return false
                rowsSeen.add(key)
            }
            val first = if (random.nextBoolean()) BinairoRules.ONE else BinairoRules.ZERO
            for (v in listOf(first, if (first == BinairoRules.ONE) BinairoRules.ZERO else BinairoRules.ONE)) {
                if (legal(index, v)) {
                    grid[index] = v
                    if (dfs(index + 1)) return true
                    grid[index] = BinairoRules.EMPTY
                }
            }
            if (index % BinairoRules.SIZE == 0 && r > 0) rowsSeen.removeAt(rowsSeen.size - 1)
            return false
        }

        return if (dfs(0)) grid.toList() else null
    }

    // ─── clue removal ─────────────────────────────────────────────────────────

    /**
     * Starts from the full solution as the clue set and offers each cell for
     * removal in random order, keeping a removal only while the board stays
     * deduction-solvable, up to [budget] removals. Deterministic given [random].
     */
    private fun stripClues(solution: List<Int>, budget: Int, random: Random): List<Int> {
        val puzzle = solution.toMutableList()
        var removed = 0
        for (index in (0 until BinairoRules.CELLS).shuffled(random)) {
            if (removed >= budget) break
            val kept = puzzle[index]
            puzzle[index] = BinairoRules.EMPTY
            if (Deduction.solve(puzzle) == null) {
                puzzle[index] = kept
            } else {
                removed++
            }
        }
        return puzzle.toList()
    }

    // ─── the deduction solver ─────────────────────────────────────────────────

    /**
     * Solves [puzzle] by forced moves only, or returns null when it stalls.
     *
     * Three rules, each sound — any valid completion already satisfies them, so
     * every forced value is the hidden solution's value and a completed pass is
     * proof of a unique solution:
     *
     * 1. **Runs** — a window of three consecutive cells with two equal known
     *    values forces the third to the opposite state; the pattern `X ? X`
     *    forces the middle the same way (placing X there would make three).
     * 2. **Balance** — a line already holding [BinairoRules.HALF] of one state
     *    forces every remaining cell in it to the other.
     * 3. **Distinctness** — two same-kind lines agreeing on every known cell,
     *    where exactly one line holds a single known value the other lacks,
     *    force that value's opposite (matching it would duplicate the line).
     *
     * Exposed because the generator's load-bearing guarantee is asserted, not
     * assumed (roadmap §3): the tests call this directly on generated boards.
     */
    object Deduction {

        fun solve(puzzle: List<Int>): List<Int>? {
            val grid = puzzle.toIntArray()
            if (grid.size != BinairoRules.CELLS) return null

            var changed = true
            while (changed) {
                changed = false
                changed = changed or applyRuns(grid)
                changed = changed or applyBalance(grid)
                changed = changed or applyDistinctness(grid)
            }

            if (grid.any { it == BinairoRules.EMPTY }) return null
            return grid.toList()
        }

        private fun applyRuns(grid: IntArray): Boolean {
            var changed = false
            for (line in 0 until BinairoRules.LINE_COUNT) {
                val cells = BinairoRules.lineCells(line)
                // Window of three: two equal knowns force the third.
                for (start in 0..BinairoRules.SIZE - 3) {
                    val a = cells[start]
                    val b = cells[start + 1]
                    val c = cells[start + 2]
                    val va = grid[a]
                    val vb = grid[b]
                    val vc = grid[c]
                    when {
                        va != BinairoRules.EMPTY && va == vb && vc == BinairoRules.EMPTY ->
                            changed = force(grid, c, va) || changed
                        va != BinairoRules.EMPTY && va == vc && vb == BinairoRules.EMPTY ->
                            changed = force(grid, b, va) || changed
                        vb != BinairoRules.EMPTY && vb == vc && va == BinairoRules.EMPTY ->
                            changed = force(grid, a, vb) || changed
                    }
                }
            }
            return changed
        }

        private fun applyBalance(grid: IntArray): Boolean {
            var changed = false
            for (line in 0 until BinairoRules.LINE_COUNT) {
                val cells = BinairoRules.lineCells(line)
                var ones = 0
                var zeros = 0
                for (i in cells) {
                    if (grid[i] == BinairoRules.ONE) ones++
                    if (grid[i] == BinairoRules.ZERO) zeros++
                }
                if (ones == BinairoRules.HALF && zeros < BinairoRules.HALF) {
                    for (i in cells) {
                        if (grid[i] == BinairoRules.EMPTY) changed = force(grid, i, BinairoRules.ZERO) || changed
                    }
                }
                if (zeros == BinairoRules.HALF && ones < BinairoRules.HALF) {
                    for (i in cells) {
                        if (grid[i] == BinairoRules.EMPTY) changed = force(grid, i, BinairoRules.ONE) || changed
                    }
                }
            }
            return changed
        }

        private fun applyDistinctness(grid: IntArray): Boolean {
            var changed = false
            for (a in 0 until BinairoRules.LINE_COUNT) {
                for (b in a + 1 until BinairoRules.LINE_COUNT) {
                    if ((a < BinairoRules.SIZE) != (b < BinairoRules.SIZE)) continue
                    val ca = BinairoRules.lineCells(a)
                    val cb = BinairoRules.lineCells(b)
                    // Walk the pair: a known disagreement means the lines already
                    // differ and nothing is forced. Otherwise count each line's
                    // unknowns while the jointly-known cells agree.
                    var differs = false
                    var aEmpty = -1
                    var aEmpties = 0
                    var bEmpty = -1
                    var bEmpties = 0
                    for (k in 0 until BinairoRules.SIZE) {
                        val va = grid[ca[k]]
                        val vb = grid[cb[k]]
                        if (va == BinairoRules.EMPTY) { aEmpties++; aEmpty = k }
                        if (vb == BinairoRules.EMPTY) { bEmpties++; bEmpty = k }
                        if (va != BinairoRules.EMPTY && vb != BinairoRules.EMPTY && va != vb) {
                            differs = true
                            break
                        }
                    }
                    if (differs) continue
                    // One line fully decided, the twin exactly one cell short and
                    // agreeing everywhere else: filling the gap with the twin's
                    // value would duplicate the line, so the opposite is forced.
                    if (aEmpties == 0 && bEmpties == 1) {
                        changed = force(grid, cb[bEmpty], opposite(grid[ca[bEmpty]])) || changed
                    } else if (bEmpties == 0 && aEmpties == 1) {
                        changed = force(grid, ca[aEmpty], opposite(grid[cb[aEmpty]])) || changed
                    }
                }
            }
            return changed
        }

        /** Writes [v] into [index]; a forced contradiction with what is there is a stall. */
        private fun force(grid: IntArray, index: Int, v: Int): Boolean {
            val current = grid[index]
            if (current == v) return false
            if (current != BinairoRules.EMPTY) return false
            grid[index] = v
            return true
        }

        private fun opposite(v: Int) = if (v == BinairoRules.ONE) BinairoRules.ZERO else BinairoRules.ONE
    }
}