package com.example.lightapp.games.akari

import kotlin.random.Random

/**
 * The generator for `akari(10)`. Pure JVM, seeded, no Android — every sibling's
 * shape, so the daily contract (D32) is wiring rather than new machinery.
 *
 * **Puzzle validity here is uniqueness, the sudoku bar exactly** (roadmap §3): a
 * generated board must have exactly one solution, and the generator *proves* it
 * rather than assuming it — a bounded solution counter runs over the finished
 * clue set and the board ships only when the count is one. Sudoku's QQWing
 * analogue, hand-rolled.
 *
 * The pipeline, per attempt:
 *
 * 1. **Walls** — a random layout at the difficulty's density. Akari needs no
 *    connectivity guarantee: each white region is lit by its own bulbs, and an
 *    isolated white cell is simply a cell that must host a bulb.
 * 2. **A hidden solution** — a greedy cover: repeatedly bulb the unlit cell whose
 *    run lights the most unlit ground (ties broken randomly). Placing a bulb on
 *    an unlit cell can never clash (unlit means no bulb sees it), so the cover is
 *    valid by construction and terminates — every placement lights its own cell.
 * 3. **Clues** — numbered walls read their count off the *hidden solution*, so the
 *    solution satisfies every clue shipped. A difficulty-dependent subset of
 *    eligible walls is numbered; the rest stay blank.
 * 4. **The uniqueness proof** — count solutions up to two. If two exist, more
 *    walls are numbered (clue `0` walls are legitimate and strong) and the count
 *    reruns, until the count is one or the wall supply is exhausted. An exhausted
 *    attempt is discarded; the next attempt reshuffles.
 *
 * Difficulty is wall density and clue generosity together: fewer walls and fewer
 * clues leave longer sight-lines and weaker constraints, which is what "harder"
 * means in this genre.
 */
object AkariGenerator {

    val DIFFICULTIES = listOf("Easy", "Moderate", "Hard")
    val DEFAULT_DIFFICULTY = "Moderate"

    /** Wall count on the 100-cell board and how readily a wall becomes a clue. */
    private data class Setting(val walls: Int, val clueProbability: Double)

    private val SETTINGS = mapOf(
        "Easy" to Setting(walls = 42, clueProbability = 0.55),
        "Moderate" to Setting(walls = 35, clueProbability = 0.42),
        "Hard" to Setting(walls = 28, clueProbability = 0.30),
    )

    /** Attempts before the generator admits defeat — the caller regenerates or fails. */
    private const val MAX_ATTEMPTS = 60

    /** DFS nodes before [countSolutions] gives up and reports "not proven unique".
     *  The exact count on a loosely-clued board can be exponential; the generator
     *  only ever needs the yes/no answer, and every board it ships proves well
     *  within this budget. */
    private const val NODE_BUDGET = 200_000

    /**
     * A board: [AkariRules.SIZE]² cells of [AkariRules.EMPTY], [AkariRules.WALL] and
     * [AkariRules.CLUE_0]..[AkariRules.CLUE_4]. No bulbs — the start state is dark.
     * Null when every attempt failed, which the view model surfaces as a retry, the
     * connect idiom.
     */
    fun generate(difficulty: String, seed: Long? = null): List<Int>? {
        val setting = SETTINGS[difficulty] ?: SETTINGS.getValue(DEFAULT_DIFFICULTY)
        val random = seed?.let { Random(it) } ?: Random(System.nanoTime())
        repeat(MAX_ATTEMPTS) {
            val board = attempt(setting, random) ?: return@repeat
            return board
        }
        return null
    }

    // ─── one attempt ──────────────────────────────────────────────────────────

