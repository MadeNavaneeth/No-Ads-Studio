package com.example.lightapp.games.akari

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules of `akari(10)`, asserted at the seams the view model sits on. Pure JVM
 * tests, milliseconds each — the cheapest place to find a bug in this repo.
 */
class AkariRulesTest {

    private fun empty() = List(AkariRules.CELLS) { AkariRules.EMPTY }

    private fun List<Int>.with(index: Int, value: Int): List<Int> =
        toMutableList().also { it[index] = value }

    /** A real solved position: four regions split by a wall cross at rows and
     *  columns 3 and 7, each region covered by bulbs on its local diagonal —
     *  every white cell lit, no two bulbs sharing a run, no clues to satisfy. */
    private fun solvedBoard(): List<Int> {
        var cells = empty()
        for (i in 0 until AkariRules.CELLS) {
            val r = AkariRules.rowOf(i)
            val c = AkariRules.colOf(i)
            if (r == 3 || r == 7 || c == 3 || c == 7) cells = cells.with(i, AkariRules.WALL)
        }
        for (b in listOf(0, 8, 11, 19, 22, 40, 51, 62, 88, 99)) {
            cells = cells.with(b, AkariRules.BULB)
        }
        return cells
    }

    // ─── light ────────────────────────────────────────────────────────────────

    @Test
    fun `a bulb lights its row until a wall`() {
        // A blank wall stands mid-board so the run stops there.
        val cells = empty().with(45, AkariRules.BULB).with(48, AkariRules.WALL)
        val lit = AkariRules.litCells(cells)
        // Row 4: cells 40..47 lit, 48 walled, 49 dark.
        for (c in 40..47) assertTrue("cell $c should be lit", lit[c])
        assertFalse(lit[48])
        assertFalse(lit[49])
        // Column 5 lights up and down from row 4.
        assertTrue(lit[5])
        assertTrue(lit[15])
        assertTrue(lit[25])
        assertTrue(lit[35])
        assertTrue(lit[55])
        assertTrue(lit[65])
        // A distant cell on no shared run stays dark.
        assertFalse(lit[0])
    }

    @Test
    fun `a bulb lights itself`() {
        val cells = empty().with(55, AkariRules.BULB)
        val lit = AkariRules.litCells(cells)
        assertTrue(lit[55])
        // A distant cell on no shared run stays dark.
        assertFalse(lit[0])
    }

    @Test
    fun `a numbered wall blocks light like any wall`() {
        val cells = empty().with(45, AkariRules.BULB).with(48, AkariRules.CLUE_1)
        val lit = AkariRules.litCells(cells)
        assertTrue(lit[47])
        assertFalse(lit[49])
    }

    // ─── clashes ──────────────────────────────────────────────────────────────

    @Test
    fun `bulbs in the same run clash`() {
        val cells = empty().with(40, AkariRules.BULB).with(47, AkariRules.BULB)
        assertEquals(setOf(40, 47), AkariRules.clashCells(cells))
    }

    @Test
    fun `a wall between bulbs prevents the clash`() {
        val cells = empty()
            .with(40, AkariRules.BULB)
            .with(44, AkariRules.WALL)
            .with(47, AkariRules.BULB)
        assertTrue(AkariRules.clashCells(cells).isEmpty())
    }

    @Test
    fun `bulbs in different rows and columns do not clash`() {
        val cells = empty().with(0, AkariRules.BULB).with(99, AkariRules.BULB)
        assertTrue(AkariRules.clashCells(cells).isEmpty())
    }

    // ─── clues ────────────────────────────────────────────────────────────────

    @Test
    fun `a clue counts orthogonal bulbs only`() {
        // Wall at (4,4)=44; bulbs left and right of it, and one diagonal that
        // must not count.
        val cells = empty()
            .with(43, AkariRules.BULB)
            .with(45, AkariRules.BULB)
            .with(33, AkariRules.BULB)
        assertEquals(2, AkariRules.adjacentBulbCount(cells, 44))
    }

    @Test
    fun `an over-satisfied clue is not a win`() {
        // Clue 1 at 44 with two adjacent bulbs: every cell lit and legal otherwise.
        val cells = empty()
            .with(44, AkariRules.CLUE_1)
            .with(43, AkariRules.BULB)
            .with(45, AkariRules.BULB)
        assertFalse(AkariRules.isWin(cells))
        assertTrue(AkariRules.exceededWalls(cells).contains(44))
    }

    @Test
    fun `an under-satisfied clue is not a win`() {
        // Clue 1 at 44, one adjacent bulb, but the far column left dark.
        val cells = empty()
            .with(44, AkariRules.CLUE_1)
            .with(43, AkariRules.BULB)
            .with(99, AkariRules.WALL)
        assertFalse(AkariRules.isWin(cells))
        assertTrue(AkariRules.exceededWalls(cells).isEmpty())
    }

    // ─── the win ──────────────────────────────────────────────────────────────

    @Test
    fun `a fully solved board wins`() {
        val cells = solvedBoard()
        assertTrue(AkariRules.isWin(cells))
        assertEquals(1f, AkariRules.completion(cells), 0.001f)
    }

    @Test
    fun `removing one bulb from a solved board un-solves it`() {
        // 22 = (2,2) is a real bulb of the solved board; its cell goes dark.
        val cells = solvedBoard().with(22, AkariRules.EMPTY)
        assertFalse(AkariRules.isWin(cells))
    }

    @Test
    fun `an empty board is not a win`() {
        assertFalse(AkariRules.isWin(empty()))
    }

    // ─── input and the one quantity ───────────────────────────────────────────

    @Test
    fun `the tap cycle is ground to bulb and back`() {
        assertEquals(AkariRules.BULB, AkariRules.cycle(AkariRules.EMPTY))
        assertEquals(AkariRules.EMPTY, AkariRules.cycle(AkariRules.BULB))
        assertEquals(AkariRules.WALL, AkariRules.cycle(AkariRules.WALL))
        assertEquals(AkariRules.CLUE_2, AkariRules.cycle(AkariRules.CLUE_2))
    }

    @Test
    fun `completion reads lit share of white cells`() {
        // One bulb at 0 lights its row (10 cells) and its column's other nine —
        // 19 white cells of 100.
        val cells = empty().with(0, AkariRules.BULB)
        assertEquals(0.19f, AkariRules.completion(cells), 0.001f)
    }
}
