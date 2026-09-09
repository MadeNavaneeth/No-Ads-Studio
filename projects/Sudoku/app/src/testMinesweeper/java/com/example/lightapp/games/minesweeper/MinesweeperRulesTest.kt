package com.example.lightapp.games.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules for `minesweeper(10)`. Pure JVM tests, milliseconds each — the cheapest
 * place to find a bug in this repo.
 */
class MinesweeperRulesTest {

    private fun emptyBoard() = List(MinesweeperRules.CELLS) { MinesweeperRules.UNKNOWN }

    private fun boardOf(vararg specials: Pair<Int, Int>): List<Int> {
        val board = emptyBoard().toMutableList()
        for ((index, value) in specials) board[index] = value
        return board
    }

    private fun minesAt(vararg indexes: Int): List<Boolean> =
        List(MinesweeperRules.CELLS) { it in indexes }

    @Test
    fun `centre cell has eight neighbours and corner has three`() {
        assertEquals(8, MinesweeperRules.neighbours(40).size)
        assertEquals(3, MinesweeperRules.neighbours(0).size)
        assertEquals(3, MinesweeperRules.neighbours(MinesweeperRules.CELLS - 1).size)
    }

    @Test
    fun `adjacency counts every neighbouring mine`() {
        // Mine at 0; neighbours of cell 4 (corner row adjacency along the top edge).
        val mines = minesAt(0)
        val counts = MinesweeperRules.adjacency(mines)
        assertEquals(1, counts[1])
        assertEquals(1, counts[9])
        assertEquals(1, counts[10])
        assertEquals(0, counts[40])
    }

    @Test
    fun `reveal on a mine detonates it and reveals the other mines`() {
        val mines = minesAt(40, 41)
        val cells = boardOf(41 to MinesweeperRules.FLAGGED) // the other mine, correctly flagged
        val next = requireNotNull(MinesweeperRules.reveal(cells, mines, 40)) { "tapping a mine must detonate" }
        assertEquals(MinesweeperRules.DETONATED, next[40])
        // A correctly flagged mine is left alone; only unflagged mines are shown.
        assertEquals(MinesweeperRules.FLAGGED, next[41])
    }

    @Test
    fun `reveal flood-fills a zero region up to its number ring`() {
        // One mine in the far corner, first tap in the near corner. Every cell but
        // the mine is zero-connected or on a zero region's ring, so the flood opens
        // the whole board except the mine itself — and never, ever the mine.
        val mines = minesAt(80)
        val next = requireNotNull(MinesweeperRules.reveal(emptyBoard(), mines, 0)) { "fresh-cell reveal must change the board" }
        for (i in 0 until MinesweeperRules.CELLS) {
            if (mines[i]) {
                assertEquals("mine $i must stay covered", MinesweeperRules.UNKNOWN, next[i])
            } else {
                assertEquals("safe cell $i must be revealed", MinesweeperRules.REVEALED, next[i])
            }
        }
    }

    @Test
    fun `reveal rejects revealed and flagged cells without changing the board`() {
        val mines = minesAt(80)
        assertNull(MinesweeperRules.reveal(boardOf(5 to MinesweeperRules.REVEALED), mines, 5))
        assertNull(MinesweeperRules.reveal(boardOf(5 to MinesweeperRules.FLAGGED), mines, 5))
    }

    @Test
    fun `toggleFlag flips unknown to flagged and back`() {
        val mines = minesAt(80)
        val flagged = requireNotNull(MinesweeperRules.toggleFlag(emptyBoard(), 12)) { "unexpected null" }
        assertEquals(MinesweeperRules.FLAGGED, flagged[12])
        val cleared = requireNotNull(MinesweeperRules.toggleFlag(flagged, 12)) { "unexpected null" }
        assertEquals(MinesweeperRules.UNKNOWN, cleared[12])
    }

    @Test
    fun `toggleFlag refuses a revealed cell`() {
        assertNull(MinesweeperRules.toggleFlag(boardOf(12 to MinesweeperRules.REVEALED), 12))
    }

    @Test
    fun `win is every safe cell revealed, flags not required`() {
        val mines = minesAt(80)
        val cells = List(MinesweeperRules.CELLS) { i ->
            if (i == 80) MinesweeperRules.UNKNOWN else MinesweeperRules.REVEALED
        }
        assertTrue(MinesweeperRules.isWin(cells, mines))
        // One covered safe cell keeps the game open.
        val notYet = cells.toMutableList().also { it[0] = MinesweeperRules.UNKNOWN }
        assertFalse(MinesweeperRules.isWin(notYet, mines))
    }

    @Test
    fun `minesRemaining counts flags on non-mines too`() {
        val mines = minesAt(40, 41)
        val cells = boardOf(
            40 to MinesweeperRules.FLAGGED,  // correct
            0 to MinesweeperRules.FLAGGED,   // wrong — spends the number honestly
        )
        assertEquals(MinesweeperRules.MINE_COUNT - 2, MinesweeperRules.minesRemaining(mines, cells))
    }

    @Test
    fun `completion is revealed safe cells over total safe cells`() {
        val mines = minesAt(0, 1, 2, 3, 4, 5, 6, 7, 8, 80)
        val cells = List(MinesweeperRules.CELLS) { i -> if (i == 9) MinesweeperRules.REVEALED else MinesweeperRules.UNKNOWN }
        assertEquals(1f / (MinesweeperRules.CELLS - 10), MinesweeperRules.completion(cells, mines), 1e-6f)
    }

    @Test
    fun `fresh board reads zero completion`() {
        val mines = minesAt(0)
        assertEquals(0f, MinesweeperRules.completion(emptyBoard(), mines), 1e-6f)
    }
}
