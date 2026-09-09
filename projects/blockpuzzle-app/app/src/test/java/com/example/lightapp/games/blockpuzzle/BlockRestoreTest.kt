package com.example.lightapp.games.blockpuzzle

import com.example.lightapp.studio.persistence.BlockCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Restore validation and the codec round-trip for `blockpuzzle(8)`. Restore's job:
 * accept exactly the runs legal play can produce, reject everything else to "start
 * fresh" — a tray triple naming a family the catalog does not have is corruption,
 * not difficulty.
 */
class BlockRestoreTest {

    private fun freshState() = BlockState.fresh(
        cells = List(BlockRules.CELLS) { BlockRules.EMPTY },
        tray = requireNotNull(BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = 11)),
        difficulty = "Classic",
    )

    @Test
    fun `codec round-trips a legal run`() {
        val session = freshState().toSession()
        val cells = BlockCodec.decodeCells(BlockCodec.encodeCells(session.cells))
        val tray = BlockCodec.decodeTray(BlockCodec.encodeTray(session.tray))
        assertEquals(session.cells, cells)
        assertEquals(session.tray, tray)
    }

    @Test
    fun `a legal run restores`() {
        val state = requireNotNull(BlockRestore.stateOrNull(freshState().toSession()))
        assertEquals("Classic", state.difficulty)
        assertEquals(freshState().tray, state.tray)
    }

    @Test
    fun `a finished run is not restored as in-progress`() {
        // "Finished" is semantic here: an empty tray is a dead run (the deal was
        // consumed with no refill possible), so restore refuses it.
        val session = freshState().toSession().copy(tray = emptyList())
        assertNull(BlockRestore.stateOrNull(session))
    }

    @Test
    fun `a tray with no legal landing is rejected`() {
        // A board with one empty cell cannot host any piece in the catalog.
        val cells = List(BlockRules.CELLS) { BlockRules.FILLED }.toMutableList()
        cells[BlockRules.CELLS - 1] = BlockRules.EMPTY
        val session = freshState()
            .copy(cells = cells.toList())
            .toSession()
        assertNull(BlockRestore.stateOrNull(session))
    }

    @Test
    fun `a tray triple naming an unknown family is rejected`() {
        val base = freshState().toSession()
        val corrupt = base.copy(tray = listOf("NOPE:0:0"))
        assertNull(BlockRestore.stateOrNull(corrupt))
    }

    @Test
    fun `a malformed triple is rejected`() {
        val base = freshState().toSession()
        assertNull(BlockRestore.stateOrNull(base.copy(tray = listOf("O:x:y"))))
        assertNull(BlockRestore.stateOrNull(base.copy(tray = listOf("O:0"))))
    }

    @Test
    fun `an out-of-range variant or density is rejected`() {
        val base = freshState().toSession()
        assertNull(BlockRestore.stateOrNull(base.copy(tray = listOf("O:9:0"))))
        assertNull(BlockRestore.stateOrNull(base.copy(tray = listOf("O:0:9"))))
    }

    @Test
    fun `the daily label is accepted`() {
        val state = requireNotNull(
            BlockRestore.stateOrNull(
                freshState().copy(difficulty = BlockViewModel.DAILY_NAME).toSession(),
            ),
        )
        assertEquals(BlockViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is rejected`() {
        val base = freshState().toSession()
        assertNull(BlockRestore.stateOrNull(base.copy(difficulty = "Fortnightly")))
    }
}
