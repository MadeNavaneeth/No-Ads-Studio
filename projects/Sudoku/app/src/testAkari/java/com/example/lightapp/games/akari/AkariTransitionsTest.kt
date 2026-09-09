package com.example.lightapp.games.akari

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker's transitions, asserted directly — the pure functions the view model
 * calls, no Android required — plus the undo history's contract.
 */
class AkariTransitionsTest {

    // ─── the picker ───────────────────────────────────────────────────────────

    @Test
    fun `opening the picker from playing keeps the run as return-to`() {
        val state = AkariState(cells = List(AkariRules.CELLS) { AkariRules.WALL }, difficulty = "Moderate")
        val picker = AkariDifficultyTransition.open(AkariUi.Playing(state))
        assertEquals("Moderate", picker.currentDifficulty)
        assertEquals(AkariUi.Playing(state), picker.returnTo)
    }

    @Test
    fun `opening the picker abandons an in-flight generation`() {
        val picker = AkariDifficultyTransition.open(AkariUi.Generating)
        assertNull(picker.returnTo)
    }

    @Test
    fun `dismissing returns exactly what the picker kept`() {
        val state = AkariState(cells = List(AkariRules.CELLS) { AkariRules.WALL }, difficulty = "Moderate")
        val picker = AkariDifficultyTransition.open(AkariUi.Playing(state))
        assertEquals(AkariUi.Playing(state), AkariDifficultyTransition.dismiss(picker))
    }

    @Test
    fun `dismissing with nothing behind means generate`() {
        val picker = AkariDifficultyTransition.open(AkariUi.Generating)
        assertNull(AkariDifficultyTransition.dismiss(picker))
    }

    // ─── history ──────────────────────────────────────────────────────────────

    @Test
    fun `undo reverts the last tap first`() {
        val history = AkariHistory()
        history.record(AkariHistory.Move(index = 5, previousValue = AkariRules.EMPTY))
        history.record(AkariHistory.Move(index = 9, previousValue = AkariRules.BULB))
        assertEquals(AkariHistory.Move(9, AkariRules.BULB), history.revert())
        assertEquals(AkariHistory.Move(5, AkariRules.EMPTY), history.revert())
        assertNull(history.revert())
        assertFalse(history.canUndo)
    }

    @Test
    fun `history is bounded and drops the oldest`() {
        val history = AkariHistory()
        repeat(AkariHistory.DEFAULT_BOUND + 10) { i ->
            history.record(AkariHistory.Move(index = i % AkariRules.CELLS, previousValue = i))
        }
        // The ten oldest entries are gone: the first pop is entry 10, not entry 0.
        assertEquals(AkariHistory.Move(index = 10 % AkariRules.CELLS, previousValue = 10), history.revert())
        assertEquals(AkariHistory.DEFAULT_BOUND - 1, count(history))
    }

    @Test
    fun `a new run clears the past`() {
        val history = AkariHistory()
        history.record(AkariHistory.Move(0, AkariRules.EMPTY))
        history.clear()
        assertFalse(history.canUndo)
        assertNull(history.revert())
    }

    private fun count(history: AkariHistory): Int {
        var n = 0
        while (history.revert() != null) n++
        return n
    }
}
