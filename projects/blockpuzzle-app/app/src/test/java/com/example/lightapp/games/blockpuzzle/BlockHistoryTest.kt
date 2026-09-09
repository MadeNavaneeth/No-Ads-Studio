package com.example.lightapp.games.blockpuzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The undo contract (decision D17, parity G4), asserted at the seam the view model
 * sits on: LIFO reverts, the 50-deep bound, and the one rule this genre must not
 * bend — a terminal position is never recorded, so undo never resurrects a
 * finished run that has already been emitted and cleared.
 */
class BlockHistoryTest {

    /** A live position that differs from others by its score alone. */
    private fun live(score: Int, over: Boolean = false) =
        BlockState(
            cells = List(BlockRules.CELLS) { BlockRules.EMPTY },
            tray = emptyList(),
            score = score,
            over = over,
        )

    @Test
    fun `revert is LIFO — the last placement is taken back first`() {
        val history = BlockHistory()
        history.record(live(score = 1))
        history.record(live(score = 2))
        history.record(live(score = 3))

        assertEquals(3, history.revert()?.score)
        assertEquals(2, history.revert()?.score)
        assertEquals(1, history.revert()?.score)
        assertNull(history.revert())
        assertFalse(history.canUndo)
    }

    @Test
    fun `history is bounded at 50 — decision D17`() {
        val history = BlockHistory()
        for (score in 1..60) history.record(live(score))

        // 60 recorded against a 50 bound: scores 1–10 fell off the old end.
        // LIFO: the first revert is the newest (60); exactly 50 come back, the
        // last of them being 11 — proof the oldest ten were dropped.
        assertEquals(60, history.revert()?.score)
        var last = 60
        var n = 1
        while (true) {
            last = history.revert()?.score ?: break
            n++
        }
        assertEquals(50, n)
        assertEquals(11, last)
    }

    @Test
    fun `a terminal position is never recorded`() {
        val history = BlockHistory()
        history.record(live(score = 5, over = true))

        assertFalse(history.canUndo)
        assertNull(history.revert())
    }

    @Test
    fun `a new run has no past`() {
        val history = BlockHistory()
        history.record(live(score = 1))
        history.record(live(score = 2))
        history.clear()

        assertFalse(history.canUndo)
        assertNull(history.revert())
    }

    @Test
    fun `empty history reports no undo available`() {
        val history = BlockHistory()
        assertFalse(history.canUndo)
        history.record(live(score = 1))
        assertTrue(history.canUndo)
    }
}
