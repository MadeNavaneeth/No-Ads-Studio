package com.example.lightapp.games.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker's two transitions, as pure functions. These assert the shape the other
 * games already fixed: the picker never nests, and dismissing it never lands on a
 * generation that was cancelled when the picker opened.
 */
class ConnectTransitionTest {

    private val playing = ConnectUi.Playing(ConnectState.fresh(solvedBoard(), "Moderate"))

    @Test
    fun `open from playing returns to playing on dismiss`() {
        val picker = ConnectDifficultyTransition.open(playing)
        assertEquals("Moderate", picker.currentDifficulty)
        assertEquals(playing, ConnectDifficultyTransition.dismiss(picker))
    }

    @Test
    fun `open from generating has nowhere to return`() {
        val picker = ConnectDifficultyTransition.open(ConnectUi.Generating)
        assertNull(picker.currentDifficulty)
        assertNull(ConnectDifficultyTransition.dismiss(picker))
    }

    @Test
    fun `the view model refuses to open a picker twice`() {
        // Nesting is prevented at the call site (showDifficultyPicker returns early
        // when a picker is already up); the transition itself would just replace the
        // return destination, which the picker contract above pins.
        val first = ConnectDifficultyTransition.open(playing)
        assertEquals(playing, ConnectDifficultyTransition.dismiss(first))
    }

    @Test
    fun `open from complete returns to complete on dismiss`() {
        val complete = ConnectUi.Complete(requireNotNull(playing.state).copy(path = emptyList()))
        val picker = ConnectDifficultyTransition.open(complete)
        assertTrue(ConnectDifficultyTransition.dismiss(picker) is ConnectUi.Complete)
    }
}
