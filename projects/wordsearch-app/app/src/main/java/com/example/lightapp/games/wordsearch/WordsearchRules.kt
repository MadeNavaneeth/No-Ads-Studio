package com.example.lightapp.games.wordsearch

/**
 * Rules for `wordsearch(12)` — pure functions, no Android dependency, milliseconds on
 * the JVM.
 *
 * ## The encoding
 *
 * A cell holds one `Int`:
 *
 * - [HIDDEN] `0` — an unfound letter. The grid *content* (the letters themselves)
 *   is a parallel `List<Char>`; this layer only tracks what the player has earned
 *   the right to see.
 * - [FOUND] `1` — a cell on a matched line. It stays lettered and inert.
 *
 * Keeping geometry and content separate is what lets the board validate a selection
 * without knowing the alphabet: the rules never spell anything, they only measure
 * lines. The words themselves live in the generator's placements and the state.
 *
 * ## The one mutation
 *
 * [evaluate] turns a drag (first cell, last cell) into either a matched line —
 * every placed word the drag spells, which can be more than one when two words
 * overlap on the same line — or nothing. A wrong drag is not an error state; it is
 * a shake-less no-op, because a wrong guess in a word search costs the player only
 * the seconds it took to trace (method §6: nothing here is urgent enough for red).
 */
object WordsearchRules {

    const val SIZE = 12
    const val CELLS = SIZE * SIZE

    const val HIDDEN = 0
    const val FOUND = 1

    /** The eight directions a word may run. */
    val DIRECTIONS: List<Pair<Int, Int>> = listOf(
        0 to 1,    // east
        1 to 0,    // south
        0 to -1,   // west
        -1 to 0,   // north
        1 to 1,    // south-east
        1 to -1,   // south-west
        -1 to 1,   // north-east
        -1 to -1,  // north-west
    )

    fun rowOf(index: Int): Int = index / SIZE
    fun colOf(index: Int): Int = index % SIZE

    /** Cells along a line from [start] stepping [dr]/[dc] for [length] cells, or null off-board. */
    fun line(start: Int, dr: Int, dc: Int, length: Int): List<Int>? {
        val out = ArrayList<Int>(length)
        var r = rowOf(start)
        var c = colOf(start)
        for (i in 0 until length) {
            if (r !in 0 until SIZE || c !in 0 until SIZE) return null
            out += r * SIZE + c
            r += dr
            c += dc
        }
        return out
    }

    /** The letters [line] reads off the grid, in walk order. */
    fun spell(cells: List<Char>, line: List<Int>): String =
        line.map { cells[it] }.joinToString("")

    /**
     * Resolves a drag from [first] to [last] against [placements].
     *
     * A drag is only a straight line in one of the eight directions — anything else
     * is null, matching the genre's convention that wiggles simply do not count.
     * Returns every word the drag spells, which can be two when words share a line:
     * "RUSTLE" backward can be exactly "ELSTUR" forward and hide a second word on
     * the same cells. Reversed drags match reversed words, as the genre expects.
     */
    fun evaluate(
        placements: List<Placement>,
        first: Int,
        last: Int,
    ): List<Placement> {
        val dr = rowOf(last) - rowOf(first)
        val dc = colOf(last) - colOf(first)
        val span = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
        if (span == 0) return emptyList()
        // Straight line only: one axis flat, or a perfect diagonal. The two guards
        // below are the whole contract — note there is deliberately no length cap
        // here. (A previous draft rejected |dr|>1 && |dc|>1, which reads like a
        // knight-move filter but actually kills every diagonal longer than 2 cells,
        // making generator-placed diagonal words unfindable.)
        if (dr != 0 && dc != 0 && kotlin.math.abs(dr) != kotlin.math.abs(dc)) return emptyList()

        val length = span + 1
        val walked = line(first, dr.sign(), dc.sign(), length) ?: return emptyList()
        if (walked.last() != last) return emptyList()

        // A word matches from either end: the player may trace it first-letter-first
        // or last-letter-first, so the walked line can equal the placement's cells
        // reversed. (Placement cells are always first-letter-first.)
        return placements.filter { placement ->
            placement.cells == walked || placement.cells.asReversed() == walked
        }
    }

    private fun Int.sign(): Int = if (this == 0) 0 else if (this > 0) 1 else -1

    /** Every matched word's cells marked found. */
    fun applyFound(cells: List<Int>, foundLines: List<List<Int>>): List<Int> {
        val out = cells.toMutableList()
        for (l in foundLines) for (cell in l) out[cell] = FOUND
        return out
    }

    /** The win: every placed word found. */
    fun isWin(cells: List<Int>, placements: List<Placement>): Boolean =
        placements.all { p -> p.cells.all { cells[it] == FOUND } }

    /** The one quantity (method §2): words found over words placed. */
    fun completion(placements: List<Placement>, found: Set<String>): Float =
        if (placements.isEmpty()) 0f else found.size.toFloat() / placements.size
}

/**
 * One word's placement on the board. Immutable; two placements can overlap in cells
 * (crossing words), which is exactly what the generator is allowed to produce and
 * what [WordsearchRules.evaluate] handles by matching whole lines.
 */
data class Placement(
    val word: String,
    /** Board indexes in reading order — the word's first letter first. */
    val cells: List<Int>,
    /**
     * True when the word was laid last-letter-first — the generator's walk ran
     * against the reading order. [cells] always stores first-letter-first
     * regardless, so this is descriptive only — matching ([WordsearchRules.evaluate])
     * runs from either end — kept for the generator's bookkeeping and future stats.
     * It does not survive persistence: a `WORD:start:end` triple re-derives its
     * geometry, and a first-letter-first triple decodes with `reversed = false`.
     */
    val reversed: Boolean,
)