    private fun attempt(setting: Setting, random: Random): List<Int>? {
        val walls = layoutWalls(setting.walls, random)
        val solution = cover(walls, random) ?: return null

        // First clue pass: eligible walls (those touching a solution bulb) are
        // numbered with their solution count, at the difficulty's probability.
        val clueFields = BooleanArray(AkariRules.CELLS)
        for (w in 0 until AkariRules.CELLS) {
            if (!walls[w]) continue
            val n = adjacentBulbCount(solution, w)
            if (n > 0 && random.nextDouble() < setting.clueProbability) clueFields[w] = true
        }
        val cells = combine(walls, clueFields, solution)
        if (countSolutions(cells, cap = 2) == 1) return cells

        // Not unique: number more walls until the count collapses to one. Walls
        // touching zero solution bulbs become clue `0` — a legitimate and strong
        // constraint. Out of walls and still ambiguous means the attempt dies.
        val extras = (0 until AkariRules.CELLS)
            .filter { walls[it] && !clueFields[it] }
            .shuffled(random)
        for (w in extras) {
            clueFields[w] = true
            val next = combine(walls, clueFields, solution)
            if (countSolutions(next, cap = 2) == 1) return next
        }
        return null
    }

    /** [walls] with [clueFields] applied, counts read off [solution] — a start-state
     * board, dark and clued: the shipped layout, before a single bulb is placed. */
    private fun combine(walls: BooleanArray, clueFields: BooleanArray, solution: BooleanArray): List<Int> =
        List(AkariRules.CELLS) { i ->
            when {
                clueFields[i] -> AkariRules.CLUE_0 + adjacentBulbCount(solution, i)
                walls[i] -> AkariRules.WALL
                else -> AkariRules.EMPTY
            }
        }

    private fun layoutWalls(count: Int, random: Random): BooleanArray {
        val walls = BooleanArray(AkariRules.CELLS)
        for (w in (0 until AkariRules.CELLS).shuffled(random).take(count)) walls[w] = true
        return walls
    }

    // ─── the hidden solution ──────────────────────────────────────────────────

    /**
     * A valid bulb cover over [walls], or null (never in practice — see below).
     * Greedy max-coverage: bulb the unlit cell lighting the most unlit ground.
     * Placing a bulb on an unlit cell cannot clash (unlit = no bulb in sight), so
     * validity is structural; termination is too, since every placement lights
     * its own cell.
     */
    private fun cover(walls: BooleanArray, random: Random): BooleanArray? {
        val bulbs = BooleanArray(AkariRules.CELLS)
        while (true) {
            val unlit = (0 until AkariRules.CELLS).filter { i ->
                !walls[i] && !bulbs[i] && !isLit(i, walls, bulbs)
            }
            if (unlit.isEmpty()) return bulbs
            // A bulb on an unlit cell lights itself at minimum, so a candidate
            // always exists; null is unreachable and said so for honesty.
            val best = unlit
                .groupBy { coverage(it, walls, unlit) }
                .maxByOrNull { it.key }
                ?.value ?: return null
            bulbs[best.random(random)] = true
        }
    }

    /** Unlit white cells (from [unlit]) that [index]'s bulb would light. */
    private fun coverage(index: Int, walls: BooleanArray, unlit: List<Int>): Int {
        var count = 0
        val r = AkariRules.rowOf(index)
        val c = AkariRules.colOf(index)
        var up = r - 1
        while (up >= 0 && !walls[up * AkariRules.SIZE + c]) {
            if (unlit.contains(up * AkariRules.SIZE + c)) count++
            up--
        }
        var down = r + 1
        while (down < AkariRules.SIZE && !walls[down * AkariRules.SIZE + c]) {
            if (unlit.contains(down * AkariRules.SIZE + c)) count++
            down++
        }
        var left = c - 1
        while (left >= 0 && !walls[r * AkariRules.SIZE + left]) {
            if (unlit.contains(r * AkariRules.SIZE + left)) count++
            left--
        }
        var right = c + 1
        while (right < AkariRules.SIZE && !walls[r * AkariRules.SIZE + right]) {
            if (unlit.contains(r * AkariRules.SIZE + right)) count++
            right++
        }
        return count
    }

