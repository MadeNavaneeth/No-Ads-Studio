package com.example.lightapp.games.connect

/**
 * Rules for `connect(7)` — pure functions, no Android dependency, milliseconds on the JVM.
 *
 * ## The encoding
 *
 * A cell holds one `Int`:
 *
 * - [EMPTY] `0` — untouched ground
 * - `1..[PAIRS]` — an **endpoint**, numbered by its pair (`endpointOf`)
 * - `[PATH_BASE]+1..[PATH_BASE]+[PAIRS]` — a laid **path segment** of that pair (`pathOf`)
 *
 * Endpoints carry numerals, never colours (roadmap §4, the whole monochrome adaptation
 * of Numberlink). A laid segment carries its pair's number too — in storage, not on
 * screen — which is what lets restore validate a half-finished board without the live
 * walk that laid it.
 *
 * ## The one mutation
 *
 * All play goes through [start] and [step]. A walk is a list of cell indexes beginning
 * at an endpoint; [step] extends, retracts, or trims it, and every branch keeps the walk
 * a connected orthogonal route. A trimmed or retracted cell returns to [EMPTY] — cells
 * holding an endpoint are never rewritten, so an endpoint can be walked over in the path
 * list while the board still says what it is.
 *
 * There is no wrong move in this game: every step is retractable, so there is no
 * mistake to count and no red anywhere (method §6). The run ends one way — solved.
 */
object ConnectRules {

    const val SIZE = 7
    const val CELLS = SIZE * SIZE

    /** The highest pair id the encoding admits; the generator uses 4–6 of them. */
    const val PAIRS = 8

    const val EMPTY = 0

    /** Path segment values sit above every endpoint id. */
    const val PATH_BASE = PAIRS

    fun endpointOf(pair: Int): Int = pair
    fun pathOf(pair: Int): Int = PATH_BASE + pair

    fun isEndpoint(value: Int): Boolean = value in 1..PAIRS
    fun isPath(value: Int): Boolean = value in PATH_BASE + 1..PATH_BASE + PAIRS

    /** The pair an endpoint cell belongs to, or null when the cell is not an endpoint. */
    fun endpointPair(value: Int): Int? = if (isEndpoint(value)) value else null

    /** The pair a path cell belongs to, or null when the cell is not a laid segment. */
    fun pathPair(value: Int): Int? = if (isPath(value)) value - PATH_BASE else null

    /** Orthogonal neighbours — the connection moves, never diagonal. */
    fun neighbours(index: Int): List<Int> {
        val r = index / SIZE
        val c = index % SIZE
        val out = ArrayList<Int>(4)
        if (r > 0) out += index - SIZE
        if (r < SIZE - 1) out += index + SIZE
        if (c > 0) out += index - 1
        if (c < SIZE - 1) out += index + 1
        return out
    }

    fun rowOf(index: Int): Int = index / SIZE
    fun colOf(index: Int): Int = index % SIZE

    // ─── the walk ─────────────────────────────────────────────────────────────

    /**
     * Begins a walk on an endpoint. Any other cell is not a handle — null, and the
     * screen treats the tap as a no-op rather than an error.
     */
    fun start(cells: List<Int>, index: Int): List<Int>? {
        if (isWin(cells)) return null
        return if (endpointPair(cells[index]) != null) listOf(index) else null
    }

    /** The pair whose walk is live, or null when no walk is under way. */
    fun activePair(cells: List<Int>, path: List<Int>): Int? =
        path.firstOrNull()?.let { endpointPair(cells[it]) }

    /**
     * One drag step onto [target], which must be orthogonally adjacent to the walk's
     * head. The branches, in priority order:
     *
     * 1. the walk's own partner — the connection completes (the endpoint value stays;
     *    only the path list records the arrival)
     * 2. the cell the walk came from — one step back ([retract])
     * 3. any earlier cell of this walk — the fork trims back to it ([trim])
     * 4. an own path cell not in the live walk — adopted (this is how a restored or
     *    revisited pair picks its laid segments back up)
     * 5. empty ground — laid
     *
     * Anything else — another pair's endpoint or segment — is null: not an error, just
     * not this walk's ground. Null also on a finished board, which is frozen.
     */
    fun step(cells: List<Int>, path: List<Int>, target: Int): Step? {
        if (isWin(cells)) return null
        if (path.isEmpty()) return null
        val pair = endpointPair(cells[path.first()]) ?: return null
        if (target !in neighbours(path.last())) return null
        val value = cells[target]

        return when {
            value == endpointOf(pair) && target != path.first() ->
                Step(cells, path + target, changed = false)

            path.size >= 2 && target == path[path.size - 2] ->
                Step(retract(cells, path.last(), pair), path.dropLast(1), changed = true)

            // The trim check precedes adoption: an own path cell already in the live
            // walk is a fork to unwind, not new ground to adopt.
            else -> {
                val earlier = path.indexOf(target)
                when {
                    earlier >= 0 ->
                        Step(
                            trim(cells, path.drop(earlier + 1), pair),
                            path.subList(0, earlier + 1).toList(),
                            changed = true,
                        )

                    value == pathOf(pair) ->
                        Step(cells, path + target, changed = false)

                    value == EMPTY ->
                        Step(lay(cells, target, pair), path + target, changed = true)

                    else -> null
                }
            }
        }
    }

