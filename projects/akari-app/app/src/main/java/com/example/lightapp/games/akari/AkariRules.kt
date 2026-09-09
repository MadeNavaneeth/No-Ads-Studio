package com.example.lightapp.games.akari

/**
 * Rules for `akari(10)`. Pure functions — no Android, no Compose — so every one of
 * these is unit-testable on the JVM.
 *
 * The board is a 10×10 grid of walls (some numbered) and white ground. The player
 * places bulbs; a bulb lights its whole row and column until a wall stops it. The
 * run is won when every white cell is lit, no two bulbs see each other, and every
 * numbered wall touches exactly its number of bulbs.
 *
 * **One cell vocabulary, one array** — walls and clues share the cells array with
 * ground and bulbs, so the board travels as a single string (decision D7's boring
 * shape) and every rule is a function over one `List<Int>`:
 *
 * | value | meaning |
 * |---|---|
 * | [EMPTY] | white ground, unlit or lit — a player toggle target |
 * | [WALL] | a black wall, no number — blocks light |
 * | [BULB] | a placed bulb — a light source, and an obstruction like a wall |
 * | [CLUE_0]..[CLUE_4] | numbered walls: exactly n bulbs must touch them |
 *
 * **Zero red, refusal structural (parity A5, method §6).** A tap on a wall does
 * nothing; a bulb that would clash or exceed a clue is *legal to place* — the
 * contradiction is visible on the board (two lit bulbs, an over-numbered wall)
 * and the win check refuses the board until it is resolved. Nothing announces an
 * error because nothing is an error; an unsolved board is simply unsolved.
 *
 * Bulbs are dots — the design language's own motif — and the lit state is a raise
 * in surface, so the whole game survives grayscale by construction (method §4).
 */
object AkariRules {

    const val SIZE = 10
    const val CELLS = SIZE * SIZE

    const val EMPTY = 0
    const val WALL = 1
    const val BULB = 2
    const val CLUE_0 = 3
    const val CLUE_1 = 4
    const val CLUE_2 = 5
    const val CLUE_3 = 6
    const val CLUE_4 = 7

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE

    /** The number a clue wall demands, or null for every other cell. */
    fun clueNumber(value: Int): Int? =
        if (value in CLUE_0..CLUE_4) value - CLUE_0 else null

    /** Any obstruction: a blank wall, a numbered wall, or a bulb. */
    fun isWall(value: Int): Boolean = value == WALL || value in CLUE_0..CLUE_4

    // ─── light ────────────────────────────────────────────────────────────────

    /**
     * Which cells are lit. A cell is lit when it holds a bulb or shares an
     * unobstructed row or column run with one. Bulbs are always lit by
     * themselves; ground is lit by any bulb that sees it.
     */
    fun litCells(cells: List<Int>): BooleanArray {
        val lit = BooleanArray(CELLS)
        for (i in 0 until CELLS) {
            if (cells[i] != BULB) continue
            lit[i] = true
            val r = rowOf(i)
            val c = colOf(i)
            var up = r - 1
            while (up >= 0 && !isWall(cells[up * SIZE + c])) { lit[up * SIZE + c] = true; up-- }
            var down = r + 1
            while (down < SIZE && !isWall(cells[down * SIZE + c])) { lit[down * SIZE + c] = true; down++ }
            var left = c - 1
            while (left >= 0 && !isWall(cells[r * SIZE + left])) { lit[r * SIZE + left] = true; left-- }
            var right = c + 1
            while (right < SIZE && !isWall(cells[r * SIZE + right])) { lit[r * SIZE + right] = true; right++ }
        }
        return lit
    }

    /** Bulbs that see another bulb — the set of indices in conflict, empty when legal. */
    fun clashCells(cells: List<Int>): Set<Int> {
        val out = mutableSetOf<Int>()
        for (i in 0 until CELLS) {
            if (cells[i] != BULB) continue
            // Scan right and down only: every seeing pair is found from its
            // top-left member exactly once.
            val r = rowOf(i)
            val c = colOf(i)
            var right = c + 1
            while (right < SIZE && !isWall(cells[r * SIZE + right])) {
                if (cells[r * SIZE + right] == BULB) { out += i; out += r * SIZE + right }
                right++
            }
            var down = r + 1
            while (down < SIZE && !isWall(cells[down * SIZE + c])) {
                if (cells[down * SIZE + c] == BULB) { out += i; out += down * SIZE + c }
                down++
            }
        }
        return out
    }

    /** Bulbs directly adjacent to [index] — what a clue wall counts. */
    fun adjacentBulbCount(cells: List<Int>, index: Int): Int {
        val r = rowOf(index)
        val c = colOf(index)
        var count = 0
        if (r > 0 && cells[(r - 1) * SIZE + c] == BULB) count++
        if (r < SIZE - 1 && cells[(r + 1) * SIZE + c] == BULB) count++
        if (c > 0 && cells[r * SIZE + c - 1] == BULB) count++
        if (c < SIZE - 1 && cells[r * SIZE + c + 1] == BULB) count++
        return count
    }

    /**
     * Numbered walls touched by more bulbs than their number allows. The exceed
     * is legal play — the player resolves it by removing a bulb — but it blocks
     * the win, so it is part of the board's honest state.
     */
    fun exceededWalls(cells: List<Int>): Set<Int> {
        val out = mutableSetOf<Int>()
        for (i in 0 until CELLS) {
            val clue = clueNumber(cells[i]) ?: continue
            if (adjacentBulbCount(cells, i) > clue) out += i
        }
        return out
    }

    // ─── input ────────────────────────────────────────────────────────────────

    /**
     * The tap cycle: ground gains a bulb, a bulb returns to ground. Walls — blank
     * or numbered — are not in the cycle; the caller treats an unchanged value as
     * the refusal it is.
     */
    fun cycle(value: Int): Int = when (value) {
        EMPTY -> BULB
        BULB -> EMPTY
        else -> value
    }

    // ─── the win ──────────────────────────────────────────────────────────────

    /**
     * The whole game in one predicate: every white cell lit, no two bulbs seeing
     * each other, no numbered wall exceeded. All three are checked here — even
     * though restore only accepts boards legal play can produce, legality is
     * positional (any bulb on empty ground is reachable), so a saved board can
     * still be mid-contradiction, and only this predicate says when it is done.
     */
    fun isWin(cells: List<Int>): Boolean {
        if (cells.size != CELLS) return false
        val lit = litCells(cells)
        for (i in 0 until CELLS) {
            if (!isWall(cells[i]) && !lit[i]) return false
        }
        return clashCells(cells).isEmpty() && exceededWalls(cells).isEmpty()
    }

    /** The one quantity (method §2): lit white cells over all white cells. */
    fun completion(cells: List<Int>): Float {
        val lit = litCells(cells)
        var white = 0
        var litCount = 0
        for (i in 0 until CELLS) {
            if (isWall(cells[i])) continue
            white++
            if (lit[i]) litCount++
        }
        return if (white == 0) 0f else litCount.toFloat() / white
    }
}