    private fun isLit(index: Int, walls: BooleanArray, bulbs: BooleanArray): Boolean {
        val r = AkariRules.rowOf(index)
        val c = AkariRules.colOf(index)
        var up = r
        while (up >= 0 && !walls[up * AkariRules.SIZE + c]) {
            if (bulbs[up * AkariRules.SIZE + c]) return true
            up--
        }
        var down = r + 1
        while (down < AkariRules.SIZE && !walls[down * AkariRules.SIZE + c]) {
            if (bulbs[down * AkariRules.SIZE + c]) return true
            down++
        }
        var left = c - 1
        while (left >= 0 && !walls[r * AkariRules.SIZE + left]) {
            if (bulbs[r * AkariRules.SIZE + left]) return true
            left--
        }
        var right = c + 1
        while (right < AkariRules.SIZE && !walls[r * AkariRules.SIZE + right]) {
            if (bulbs[r * AkariRules.SIZE + right]) return true
            right++
        }
        return false
    }

    private fun adjacentBulbCount(bulbs: BooleanArray, index: Int): Int {
        val r = AkariRules.rowOf(index)
        val c = AkariRules.colOf(index)
        var count = 0
        if (r > 0 && bulbs[(r - 1) * AkariRules.SIZE + c]) count++
        if (r < AkariRules.SIZE - 1 && bulbs[(r + 1) * AkariRules.SIZE + c]) count++
        if (c > 0 && bulbs[r * AkariRules.SIZE + c - 1]) count++
        if (c < AkariRules.SIZE - 1 && bulbs[r * AkariRules.SIZE + c + 1]) count++
        return count
    }

    // ─── the uniqueness proof ─────────────────────────────────────────────────