    /** One reversible change — decision D17, in memory only. */
    data class Step(val cells: List<Int>, val path: List<Int>, val changed: Boolean)

    private fun lay(cells: List<Int>, index: Int, pair: Int): List<Int> =
        cells.toMutableList().also { it[index] = pathOf(pair) }

    private fun clear(cells: List<Int>, index: Int): List<Int> =
        cells.toMutableList().also { it[index] = EMPTY }

    /** The head cell returns to empty ground — unless it was an endpoint, which never rewrites. */
    private fun retract(cells: List<Int>, index: Int, pair: Int): List<Int> =
        if (cells[index] == pathOf(pair)) clear(cells, index) else cells

    /** Every cell in [dropped] returns to empty ground when it held a segment of [pair]. */
    private fun trim(cells: List<Int>, dropped: List<Int>, pair: Int): List<Int> {
        var out = cells
        for (index in dropped) {
            if (out[index] == pathOf(pair)) out = clear(out, index)
        }
        return out
    }

    // ─── board-level reading ──────────────────────────────────────────────────

    /** Every pair id present as an endpoint on the board. */
    fun endpointPairs(cells: List<Int>): Set<Int> =
        cells.mapNotNull(::endpointPair).toSet()

    /** How many cells a pair has laid. Zero means the pair has not been started. */
    fun laidCount(cells: List<Int>, pair: Int): Int =
        cells.count { it == pathOf(pair) }

    /**
     * The route a pair's laid segments form, read from the board alone: a walk from the
     * pair's endpoint through its segments that consumes every one of them. Null when
     * the segments are disconnected, orphaned, or the pair has not started — which is
     * what makes this the restore validator's honest check, not a formality.
     */
    fun routeFor(cells: List<Int>, pair: Int): List<Int>? {
        val from = cells.indexOf(endpointOf(pair))
        if (from < 0) return null
        val to = cells.lastIndexOf(endpointOf(pair))
        if (to == from) return null

        val total = laidCount(cells, pair)
        // total == 0 is legitimate: adjacent endpoints are already connected, with
        // nothing to walk. extend() handles it — consumed(0) == total(0) reduces to
        // "the partner is adjacent". A pair that has not started (endpoints far
        // apart, nothing laid) fails there instead.

        // The walk backtracks, because a laid route can **fold**: two cells of the
        // same route can sit side by side on the grid without being consecutive
        // along it, so a greedy walk from the endpoint can step into the fold and
        // dead-end with segments still unconsumed. Branching is bounded by the
        // route's own width and the board is 49 cells, so this stays cheap.
        val walked = ArrayList<Int>(total + 1)
        val onWalk = BooleanArray(CELLS)
        walked += from
        onWalk[from] = true

        fun extend(head: Int, consumed: Int): Boolean {
            // Every segment consumed and standing beside the partner: a real route.
            if (consumed == total) return to in neighbours(head)
            for (next in neighbours(head)) {
                if (!onWalk[next] && cells[next] == pathOf(pair)) {
                    onWalk[next] = true
                    walked += next
                    if (extend(next, consumed + 1)) return true
                    walked.removeAt(walked.lastIndex)
                    onWalk[next] = false
                }
            }
            return false
        }

        return if (extend(from, 0)) walked.toList() else null
    }

    /** Pairs whose laid route is complete — the LINE readout, derived once per position. */
    fun solvedPairs(cells: List<Int>): Int =
        endpointPairs(cells).count { routeFor(cells, it) != null }

    /**
     * The win: every cell used, and every pair's segments forming a real route between
     * its endpoints. Full cover alone is not enough — a corrupt board could fill all
     * 49 cells with orphaned segments, and this is what refuses to call that solved.
     */
    fun isWin(cells: List<Int>): Boolean {
        if (cells.any { it == EMPTY }) return false
        val pairs = endpointPairs(cells)
        if (pairs.isEmpty()) return false
        return pairs.all { routeFor(cells, it) != null }
    }

    /** The one quantity (method §2): used cells over all cells. */
    fun completion(cells: List<Int>): Float =
        cells.count { it != EMPTY }.toFloat() / CELLS
}
