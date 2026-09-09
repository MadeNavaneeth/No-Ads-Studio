package com.example.lightapp.games.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramRulesTest {

    // ─── clues ──────────────────────────────────────────────────────────

    @Test
    fun runsAreCountedInOrder() {
        assertEquals(listOf(1, 2, 1), NonogramRules.cluesForLine(listOf(1, 0, 0, 1, 1, 0, 1)))
    }

    @Test
    fun emptyLineHasNoClue() {
        assertEquals(emptyList<Int>(), NonogramRules.cluesForLine(listOf(0, 0, 0)))
    }

    @Test
    fun fullLineIsOneRun() {
        assertEquals(listOf(3), NonogramRules.cluesForLine(listOf(1, 1, 1)))
    }

    @Test
    fun edgeRunsCount() {
        assertEquals(listOf(1, 1), NonogramRules.cluesForLine(listOf(1, 0, 1)))
    }

    // ─── placements ─────────────────────────────────────────────────────

    @Test
    fun fullRunHasOnePlacement() {
        val out = NonogramRules.placements(5, listOf(5))
        assertEquals(1, out.size)
        assertTrue(out[0].all { it })
    }

    @Test
    fun emptyClueHasOneEmptyPlacement() {
        val out = NonogramRules.placements(5, emptyList())
        assertEquals(1, out.size)
        assertTrue(out[0].none { it })
    }

    @Test
    fun singleCellHasOnePlacementPerSlot() {
        assertEquals(3, NonogramRules.placements(3, listOf(1)).size)
    }

    // ─── solver ─────────────────────────────────────────────────────────

    private fun border(): List<Int> = List(NonogramRules.CELLS) { i ->
        val r = NonogramRules.rowOf(i)
        val c = NonogramRules.colOf(i)
        if (r == 0 || r == 9 || c == 0 || c == 9) 1 else 0
    }

    @Test
    fun framedBoardIsDeducible() {
        assertTrue(NonogramRules.isDeducible(border()))
    }

    @Test
    fun diagonalNeedsGuessing() {
        // Every row and column clues [1]: no line forces any cell.
        val diagonal = List(NonogramRules.CELLS) { i ->
            if (NonogramRules.rowOf(i) == NonogramRules.colOf(i)) 1 else 0
        }
        assertFalse(NonogramRules.isDeducible(diagonal))
    }

    @Test
    fun emptyBoardIsVacuouslyDeducible() {
        // Documents why the generator enforces a fill range: an empty solution
        // is "solvable" and an instant win, so it must never ship.
        assertTrue(NonogramRules.isDeducible(List(NonogramRules.CELLS) { 0 }))
    }

    // ─── input ──────────────────────────────────────────────────────────

    @Test
    fun tapCyclesUnknownFilledUnknown() {
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        val filled = NonogramRules.tap(cells, 0)
        assertEquals(NonogramRules.FILLED, filled[0])
        assertEquals(NonogramRules.UNKNOWN, NonogramRules.tap(filled, 0)[0])
    }

    @Test
    fun tapClearsAMark() {
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        cells[3] = NonogramRules.MARKED
        assertEquals(NonogramRules.UNKNOWN, NonogramRules.tap(cells, 3)[3])
    }

    @Test
    fun markTogglesOnUnknown() {
        val cells = List(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        val marked = requireNotNull(NonogramRules.toggleMark(cells, 5))
        assertEquals(NonogramRules.MARKED, marked[5])
        assertEquals(NonogramRules.UNKNOWN, requireNotNull(NonogramRules.toggleMark(marked, 5))[5])
    }

    @Test
    fun markRejectsFilledCells() {
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        cells[2] = NonogramRules.FILLED
        assertNull(NonogramRules.toggleMark(cells, 2))
    }

    @Test
    fun paintSkipsDecidedCells() {
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        cells[1] = NonogramRules.MARKED
        cells[2] = NonogramRules.FILLED
        assertNull(NonogramRules.paintCell(cells, 1, NonogramRules.FILLED))
        assertNull(NonogramRules.paintCell(cells, 2, NonogramRules.UNKNOWN))
        assertEquals(
            NonogramRules.FILLED,
            requireNotNull(NonogramRules.paintCell(cells, 0, NonogramRules.FILLED))[0]
        )
        assertNull(NonogramRules.paintCell(cells, 0, NonogramRules.UNKNOWN)?.let {
            if (it[0] == NonogramRules.UNKNOWN) null else it
        })
    }

    // ─── win and progress ───────────────────────────────────────────────

    private fun plus(): List<Int> = List(NonogramRules.CELLS) { i ->
        val r = NonogramRules.rowOf(i)
        val c = NonogramRules.colOf(i)
        if (r == 4 || c == 4) 1 else 0
    }

    @Test
    fun exactMatchWins() {
        val solution = plus()
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        solution.forEachIndexed { i, v -> if (v == 1) cells[i] = NonogramRules.FILLED }
        // Marks elsewhere do not spoil the win.
        cells[0] = NonogramRules.MARKED
        assertTrue(NonogramRules.isWin(cells, solution))
    }

    @Test
    fun extraFillSpoilsTheWin() {
        val solution = plus()
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        solution.forEachIndexed { i, v -> if (v == 1) cells[i] = NonogramRules.FILLED }
        cells[0] = NonogramRules.FILLED
        assertFalse(NonogramRules.isWin(cells, solution))
    }

    @Test
    fun emptyBoardNeverWins() {
        val cells = List(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        assertFalse(NonogramRules.isWin(cells, List(NonogramRules.CELLS) { 0 }))
    }

    @Test
    fun completionRunsZeroToOne() {
        val solution = plus()
        val fills = solution.count { it == 1 }
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        assertEquals(0f, NonogramRules.completion(cells, solution))
        var correct = 0
        solution.forEachIndexed { i, v ->
            if (v == 1) {
                cells[i] = NonogramRules.FILLED
                correct++
                assertEquals(
                    correct.toFloat() / fills,
                    NonogramRules.completion(cells, solution),
                    0.001f,
                )
            }
        }
        assertEquals(1f, NonogramRules.completion(cells, solution))
    }

    @Test
    fun solvedLinesCountRowsAndColumns() {
        val solution = border()
        val cells = MutableList(NonogramRules.CELLS) { NonogramRules.UNKNOWN }
        assertEquals(0, NonogramRules.solvedLineCount(cells, solution))
        // Fill the entire first row correctly: row 0 solved, nothing else.
        for (c in 0 until NonogramRules.SIZE) cells[c] = NonogramRules.FILLED
        assertEquals(1, NonogramRules.solvedLineCount(cells, solution))
    }
}