    /**
     * Counts solutions of [cells] up to [cap]. Depth-first over the white cells in
     * order — at each, branch "no bulb" and, when legal, "bulb" — with three
     * prunings that keep a 10×10 inside milliseconds:
     *
     * - a bulb may not be placed on a cell another bulb already lights (the clash
     *   rule, seen from the other side),
     * - a bulb may not push an adjacent numbered wall past its number,
     * - every decided-but-unlit white cell must still have an undecided cell in
     *   its run that could light it, else the branch is dead.
     *
     * Clue *exactness* (a numbered wall touching too few bulbs) is checked only at
     * the leaves — during the descent, under-count is not yet a contradiction.
     * A node budget bounds the search: when it fires the function returns `cap` —
     * *not proven unique*, exactly what the generator must treat it as — so no
     * input, however open, can stall generation.
     * Exposed because the generator's load-bearing guarantee is asserted, not
     * assumed (roadmap §3): the tests call this directly on generated boards.
     */
    fun countSolutions(cells: List<Int>, cap: Int = 2): Int {
        val white = (0 until AkariRules.CELLS).filter { cells[it] == AkariRules.EMPTY }
        // position of each cell index in the white-cell decision order
        val posOf = IntArray(AkariRules.CELLS) { -1 }
        white.forEachIndexed { p, i -> posOf[i] = p }
        // for each white cell, every white cell that could light it (its two runs)
        val lighters = Array(AkariRules.CELLS) { emptyList<Int>() }
        for (i in white) lighters[i] = runCells(cells, i)

        val bulbs = BooleanArray(AkariRules.CELLS)
        var count = 0
        var nodes = 0

        fun litNow(i: Int) = lighters[i].any { bulbs[it] }

        /** Placed bulbs orthogonally adjacent to [index] — what a clue wall counts. */
        fun adjacentPlaced(index: Int): Int {
            val r = AkariRules.rowOf(index)
            val c = AkariRules.colOf(index)
            var placed = 0
            if (r > 0 && bulbs[(r - 1) * AkariRules.SIZE + c]) placed++
            if (r < AkariRules.SIZE - 1 && bulbs[(r + 1) * AkariRules.SIZE + c]) placed++
            if (c > 0 && bulbs[r * AkariRules.SIZE + c - 1]) placed++
            if (c < AkariRules.SIZE - 1 && bulbs[r * AkariRules.SIZE + c + 1]) placed++
            return placed
        }

        fun clueExceeded(index: Int): Boolean {
            val r = AkariRules.rowOf(index)
            val c = AkariRules.colOf(index)
            // A bulb at [index] lands adjacent to each orthogonal neighbour; if any
            // of those is a numbered clue this placement pushes past its number, it
            // is illegal. Count *placed* bulbs — the raw board is dark, so summing
            // it would always read zero.
            val neighbours = listOf(
                if (r > 0) (r - 1) * AkariRules.SIZE + c else null,
                if (r < AkariRules.SIZE - 1) (r + 1) * AkariRules.SIZE + c else null,
                if (c > 0) r * AkariRules.SIZE + c - 1 else null,
                if (c < AkariRules.SIZE - 1) r * AkariRules.SIZE + c + 1 else null,
            )
            for (nb in neighbours) {
                val n = nb?.let { AkariRules.clueNumber(cells[it]) } ?: continue
                if (adjacentPlaced(nb) + 1 > n) return true
            }
            return false
        }

        fun clueExact(): Boolean {
            for (i in 0 until AkariRules.CELLS) {
                val n = AkariRules.clueNumber(cells[i]) ?: continue
                if (adjacentPlaced(i) != n) return false
            }
            return true
        }

        fun dfs(p: Int) {
            if (++nodes > NODE_BUDGET) {
                count = cap
                return
            }
            if (count >= cap) return
            if (p == white.size) {
                if (white.all { litNow(it) } && clueExact()) count++
                return
            }
            val j = white[p]
            // Branch 1: a bulb here — legal only in the dark and under the clues.
            // Explored *first* deliberately: a bulb instantly lights its whole run,
            // so on open boards this reaches a cover on the first descent (one bulb
            // per row on the diagonal already covers a wall-less 10×10). The cap
            // stop is about finding covers fast — a valid cover is what increments
            // count — and blundering through the no-bulb side of the tree first
            // spends the whole budget before the first cover appears. Branch order
            // does not change the exact total (both subtrees are visited), only the
            // order leaves are found in.
            if (!litNow(j) && !clueExceeded(j)) {
                bulbs[j] = true
                dfs(p + 1)
                bulbs[j] = false
            }
            if (count >= cap) return
            // Branch 2: no bulb here — viable only while every decided-but-unlit
            // cell can still be lit by an undecided cell.
            if (white.take(p + 1).all { cell ->
                    litNow(cell) || lighters[cell].any { posOf[it] > p }
                }
            ) dfs(p + 1)
        }
        dfs(0)
        return count
    }

    /** The white cells sharing [index]'s row run and column run, including itself. */
    private fun runCells(cells: List<Int>, index: Int): List<Int> {
        val out = mutableListOf(index)
        val r = AkariRules.rowOf(index)
        val c = AkariRules.colOf(index)
        var up = r - 1
        while (up >= 0 && !AkariRules.isWall(cells[up * AkariRules.SIZE + c])) {
            if (cells[up * AkariRules.SIZE + c] == AkariRules.EMPTY) out += up * AkariRules.SIZE + c
            up--
        }
        var down = r + 1
        while (down < AkariRules.SIZE && !AkariRules.isWall(cells[down * AkariRules.SIZE + c])) {
            if (cells[down * AkariRules.SIZE + c] == AkariRules.EMPTY) out += down * AkariRules.SIZE + c
            down++
        }
        var left = c - 1
        while (left >= 0 && !AkariRules.isWall(cells[r * AkariRules.SIZE + left])) {
            if (cells[r * AkariRules.SIZE + left] == AkariRules.EMPTY) out += r * AkariRules.SIZE + left
            left--
        }
        var right = c + 1
        while (right < AkariRules.SIZE && !AkariRules.isWall(cells[r * AkariRules.SIZE + right])) {
            if (cells[r * AkariRules.SIZE + right] == AkariRules.EMPTY) out += r * AkariRules.SIZE + right
            right++
        }
        return out
    }
}
