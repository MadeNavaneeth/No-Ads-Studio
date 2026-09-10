package com.example.lightapp.games.binairo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily contract for `binairo(10)` (decision D32): the day's seed must generate
 * a valid board deterministically, and restore must accept the `Daily` label a
 * stored daily session carries. The view model needs an Android context, so the
 * JVM tests hold the contract at the seams.
 */
class BinairoDailyTest {

    /** A plausible epoch day — 2026-ish. The daily seeds with values of this size. */
    private val day = 20_660L

    companion object {
        /** Generated once per class — binairo's generation is cheap, and using the
         *  day's real seed keeps the label tests provenance-honest. */
        private val puzzle = requireNotNull(
            BinairoGenerator.generate(BinairoViewModel.DAILY_DIFFICULTY, seed = 20_660L)
        )
    }

    @Test
    fun `the day's seed generates a board`() {
        val board = requireNotNull(
            BinairoGenerator.generate(BinairoViewModel.DAILY_DIFFICULTY, seed = day)
        ) { "the daily seed must generate" }
        assertEquals(BinairoRules.CELLS, board.size)
        assertTrue(
            "a daily start state carries only givens and undecided cells",
            board.all {
                it == BinairoRules.EMPTY || it == BinairoRules.ONE || it == BinairoRules.ZERO
            },
        )
    }

    @Test
    fun `the same day generates the same board`() {
        assertEquals(
            BinairoGenerator.generate(BinairoViewModel.DAILY_DIFFICULTY, seed = day),
            BinairoGenerator.generate(BinairoViewModel.DAILY_DIFFICULTY, seed = day),
        )
    }

    @Test
    fun `a daily session restores under the Daily label`() {
        // Sessions are built through the state's own serializer — the production path.
        val session = BinairoState.start(
            givens = puzzle,
            difficulty = BinairoViewModel.DAILY_NAME,
            dailyDay = day,
        ).toSession()
        val state = requireNotNull(BinairoRestore.stateOrNull(session))
        assertEquals(BinairoViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `the daily start state asks for the whole board`() {
        val state = BinairoState.start(
            givens = puzzle,
            difficulty = BinairoViewModel.DAILY_NAME,
            dailyDay = day,
        )
        assertEquals(0f, state.completion, 0.001f)
        assertTrue(state.entries.all { it == BinairoRules.EMPTY })
    }
}