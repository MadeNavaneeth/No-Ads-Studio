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
        // The newest entry is still on top — undo is LIFO at the bound.
        assertEquals(AkariHistory.Move(index = 59, previousValue = 59), history.revert())
        // Draining the rest, the last survivor is entry 10: the ten oldest
        // entries (0–9) were evicted oldest-first, exactly the G4 contract.
        var last: AkariHistory.Move? = null
        while (history.revert()?.also { last = it } != null) { /* drain */ }
        assertEquals(AkariHistory.Move(index = 10, previousValue = 10), last)
        assertFalse(history.canUndo)
    }

    @Test
    fun `a new run clears the past`() {
        val history = AkariHistory()
        history.record(AkariHistory.Move(0, AkariRules.EMPTY))
        history.clear()
        assertFalse(history.canUndo)
        assertNull(history.revert())
    }
}
