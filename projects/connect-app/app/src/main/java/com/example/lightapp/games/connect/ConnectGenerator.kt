package com.example.lightapp.games.connect

import kotlin.random.Random

/**
 * Puzzle layouts for `connect(7)`.
 *
 * ## Generation by construction, not by repair
 *
 * The wrong way to make a Numberlink is to sprinkle endpoint pairs at random and then
 * write a repair pass for the boards that come out disconnected. The right way is what
 * the genre's solvers do in reverse: **grow a Hamiltonian path, then cut it into legs.**
 * One path visits every cell of the 7×7, so a cut into pair legs *automatically* yields
 * a board with
 *
 * - a full solution — the path itself, covering all 49 cells (admission gate: valid),
 * - every cell used in the win state — a straight consequence of Hamiltonicity,
 * - disjoint routes — one path cannot cross itself,
 * - and no decoration: the board is exactly the answer, nothing else on it.
 *
 * ## Why the Hamiltonian path is *constructed*, not searched
 *
 * The first draft searched for a path with warnsdorff-guided backtracking. On a grid
 * that heuristic is nearly useless — every interior cell has degree four, so the
 * "fewest onward moves" ordering is uninformative and the search degenerates to
 * random DFS, which thrashes against an exponential space. Measured, not assumed:
 * zero successful starts out of 49 within budget.
 *
 * So the path is built in O(n) with no search at all. Partition the grid into
 * horizontal strips (or vertical ones), then walk each strip boustrophedon —
 * left-to-right, drop, right-to-left. Any such walk visits every cell exactly once:
 * a Hamiltonian path by construction. Random strip height, random orientation, and
 * optional reversal give hundreds of distinct shapes; every one is guaranteed to
 * terminate and to cover the board.
 *
 * ## Why there is no uniqueness gate
 *
 * The obvious next step — reject boards some *other* packing could also fill — was
 * tried and retired, and the reasoning is worth keeping. The win check
 * ([ConnectRules.isWin]) demands full cover and pairwise-disjoint routes; it does not
 * demand the *intended* packing. So if a second filling exists, a player who finds it
 * has solved the puzzle every bit as honestly — the check fires, the run completes.
 * Uniqueness is therefore a *curatorial* quality, not a correctness one, and enforcing
 * it at generation time costs an exhaustive search over packings that can only
 * terminate by exhausting a pair's route space — exponential, on the generation
 * budget (roadmap §3). A cheap uniqueness *filter* can return later if player testing
 * ever shows ambiguous boards feeling samey; correctness never needed it.
 *
 * Deterministic per seed: the same seed builds the same path and accepts the same
 * board, which is what lets a saved puzzle re-derive itself after process death.
 */
object ConnectGenerator {

    const val DEFAULT_DIFFICULTY = "Moderate"
    val DIFFICULTIES = listOf("Simple", "Moderate", "Hard")

    /** Pair counts per level — roadmap §4 fixes the board at 7×7; density is the dial. */
    private val PAIR_COUNTS = mapOf(
        "Simple" to 4,
        "Moderate" to 6,
        "Hard" to 8,
    )

    fun pairCountFor(difficulty: String): Int =
        PAIR_COUNTS[difficulty] ?: PAIR_COUNTS.getValue(DEFAULT_DIFFICULTY)

    /** Candidate budget. Every strip walk is valid, so acceptance is near-certain. */
    private const val MAX_ATTEMPTS = 30

    /**
     * A solved board, or null when none landed in budget.
     *
     * The returned list holds one value per cell — endpoints as [ConnectRules.endpointOf],
     * solution interiors still [ConnectRules.EMPTY]. It is the puzzle's answer sheet:
     * [ConnectState.fresh] keeps only the endpoints, so the shipped position never
     * carries the solution the player is meant to find.
     */
    fun generate(difficulty: String, seed: Long? = null): List<Int>? {
        val pairs = pairCountFor(difficulty)
        if (pairs !in 2..ConnectRules.PAIRS) return null

        val random = if (seed == null) Random.Default else Random(seed)
        repeat(MAX_ATTEMPTS) {
            val path = stripWalk(random)
            val board = cutAndVerify(path, pairs, random) ?: return@repeat
            return board
        }
        return null
    }

