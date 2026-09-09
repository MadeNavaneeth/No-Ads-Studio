package com.example.lightapp.games.sudoku

import com.example.lightapp.studio.persistence.SudokuCodec
import com.example.lightapp.studio.persistence.SudokuSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Restore validation — the boundary between "these bytes decode" and "this is a puzzle".
 *
 * `SudokuCodecTest` already covers the first. These cover the second, which is the gap a
 * well-formed but meaningless save fell through: it restored as a grid the player could
 * never clear, with nothing on screen to say why.
 *
 * A rejected save must return `null` rather than throw, because the caller's only sane
 * response is to generate a fresh puzzle.
 */
class SudokuRestoreTest {

    /**
     * A valid, solvable board with 30 givens.
     *
     * Built from a known-good complete grid by blanking cells, so the givens are
     * guaranteed internally consistent — the property under test must not be accidentally
     * satisfied by a hand-typed board.
     */
    private fun validGivens(revealed: Int = 30): List<Int> {
        val solved = solvedGrid()
        return List(Conflicts.CELLS) { i -> if (i < revealed) solved[i] else 0 }
    }

    /** A complete, conflict-free 9×9. Each row is the one above shifted by three. */
    private fun solvedGrid(): List<Int> {
        val pattern = { r: Int, c: Int -> (r * 3 + r / 3 + c) % 9 + 1 }
        return List(Conflicts.CELLS) { i -> pattern(i / 9, i % 9) }
    }

    private fun session(
        givens: List<Int>,
        entries: List<Int> = SudokuCodec.emptyGrid(),
        notes: List<Int> = SudokuCodec.emptyGrid(),
        elapsedMs: Long = 0L,
        difficulty: String = "Moderate",
        mistakes: Int = 0,
    ) = SudokuSession(givens, entries, notes, elapsedMs, difficulty, mistakes)

    @Test
    fun `the fixture grid really is a solved sudoku`() {
        // Guards the tests below: if this shifts, every "valid" case is meaningless.
        assertEquals(emptySet<Int>(), Conflicts.allConflicts(solvedGrid()))
    }

    @Test
    fun `a well-formed session restores`() {
        val state = requireNotNull(SudokuRestore.stateOrNull(session(validGivens()))) { "well-formed session must restore" }

        assertEquals("Moderate", state.difficulty)
        assertEquals(30, state.givens.count { it != 0 })
    }

    @Test
    fun `a board with too few givens is rejected`() {
        // 16 is one below the proven minimum for a unique solution, so it cannot have
        // come from the generator.
        assertNull(SudokuRestore.stateOrNull(session(validGivens(revealed = 16))))
    }

    @Test
    fun `a board with exactly the minimum givens is accepted`() {
        assertNotNull(
            SudokuRestore.stateOrNull(
                session(validGivens(revealed = SudokuRestore.MIN_GIVENS))
            )
        )
    }

    @Test
    fun `an empty grid is rejected rather than restored as a blank puzzle`() {
        assertNull(SudokuRestore.stateOrNull(session(SudokuCodec.emptyGrid())))
    }

    @Test
    fun `a fully populated grid is rejected because it is not a puzzle`() {
        assertNull(SudokuRestore.stateOrNull(session(solvedGrid())))
    }

    @Test
    fun `givens that contradict each other are rejected`() {
        // Well-formed by every codec rule, and unsolvable: two 1s in the top row.
        val contradictory = validGivens().toMutableList()
        contradictory[0] = 1
        contradictory[1] = 1

        assertNull(SudokuRestore.stateOrNull(session(contradictory)))
    }

    @Test
    fun `a player entry on top of a given is rejected`() {
        // The invariant that makes a given uneditable across a restart. If this save were
        // accepted, the merged grid would show the given while entries held something
        // else, and erasing would appear to do nothing.
        val givens = validGivens()
        val entries = SudokuCodec.emptyGrid().toMutableList()
        entries[0] = 9

        assertNull(SudokuRestore.stateOrNull(session(givens, entries = entries)))
    }

    @Test
    fun `a player entry in an empty cell is preserved`() {
        val givens = validGivens(revealed = 30)
        val entries = SudokuCodec.emptyGrid().toMutableList()
        entries[80] = 4 // last cell, guaranteed blank at 30 givens

        val state = requireNotNull(SudokuRestore.stateOrNull(session(givens, entries = entries))) { "entry in an empty cell must restore" }

        assertEquals(4, state.entries[80])
    }

    @Test
    fun `a negative clock and mistake count are clamped, not fatal`() {
        // Nonsense numbers attached to a valid board. Losing the board over them would be
        // the wrong trade.
        val state = requireNotNull(
            SudokuRestore.stateOrNull(
                session(validGivens(), elapsedMs = -5_000L, mistakes = -3)
            )
        ) { "nonsense numbers on a valid board must still restore" }

        assertEquals(0L, state.elapsedMs)
        assertEquals(0, state.mistakes)
    }

    @Test
    fun `an unrecognised difficulty still restores`() {
        // The board is what matters. A difficulty string the app no longer knows is
        // resolved to a default at the point of use, not by discarding the puzzle.
        assertNotNull(
            SudokuRestore.stateOrNull(session(validGivens(), difficulty = "Impossible"))
        )
    }
}
