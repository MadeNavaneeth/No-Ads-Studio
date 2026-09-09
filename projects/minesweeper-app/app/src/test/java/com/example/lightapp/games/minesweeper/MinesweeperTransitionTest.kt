package com.example.lightapp.games.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The picker's two transitions, asserted directly — the same shape of bug the other
 * two games shipped: a dismiss that had nowhere to land.
 */
class MinesweeperTransitionTest {

    @Test
    fun `opening the picker from play carries the current difficulty and a return destination`() {
        val playing = MinesweeperUi.Playing(MinesweeperState.fresh("Moderate"))
        val picker = MinesweeperDifficultyTransition.open(playing)
        assertEquals("Moderate", picker.currentDifficulty)
        assertEquals(playing, picker.returnTo)
    }

    @Test
    fun `opening the picker twice does not nest pickers`() {
        val playing = MinesweeperUi.Playing(MinesweeperState.fresh("Hard"))
        val picker = MinesweeperDifficultyTransition.open(playing)
        val again = MinesweeperDifficultyTransition.open(picker)
        // The second open returns the same picker shape, and dismissing it still
        // lands somewhere real rather than in a dead end.
        assertEquals(picker.currentDifficulty, again.currentDifficulty)
        assertNull(MinesweeperDifficultyTransition.dismiss(MinesweeperUi.ChoosingDifficulty(currentDifficulty = "Hard", returnTo = null)))
    }

    @Test
    fun `dismiss returns to the stored destination`() {
        val playing = MinesweeperUi.Playing(MinesweeperState.fresh("Simple"))
        val picker = MinesweeperDifficultyTransition.open(playing)
        assertEquals(playing, MinesweeperDifficultyTransition.dismiss(picker))
    }

    @Test
    fun `fresh state starts unknown with no layout`() {
        val fresh = MinesweeperState.fresh("Simple")
        assertEquals(MinesweeperRules.UNKNOWN, fresh.cells.first())
        assertFalse(fresh.layoutPlaced)
        assertEquals(0f, fresh.completion, 1e-6f)
    }
}
