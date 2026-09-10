package com.example.lightapp.games.binairo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker's transitions, asserted directly — the pure functions the view model
 * calls, no Android required — plus the undo history's contract.
 */
class BinairoTransitionsTest {

    private fun playing() = BinairoState(
        givens = List(BinairoRules.CELLS) { BinairoRules.EMPTY },
        entries = List(BinairoRules.CELLS) { BinairoRules.EMPTY },
        difficulty = "Moderate",
    )

    // ─── the picker ───────────────────────────────────────────────────────────

    @Test
    fun `opening the picker from playing keeps the run as return-to`() {
        val state = playing()
        val picker = BinairoDifficultyTransition.open(BinairoUi.Playing(state))
        assertEquals("Moderate", picker.currentDifficulty)
        assertEquals(BinairoUi.Playing(state), picker.returnTo)
    }

    @Test
    fun `opening the picker abandons an in-flight generation`() {
        val picker = BinairoDifficultyTransition.open(BinairoUi.Generating)
        assertNull(picker.returnTo)
    }

    @Test
    fun `dismissing returns exactly what the picker kept`() {
        val state = playing()
        val picker = BinairoDifficultyTransition.open(BinairoUi.Playing(state))
        assertEquals(BinairoUi.Playing(state), BinairoDifficultyTransition.dismiss(picker))
    }

    @Test
    fun `dismissing with nothing behind means generate`() {
        val picker = BinairoDifficultyTransition.open(BinairoUi.Generating)
        assertNull(BinairoDifficultyTransition.dismiss(picker))
    }

    // ─── history ──────────────────────────────────────────────────────────────

    @Test
    fun `undo reverts the last tap first`() {
        val history = BinairoHistory()
        history.record(BinairoHistory.Move(index = 5, previousValue = BinairoRules.EMPTY))
        history.record(BinairoHistory.Move(index = 9, previousValue = BinairoRules.ONE))
        assertEquals(BinairoHistory.Move(9, BinairoRules.ONE), history.revert())
        assertEquals(BinairoHistory.Move(5, BinairoRules.EMPTY), history.revert())
        assertNull(history.revert())
        assertFalse(history.canUndo)
    }

    @Test
    fun `history is bounded and drops the oldest`() {
        val history = BinairoHistory()
        repeat(BinairoHistory.DEFAULT_BOUND + 10) { i ->
            history.record(BinairoHistory.Move(index = i % BinairoRules.CELLS, previousValue = i))
        }
        // The newest entry is still on top — undo is LIFO at the bound.
        assertEquals(BinairoHistory.Move(index = 59, previousValue = 59), history.revert())
        // Draining the rest, the last survivor is entry 10: the ten oldest
        // entries (0–9) were evicted oldest-first, exactly the G4 contract.
        var last: BinairoHistory.Move? = null
        while (history.revert()?.also { last = it } != null) { /* drain */ }
        assertEquals(BinairoHistory.Move(index = 10, previousValue = 10), last)
        assertFalse(history.canUndo)
    }

    @Test
    fun `a new run clears the past`() {
        val history = BinairoHistory()
        history.record(BinairoHistory.Move(0, BinairoRules.EMPTY))
        history.clear()
        assertFalse(history.canUndo)
        assertNull(history.revert())
    }
}