    /**
     * A Hamiltonian path over the grid, built in linear time: partition the rows (or
     * columns) into random-height strips and walk each strip boustrophedon. Every
     * such walk covers all cells exactly once, so there is nothing to search and
     * nothing to backtrack.
     *
     * The one subtlety is **continuity between strips**: each strip's first cell must
     * sit beside the previous strip's last cell. That ending cell depends on the
     * strip's height — an odd-height strip ends on its starting side flipped, an
     * even-height strip ends where it started — so the next strip's direction is
     * derived from the running direction and the height just walked, not from a
     * strip index. (Getting this wrong yields disconnected walks that still look
     * plausible cell-by-cell — it did.)
     */
    private fun stripWalk(random: Random): List<Int> {
        val n = ConnectRules.SIZE
        val vertical = random.nextBoolean()
        val reversed = random.nextBoolean()

        // Random strip heights: 1–2 lines each until the board is partitioned.
        val lines = (0 until n).toMutableList()
        val strips = ArrayList<List<Int>>()
        while (lines.isNotEmpty()) {
            val height = 1 + random.nextInt(2.coerceAtMost(lines.size))
            strips += ArrayList(lines.take(height))
            lines.subList(0, height).clear()
        }

        val path = ArrayList<Int>(ConnectRules.CELLS)
        var forward = random.nextBoolean()
        for (strip in strips) {
            var rowForward = forward
            for (line in strip) {
                val cols = if (rowForward) (0 until n) else (n - 1 downTo 0)
                for (c in cols) {
                    path += if (vertical) c * n + line else line * n + c
                }
                rowForward = !rowForward
            }
            // An odd-height strip ends on the opposite side from where it began;
            // an even-height strip ends where it began. The next strip must start
            // exactly there.
            if (strip.size % 2 == 1) forward = !forward
        }
        // Reversing a Hamiltonian path is a Hamiltonian path.
        return if (reversed) path.asReversed() else path
    }

    /**
     * Cuts a Hamiltonian path into [legs] legs (each at least two cells), numbers the
     * cut ends as endpoint pairs — leg *n*'s first and last cell become pair *n* — and
     * verifies the cut against its own solved form: the path with every leg's interior
     * laid as that leg's segments. A cut of a Hamiltonian path solves by construction,
     * so this is a pinned invariant, not a filter — if it ever fails, the cutter is
     * broken and the candidate is discarded. A random cut that does not fit (a leg
     * shorter than two cells) returns null and the caller retries.
     */
    private fun cutAndVerify(path: List<Int>, legs: Int, random: Random): List<Int>? {
        val cuts = sortedSetOf<Int>()
        while (cuts.size < legs - 1) {
            cuts += random.nextInt(1, path.size - 1)
        }
        val bounds = IntArray(legs + 1)
        bounds[0] = 0
        bounds[legs] = path.size
        cuts.forEachIndexed { i, cut -> bounds[i + 1] = cut }
        for (i in 0 until legs) {
            if (bounds[i + 1] - bounds[i] < 2) return null
        }

        var puzzle = List(ConnectRules.CELLS) { ConnectRules.EMPTY }
        var solved = List(ConnectRules.CELLS) { ConnectRules.EMPTY }
        for (leg in 0 until legs) {
            val pair = leg + 1
            puzzle = puzzle.toMutableList().also {
                it[path[bounds[leg]]] = ConnectRules.endpointOf(pair)
                it[path[bounds[leg + 1] - 1]] = ConnectRules.endpointOf(pair)
            }
            for (step in bounds[leg] until bounds[leg + 1]) {
                solved = solved.toMutableList().also {
                    it[path[step]] = ConnectRules.pathOf(pair)
                }
            }
        }
        // Endpoints ride on top of the solved form.
        solved = solved.toMutableList().also { out ->
            for (i in 0 until ConnectRules.CELLS) {
                if (puzzle[i] != ConnectRules.EMPTY) out[i] = puzzle[i]
            }
        }
        return if (ConnectRules.isWin(solved)) puzzle else null
    }
}
