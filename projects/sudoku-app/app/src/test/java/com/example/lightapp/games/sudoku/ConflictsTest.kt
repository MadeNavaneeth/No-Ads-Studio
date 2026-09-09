package com.example.lightapp.games.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Conflict detection and completion — decision D20.
 *
 * All pure JVM. The interesting cases are the boundaries: an empty cell never conflicts,
 * a full-but-wrong grid is not solved, and completion excludes givens.
 */
class ConflictsTest {

    private fun grid(vararg pairs: Pair<Int, Int>): List<Int> {
        val out = MutableList(Conflicts.CELLS) { 0 }
        pairs.forEach { (index, value) -> out[index] = value }
        return out
    }

    @Test
    fun `peers are the twenty cells sharing a row, column, or box`() {
        // Every cell has 8 row peers + 8 column peers + 4 remaining box peers = 20.
        for (i in 0 until Conflicts.CELLS) {
            assertEquals("cell $i", 20, Conflicts.peersOf(i).size)
        }
    }

    @Test
    fun `a cell is not its own peer`() {
        for (i in 0 until Conflicts.CELLS) {
            assertFalse(Conflicts.peersOf(i).contains(i))
        }
    }

    @Test
    fun `an empty cell never conflicts`() {
        val g = grid(0 to 0, 1 to 0)
        assertFalse(Conflicts.conflictsAt(g, 0))
    }

    @Test
    fun `a duplicate in a row conflicts`() {
        val g = grid(0 to 5, 8 to 5)
        assertTrue(Conflicts.conflictsAt(g, 0))
        assertTrue(Conflicts.conflictsAt(g, 8))
        assertEquals(setOf(0, 8), Conflicts.allConflicts(g))
    }

    @Test
    fun `a duplicate in a column conflicts`() {
        val g = grid(0 to 7, 72 to 7) // same column, first and last row
        assertEquals(setOf(0, 72), Conflicts.allConflicts(g))
    }

    @Test
    fun `a duplicate in a box conflicts`() {
        val g = grid(0 to 3, 10 to 3) // both in the top-left 3x3
        assertEquals(0, Conflicts.boxOf(0))
        assertEquals(0, Conflicts.boxOf(10))
        assertEquals(setOf(0, 10), Conflicts.allConflicts(g))
    }

    @Test
    fun `the same value in a different row, column, and box does not conflict`() {
        val g = grid(0 to 4, 40 to 4) // row 0 col 0 vs row 4 col 4
        assertTrue(Conflicts.allConflicts(g).isEmpty())
    }

    @Test
    fun `an empty grid is not solved`() {
        assertFalse(Conflicts.isSolved(grid()))
    }

    @Test
    fun `a full grid with a duplicate is not solved`() {
        // Every cell 1: full, but conflicting everywhere.
        val full = List(Conflicts.CELLS) { 1 }
        assertFalse(Conflicts.isSolved(full))
    }

    @Test
    fun `a valid complete grid is solved`() {
        // Latin-square construction that also satisfies the 3x3 boxes.
        val solved = List(Conflicts.CELLS) { i ->
            val r = i / 9
            val c = i % 9
            ((r * 3 + r / 3 + c) % 9) + 1
        }
        assertTrue(Conflicts.allConflicts(solved).isEmpty())
        assertTrue(Conflicts.isSolved(solved))
    }

    @Test
    fun `exhausted digits are those placed nine times`() {
        val nineOnes = MutableList(Conflicts.CELLS) { 0 }
        repeat(9) { k -> nineOnes[k * 9] = 1 } // one per row, all in column 0
        assertEquals(setOf(1), Conflicts.exhaustedDigits(nineOnes))
    }

    // ─── completion, decision D20 ─────────────────────────────────────────────

    @Test
    fun `a fresh puzzle reads zero percent`() {
        val givens = grid(0 to 5, 1 to 3)
        val state = SudokuState(
            givens = givens,
            entries = MutableList(Conflicts.CELLS) { 0 },
            notes = MutableList(Conflicts.CELLS) { 0 },
            difficulty = "Moderate",
        )
        assertEquals(0f, state.completion, 0.0001f)
    }

    @Test
    fun `completion excludes givens from both terms`() {
        // 2 givens, so 79 cells are initially empty. Fill 79 and it reads 100%.
        val givens = grid(0 to 5, 1 to 3)
        val entries = MutableList(Conflicts.CELLS) { i -> if (i >= 2) 1 else 0 }
        val state = SudokuState(
            givens = givens,
            entries = entries,
            notes = MutableList(Conflicts.CELLS) { 0 },
            difficulty = "Moderate",
        )
        assertEquals(1f, state.completion, 0.0001f)
    }

    @Test
    fun `a given is reported as given and its value wins in the merged grid`() {
        val state = SudokuState(
            givens = grid(0 to 5),
            entries = grid(0 to 9), // must never win
            notes = MutableList(Conflicts.CELLS) { 0 },
            difficulty = "Moderate",
        )
        assertTrue(state.isGiven(0))
        assertEquals(5, state.valueAt(0))
    }
}

/**
 * Remaining-digit counts — the number the pad shows beneath each key.
 *
 * Separate from [ConflictsTest] only because it arrived later; same pure-JVM territory.
 */
class RemainingCountsTest {

    private fun grid(vararg pairs: Pair<Int, Int>): List<Int> {
        val out = MutableList(Conflicts.CELLS) { 0 }
        pairs.forEach { (index, value) -> out[index] = value }
        return out
    }

    @Test
    fun `an empty grid has nine of every digit left`() {
        val remaining = Conflicts.remainingCounts(grid())

        assertEquals(9, remaining.size)
        assertTrue(remaining.values.all { it == 9 })
    }

    @Test
    fun `placing a digit decrements only that digit`() {
        val remaining = Conflicts.remainingCounts(grid(0 to 7))

        assertEquals(8, remaining[7])
        assertEquals(9, remaining[1])
    }

    @Test
    fun `a digit placed nine times has none left`() {
        val nines = (0 until 9).map { it * 9 to 4 }.toTypedArray()

        assertEquals(0, Conflicts.remainingCounts(grid(*nines))[4])
    }

    @Test
    fun `a digit placed more than nine times clamps at zero`() {
        // Reachable mid-mistake: conflicts are permitted rather than blocked, so a grid can
        // legitimately hold ten of a digit. "-1 left" is not a thing to tell someone.
        val tenFives = (0 until 10).map { it to 5 }.toTypedArray()

        assertEquals(0, Conflicts.remainingCounts(grid(*tenFives))[5])
    }

    @Test
    fun `exhausted digits are exactly those with none remaining`() {
        val nineOnes = (0 until 9).map { it * 9 to 1 }.toTypedArray()
        val remaining = Conflicts.remainingCounts(grid(*nineOnes))

        assertEquals(
            remaining.filterValues { it == 0 }.keys,
            Conflicts.exhaustedDigits(grid(*nineOnes)),
        )
    }

    @Test
    fun `counts never include zero as a digit`() {
        // Zero is the empty marker, not a value the pad can offer.
        assertEquals((1..9).toSet(), Conflicts.remainingCounts(grid()).keys)
    }
}
