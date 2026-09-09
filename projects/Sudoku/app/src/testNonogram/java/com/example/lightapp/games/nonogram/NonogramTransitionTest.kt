package com.example.lightapp.games.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramTransitionTest {

    private fun playing(difficulty: String = "Moderate") = NonogramUi.Playing(
        NonogramState.fresh(List(NonogramRules.CELLS) { 0 }, difficulty)
    )

    @Test
    fun openRecordsPlayingAsReturn() {
        val from = playing("Hard")
        val picker = NonogramDifficultyTransition.open(from)
        assertEquals("Hard", picker.currentDifficulty)
        assertEquals(from, picker.returnTo)
    }

    @Test
    fun openNeverRecordsGenerating() {
        val picker = NonogramDifficultyTransition.open(NonogramUi.Generating)
        assertNull(picker.returnTo)
        assertNull(picker.currentDifficulty)
    }

    @Test
    fun dismissReturnsToPlaying() {
        val from = playing()
        val picker = NonogramDifficultyTransition.open(from)
        assertEquals(from, NonogramDifficultyTransition.dismiss(picker))
    }

    @Test
    fun dismissWithNowhereToGoReturnsNull() {
        val picker = NonogramDifficultyTransition.open(NonogramUi.Generating)
        assertNull(NonogramDifficultyTransition.dismiss(picker))
    }
}
