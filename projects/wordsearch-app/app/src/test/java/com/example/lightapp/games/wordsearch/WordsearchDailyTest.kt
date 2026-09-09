package com.example.lightapp.games.wordsearch

import com.example.lightapp.studio.persistence.WordsearchSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The daily contract for `wordsearch(12)` (decision D32): the day's seed must lay a
 * grid, and restore must accept the `Daily` label a stored daily session carries.
 */
class WordsearchDailyTest {

    /** A plausible epoch day — 2026-ish. The daily seeds with values of this size. */
    private val day = 20_660L

    @Test
    fun `the day's seed lays a grid`() {
        val board = requireNotNull(WordsearchGenerator.generate(WordsearchViewModel.DAILY_DIFFICULTY, seed = day))
        assertEquals(WordsearchRules.CELLS, board.letters.size)
    }

    @Test
    fun `the same day lays the same grid`() {
        assertEquals(
            WordsearchGenerator.generate(WordsearchViewModel.DAILY_DIFFICULTY, seed = day),
            WordsearchGenerator.generate(WordsearchViewModel.DAILY_DIFFICULTY, seed = day),
        )
    }

    @Test
    fun `a daily session restores under the Daily label`() {
        val board = requireNotNull(WordsearchGenerator.generate("Moderate", seed = day))
        // Sessions are built through the state's own serializer — the production path.
        val session = WordsearchState(
            letters = board.letters,
            placements = board.placements,
            difficulty = WordsearchViewModel.DAILY_NAME,
        ).toSession()
        val state = requireNotNull(WordsearchRestore.stateOrNull(session))
        assertEquals(WordsearchViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is still rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Moderate", seed = day))
        val session = WordsearchState(
            letters = board.letters,
            placements = board.placements,
            difficulty = "Fortnightly",
        ).toSession()
        assertNull(WordsearchRestore.stateOrNull(session))
    }
}
