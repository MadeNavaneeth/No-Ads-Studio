package com.example.lightapp.games.sudoku

import com.example.lightapp.studio.persistence.SudokuCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hint engine and the note sweep. Both are pure functions on the board the
 * player can see — the tests assert the deduction claim itself, because a hint
 * that asserts a value the board does not force would be a lie with UI on it.
 */
class SudokuHintTest {

    /** An empty board: every cell has nine candidates, so nothing is forced. */
    private fun emptyState(): SudokuState = SudokuState.fromPuzzle(IntArray(81), "Moderate")

    /** Places [digit] via [SudokuState.fromPuzzle]-shaped givens at [indexes]. */
    private fun stateWithGivens(vararg placements: Pair<Int, Int>): SudokuState {
        val puzzle = IntArray(81)
        for ((index, digit) in placements) puzzle[index] = digit
        return SudokuState.fromPuzzle(puzzle, "Moderate")
    }

    @Test
    fun `a cell whose peers hold eight digits is forced to the ninth`() {
        // Row 0: digits 1-8 placed in columns 0-7, column 8 empty and forced to 9.
        val placements = (0..7).map { col -> col to (col + 1) }.toTypedArray()
        val state = stateWithGivens(*placements)

        val hint = SudokuHint.nakedSingle(state)
        assertEquals(8, hint?.index)
        assertEquals(9, hint?.digit)
    }

    @Test
    fun `the selected cell is preferred over the reading-order scan`() {
        // Two forced cells: (0,8)→9 as before, and (5,8)→9 in row 5.
        val placements = (0..7).map { col -> col to (col + 1) } +
            (45..52).map { col -> col to (col - 44) }
        val state = stateWithGivens(*placements.toTypedArray())

        val pointed = state.copy(selected = 53) // row 5, col 8
        val hint = SudokuHint.nakedSingle(pointed)
        assertEquals(53, hint?.index)

        val unpointed = state.copy(selected = null)
        assertEquals(8, SudokuHint.nakedSingle(unpointed)?.index)
    }

    @Test
    fun `an empty board offers no hint`() {
        assertNull(SudokuHint.nakedSingle(emptyState()))
    }

    @Test
    fun `a filled cell is never hinted`() {
        val state = stateWithGivens(0 to 5)
        assertNull(SudokuHint.hintAt(state, 0))
    }

    @Test
    fun `a cell with two candidates is not claimed as forced`() {
        // Row 0 holds 1-7, so the empty cell (0,8) has candidates {8, 9} — not forced.
        val placements = (0..6).map { col -> col to (col + 1) }.toTypedArray()
        val state = stateWithGivens(*placements)
        assertNull(SudokuHint.hintAt(state, 8))
    }

    @Test
    fun `hints are stable in reading order for the same board`() {
        val placements = (0..7).map { col -> col to (col + 1) } +
            (9..16).map { col -> col to (col - 8) }
        val state = stateWithGivens(*placements.toTypedArray())
        val first = SudokuHint.nakedSingle(state)
        val second = SudokuHint.nakedSingle(state)
        assertEquals(first, second)
    }

    // ─── the auto-clean sweep ─────────────────────────────────────────────────

    @Test
    fun `the sweep removes the placed digit from every peer holding it`() {
        val notes = List(81) { 0 }
            .toMutableList()
            .also {
                it[1] = SudokuCodec.toggleNote(0, 4)      // same row — swept
                it[9] = SudokuCodec.toggleNote(0, 4)      // same column — swept
                it[10] = SudokuCodec.toggleNote(0, 4)     // same box — swept
                it[80] = SudokuCodec.toggleNote(0, 4)     // unrelated — untouched
                it[2] = SudokuCodec.toggleNote(0, 7)      // peer, other digit — untouched
            }
        val swept = cleanedPeerNotes(notes, index = 0, digit = 4)

        assertTrue(1 in swept && 9 in swept && 10 in swept)
        assertEquals(SudokuCodec.toggleNote(notes[1], 4), swept[1])
        assertTrue(80 !in swept)
        assertTrue(2 !in swept)
    }

    @Test
    fun `the sweep on a noteless board is empty`() {
        assertTrue(cleanedPeerNotes(List(81) { 0 }, index = 0, digit = 4).isEmpty())
    }
}
