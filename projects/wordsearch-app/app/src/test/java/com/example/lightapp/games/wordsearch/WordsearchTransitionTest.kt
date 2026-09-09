package com.example.lightapp.games.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The picker's two transitions, as pure functions. The subtle one: opening the picker
 * over an in-flight generation must *not* offer that generation as the dismiss
 * destination — it was cancelled, and going "back" to it would mean waiting on work
 * that no longer exists.
 */
class WordsearchTransitionTest {

    private fun playing() = WordsearchUi.Playing(
        WordsearchState(
            letters = List(WordsearchRules.CELLS) { 'A' },
            placements = listOf(Placement(word = "POST", cells = listOf(0, 1, 2), reversed = false)),
            difficulty = "Moderate",
        ),
    )

    @Test
    fun `open from playing carries the difficulty and the return trip`() {
        val playing = playing()
        val picker = WordsearchDifficultyTransition.open(playing)

        assertEquals("Moderate", picker.currentDifficulty)
        assertEquals(playing, picker.returnTo)
    }

    @Test
    fun `open from generating offers no return trip`() {
        val picker = WordsearchDifficultyTransition.open(WordsearchUi.Generating)

        assertNull(picker.currentDifficulty)
        assertNull(picker.returnTo)
    }

    @Test
    fun `open from a picker offers no return trip`() {
        // Two pickers stacked would be a modal over a modal; refusing to return to
        // the first one is what keeps that state unrepresentable.
        val picker = WordsearchDifficultyTransition.open(WordsearchUi.Generating)
        val stacked = WordsearchDifficultyTransition.open(picker)

        assertNull(stacked.returnTo)
    }

    @Test
    fun `open from complete keeps the finished board for the return trip`() {
        val complete = WordsearchUi.Complete(playing().state)
        val picker = WordsearchDifficultyTransition.open(complete)

        assertEquals(complete, picker.returnTo)
    }

    @Test
    fun `dismiss lands on the return trip or asks for a fresh grid`() {
        val playing = playing()
        val picker = WordsearchDifficultyTransition.open(playing)
        assertEquals(playing, WordsearchDifficultyTransition.dismiss(picker))

        val orphan = WordsearchDifficultyTransition.open(WordsearchUi.Generating)
        assertNull(WordsearchDifficultyTransition.dismiss(orphan))
    }
}
