package com.example.lightapp.games.blockpuzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules of `blockpuzzle(8)`, asserted at the seams the view model sits on. Pure
 * JVM tests, milliseconds each — the cheapest place to find a bug in this repo.
 */
class BlockRulesTest {

    private fun empty() = List(BlockRules.CELLS) { BlockRules.EMPTY }

    /** Bounds-checked [BlockRules.placed] — the raw call trusts its caller. */
    private fun put(cells: List<Int>, piece: BlockRules.Piece, row: Int, col: Int): List<Int> {
        check(BlockRules.canPlace(cells, piece, row, col)) { "test bug: piece does not fit" }
        return BlockRules.placed(cells, piece, row, col)
    }

    /** The catalog's 2×2 square. */
    private val o = BlockRules.Piece("O", variant = 0, density = 0)
    /** The catalog's horizontal 3-bar. */
    private val i3 = BlockRules.Piece("I3", variant = 0, density = 1)

    @Test
    fun `catalog shapes are small and sane`() {
        for ((family, variants) in BlockRules.CATALOG) {
            assertTrue(family, variants.isNotEmpty())
            for (grid in variants) {
                assertTrue(family, grid.size <= 4 && grid.first().size <= 4)
                assertTrue(family, grid.any { row -> row.any { it == 1 } })
            }
        }
        assertTrue(BlockRules.ALL_SHAPES.isNotEmpty())
    }

    @Test
    fun `a piece fits on empty ground and places exactly its cells`() {
        val cells = empty()
        assertTrue(BlockRules.canPlace(cells, o, 0, 0))
        val placed = BlockRules.placed(cells, o, 0, 0)
        assertEquals(o.size, placed.count { it == BlockRules.FILLED })
        // The 2×2 square landed at the top-left corner.
        assertEquals(BlockRules.FILLED, placed[0])
        assertEquals(BlockRules.FILLED, placed[1])
        assertEquals(BlockRules.FILLED, placed[BlockRules.SIZE])
        assertEquals(BlockRules.FILLED, placed[BlockRules.SIZE + 1])
    }

    @Test
    fun `placement is refused off the board and onto blocks`() {
        val cells = empty()
        // The 3-bar cannot start at column 6 — it would run off the right edge.
        assertFalse(BlockRules.canPlace(cells, i3, 0, 6))
        // Nor onto an occupied cell: the O sits on (3,3)-(4,4), so a 3-bar
        // anchored at (3,2) would run across (3,3) — refused.
        val blocked = put(empty(), o, 3, 3)
        assertFalse(BlockRules.canPlace(blocked, i3, 3, 2))
    }

    @Test
    fun `a partial line survives and a full row clears`() {
        var cells = empty()
        // Two 3-bars fill row 0 cols 0..5 — a partial line, six of eight.
        cells = put(cells, i3, 0, 0)
        cells = put(cells, i3, 0, 3)
        val (cleared, lines) = BlockRules.clearFullLines(cells)
        assertEquals(0, lines)
        // Every landed cell survives untouched.
        for (c in 0 until 6) assertEquals(BlockRules.FILLED, cleared[c])

        // A 2-bar completes row 0, and the clear takes all eight cells.
        val i2 = BlockRules.Piece("I2", variant = 0, density = 0)
        cells = put(cleared, i2, 0, 6)
        val (finalCleared, finalLines) = BlockRules.clearFullLines(cells)
        assertEquals(1, finalLines)
        for (c in 0 until BlockRules.SIZE) {
            assertEquals(BlockRules.EMPTY, finalCleared[c])
        }
    }

    @Test
    fun `a full row and column clear together at the crossing`() {
        var cells = empty()
        // Row 0: two O-squares fill cols 0..3, one 3-bar and one 1×1 fill 4..7.
        val i1 = BlockRules.Piece("I1", variant = 0, density = 2)
        cells = put(cells, o, 0, 0)
        cells = put(cells, o, 0, 2)
        cells = put(cells, i3, 0, 4)
        cells = put(cells, i1, 0, 7)
        // Column 0: rows 0-1 are already filled by the row work above; three
        // O-squares cover rows 2..7.
        for (r in 2 until BlockRules.SIZE step 2) cells = put(cells, o, r, 0)
        val (cleared, lines) = BlockRules.clearFullLines(cells)
        // Three lines clear together: row 0, and *both* columns 0 and 1 — the
        // O-squares fill two adjacent columns, which is the point being asserted:
        // every full line clears in the same read, not one per placement.
        assertEquals("row 0 and columns 0-1 clear together", 3, lines)
        // Every cell of row 0 and column 0 is ground again.
        for (c in 0 until BlockRules.SIZE) assertEquals(BlockRules.EMPTY, cleared[c])
        for (r in 0 until BlockRules.SIZE) assertEquals(BlockRules.EMPTY, cleared[r * BlockRules.SIZE])
    }

    @Test
    fun `scoring pays per cell plus per line`() {
        assertEquals(4, BlockRules.scoreFor(placedSize = 4, linesCleared = 0))
        assertEquals(24, BlockRules.scoreFor(placedSize = 4, linesCleared = 2))
    }

    @Test
    fun `completion is filled ground over all cells`() {
        assertEquals(0f, BlockRules.completion(empty()))
        val placed = BlockRules.placed(empty(), o, 0, 0)
        assertEquals(4f / BlockRules.CELLS, BlockRules.completion(placed))
    }
}
