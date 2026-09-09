package com.example.lightapp.games.akari

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily contract for `akari(10)` (decision D32): the day's seed must generate a
 * valid board, and restore must accept the `Daily` label a stored daily session
 * carries. The view model needs an Android context, so the JVM tests hold the
 * contract at the seams.
 */
class AkariDailyTest {

    /** A plausible epoch day — 2026-ish. The daily seeds with values of this size. */
    private val day = 20_660L

    @Test
    fun `the day's seed generates a unique board`() {
        val board = requireNotNull(AkariGenerator.generate(AkariViewModel.DAILY_DIFFICULTY, seed = day)) {
            "the daily seed must generate"
        }
        assertEquals(AkariRules.CELLS, board.size)
        assertEquals(1, AkariGenerator.countSolutions(board, cap = 2))
    }

    @Test
    fun `the same day generates the same board`() {
        assertEquals(
            AkariGenerator.generate(AkariViewModel.DAILY_DIFFICULTY, seed = day),
            AkariGenerator.generate(AkariViewModel.DAILY_DIFFICULTY, seed = day),
        )
    }

    @Test
    fun `a daily session restores under the Daily label`() {
        val board = requireNotNull(AkariGenerator.generate("Moderate", seed = day))
        // Sessions are built through the state's own serializer — the production path.
        val session = AkariState(cells = board, difficulty = AkariViewModel.DAILY_NAME).toSession()
        val state = requireNotNull(AkariRestore.stateOrNull(session))
        assertEquals(AkariViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is still rejected`() {
        val board = requireNotNull(AkariGenerator.generate("Moderate", seed = day))
        val session = AkariState(cells = board, difficulty = "Fortnightly").toSession()
        assertNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `the daily board is dark at the start`() {
        val board = requireNotNull(AkariGenerator.generate(AkariViewModel.DAILY_DIFFICULTY, seed = day))
        assertTrue("no bulbs in a daily start state", board.none { it == AkariRules.BULB })
    }
}
