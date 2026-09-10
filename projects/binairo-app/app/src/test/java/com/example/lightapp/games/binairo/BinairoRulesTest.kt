package com.example.lightapp.games.binairo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules of `binairo(10)`, asserted at the seams the view model sits on. Pure JVM
 * tests, milliseconds each — the cheapest place to find a bug in this repo.
 */
class BinairoRulesTest {

    private fun empty() = List(BinairoRules.CELLS) { BinairoRules.EMPTY }

    private fun List<Int>.with(index: Int, value: Int): List<Int> =
        toMutableList().also { it[index] = value }

    /** A real solved position: every row and every column is a distinct cyclic
     * shift of `s`, which is balanced (5/5), has no circular run of three, and has
     * full period — so no two rows repeat and no two columns repeat. */
    private fun solvedBoard(): List<Int> {
        val s = intArrayOf(1, 1, 0, 0, 1, 0, 1, 0, 1, 0) // 1 = ONE, 0 = ZERO
        return List(BinairoRules.CELLS) { i ->
            val r = BinairoRules.rowOf(i)
            val c = BinairoRules.colOf(i)
            if (s[(c - r + 10) % 10] == 1) BinairoRules.ONE else BinairoRules.ZERO
        }
    }

    // ─── runs ─────────────────────────────────────────────────────────────────

    @Test
    fun `three in a row is a violation`() {
        val cells = empty()
            .with(0, BinairoRules.ONE)
            .with(1, BinairoRules.ONE)
            .with(2, BinairoRules.ONE)
        assertEquals(setOf(0, 1, 2), BinairoRules.runViolationCells(cells))
    }

    @Test
    fun `two in a row is not`() {
        val cells = empty()
            .with(0, BinairoRules.ONE)
            .with(1, BinairoRules.ONE)
        assertTrue(BinairoRules.runViolationCells(cells).isEmpty())
    }

    @Test
    fun `a run broken by an empty cell is not yet a violation`() {
        val cells = empty()
            .with(0, BinairoRules.ONE)
            .with(1, BinairoRules.ONE)
            .with(3, BinairoRules.ONE)
        assertTrue(BinairoRules.runViolationCells(cells).isEmpty())
    }

    @Test
    fun `three in a column is a violation`() {
        val cells = empty()
            .with(5, BinairoRules.ZERO)
            .with(15, BinairoRules.ZERO)
            .with(25, BinairoRules.ZERO)
        assertEquals(setOf(5, 15, 25), BinairoRules.runViolationCells(cells))
    }

    @Test
    fun `a run of four marks all four cells`() {
        val cells = empty()
            .with(10, BinairoRules.ONE)
            .with(11, BinairoRules.ONE)
            .with(12, BinairoRules.ONE)
            .with(13, BinairoRules.ONE)
        assertEquals(setOf(10, 11, 12, 13), BinairoRules.runViolationCells(cells))
    }

    // ─── balance ──────────────────────────────────────────────────────────────

    @Test
    fun `six ones in a line is over-balanced`() {
        var grid = empty()
        for (c in 30..35) grid = grid.with(c, BinairoRules.ONE)
        assertTrue(BinairoRules.overBalancedLines(grid).contains(3))
    }

    @Test
    fun `five and five is balanced`() {
        var grid = empty()
        for (c in 30..34) grid = grid.with(c, BinairoRules.ONE)
        for (c in 35..39) grid = grid.with(c, BinairoRules.ZERO)
        assertTrue(BinairoRules.overBalancedLines(grid).isEmpty())
    }

    // ─── duplicates ───────────────────────────────────────────────────────────

    @Test
    fun `two identical decided rows are a duplicate`() {
        var grid = empty()
        for (c in 0..4) grid = grid.with(c, BinairoRules.ONE)
        for (c in 5..9) grid = grid.with(c, BinairoRules.ZERO)
        for (c in 10..14) grid = grid.with(c, BinairoRules.ONE)
        for (c in 15..19) grid = grid.with(c, BinairoRules.ZERO)
        assertEquals(0 to 1, BinairoRules.duplicateLine(grid))
    }

    @Test
    fun `a partial line is never called a duplicate`() {
        var grid = empty()
        for (c in 0..4) grid = grid.with(c, BinairoRules.ONE)
        for (c in 5..9) grid = grid.with(c, BinairoRules.ZERO)
        // Row 1 only partially matches row 0.
        for (c in 10..14) grid = grid.with(c, BinairoRules.ONE)
        assertNull(BinairoRules.duplicateLine(grid))
    }

    @Test
    fun `a row and a column are never compared`() {
        // Row 0 and column 0 hold the same values; different kinds, no duplicate.
        var grid = empty()
        for (c in 0..4) grid = grid.with(c, BinairoRules.ONE)
        for (c in 5..9) grid = grid.with(c, BinairoRules.ZERO)
        for (r in 0..4) grid = grid.with(r * 10, BinairoRules.ONE)
        for (r in 5..9) grid = grid.with(r * 10, BinairoRules.ZERO)
        assertNull(BinairoRules.duplicateLine(grid))
    }

    // ─── the tap cycle ────────────────────────────────────────────────────────

    @Test
    fun `the tap cycle is empty to one to zero and back`() {
        assertEquals(BinairoRules.ONE, BinairoRules.cycle(BinairoRules.EMPTY))
        assertEquals(BinairoRules.ZERO, BinairoRules.cycle(BinairoRules.ONE))
        assertEquals(BinairoRules.EMPTY, BinairoRules.cycle(BinairoRules.ZERO))
    }

    // ─── the win ──────────────────────────────────────────────────────────────

    @Test
    fun `a fully solved board wins`() {
        val cells = solvedBoard()
        assertTrue(BinairoRules.isWin(cells))
        assertEquals(1f, BinairoRules.completion(cells), 0.001f)
    }

    @Test
    fun `an empty board is not a win`() {
        assertFalse(BinairoRules.isWin(empty()))
    }

    @Test
    fun `a solved board with one cell cleared is not a win`() {
        val cells = solvedBoard().with(0, BinairoRules.EMPTY)
        assertFalse(BinairoRules.isWin(cells))
    }

    @Test
    fun `a full board with a run of three is not a win`() {
        val cells = solvedBoard()
            .with(0, BinairoRules.ONE)
            .with(1, BinairoRules.ONE)
            .with(2, BinairoRules.ONE)
        assertFalse(BinairoRules.isWin(cells))
    }

    // ─── the one quantity ─────────────────────────────────────────────────────

    @Test
    fun `completion reads decided share of the board`() {
        var grid = empty()
        for (i in 0 until 50) grid = grid.with(i, BinairoRules.ONE)
        assertEquals(0.5f, BinairoRules.completion(grid), 0.001f)
    }

    @Test
    fun `linesSolved counts decided run-free lines`() {
        // Column 0 fully decided, balanced, no runs; row 0 is not fully decided.
        val col = listOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 1)
        var grid = empty()
        for (r in 0 until 10) {
            grid = grid.with(r * 10, if (col[r] == 1) BinairoRules.ONE else BinairoRules.ZERO)
        }
        assertEquals(1, BinairoRules.linesSolved(grid))
    }
}