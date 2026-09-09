package com.example.lightapp.studio.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `minesweeper(10)` codec — decision D7, the same boring strings as sudoku and
 * nonogram. Structural tests only; semantic validation lives with the game's restore
 * step, mirroring how NonogramCodecTest splits from NonogramRestore.
 */
class MinesweeperCodecTest {

    private val mines = List(MinesweeperSession.CELLS) { if (it % 8 == 0) 1 else 0 }
    private val cells = List(MinesweeperSession.CELLS) { it % 4 }

    @Test
    fun `round-trips mines and cells`() {
        val encodedMines = MinesweeperCodec.encodeMines(mines)
        val encodedCells = MinesweeperCodec.encodeCells(cells)
        assertEquals(MinesweeperSession.CELLS, encodedMines.length)
        assertEquals(MinesweeperSession.CELLS, encodedCells.length)
        assertEquals(mines, MinesweeperCodec.decodeMines(encodedMines))
        assertEquals(cells, MinesweeperCodec.decodeCells(encodedCells))
    }

    @Test
    fun `encodes to the documented digit ranges`() {
        assertTrue(MinesweeperCodec.encodeMines(mines).all { it in '0'..'1' })
        assertTrue(MinesweeperCodec.encodeCells(cells).all { it in '0'..'3' })
    }

    @Test
    fun `refuses wrong lengths`() {
        assertNull(MinesweeperCodec.decodeMines("01"))
        assertNull(MinesweeperCodec.decodeCells("0".repeat(80)))
        assertNull(MinesweeperCodec.decodeMines(null))
    }

    @Test
    fun `refuses out-of-range digits`() {
        assertNull(MinesweeperCodec.decodeMines("2".repeat(MinesweeperSession.CELLS)))
        assertNull(MinesweeperCodec.decodeCells("9".repeat(MinesweeperSession.CELLS)))
    }
}
