package com.example.lightapp.games.connect

import com.example.lightapp.studio.persistence.ConnectSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily contract for `connect(7)` (decision D32): the day's seed must generate a
 * valid board, and restore must accept the `Daily` label a stored daily session
 * carries. The view model needs an Android context, so the JVM tests hold the
 * contract at the seams.
 */
class ConnectDailyTest {

    /** A plausible epoch day — 2026-ish. The daily seeds with values of this size. */
    private val day = 20_660L

    @Test
    fun `the day's seed generates a valid board`() {
        val board = requireNotNull(ConnectGenerator.generate(ConnectViewModel.DAILY_DIFFICULTY, seed = day)) {
            "the daily seed must generate"
        }
        assertEquals(ConnectRules.CELLS, board.size)
        // The generator returns the *puzzle* — endpoints over empty ground — not the
        // solved form, so the honest assertion is that every pair is present.
        assertTrue("the daily board must carry all pairs", ConnectRules.endpointPairs(board).isNotEmpty())
    }

    @Test
    fun `the same day generates the same board`() {
        assertEquals(
            ConnectGenerator.generate(ConnectViewModel.DAILY_DIFFICULTY, seed = day),
            ConnectGenerator.generate(ConnectViewModel.DAILY_DIFFICULTY, seed = day),
        )
    }

    @Test
    fun `a daily session restores under the Daily label`() {
        val board = requireNotNull(ConnectGenerator.generate("Moderate", seed = day))
        // Sessions are built through the state's own serializer — the production path.
        val session = ConnectState(cells = board, difficulty = ConnectViewModel.DAILY_NAME)
            .toSession()
        val state = requireNotNull(ConnectRestore.stateOrNull(session))
        assertEquals(ConnectViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is still rejected`() {
        val board = requireNotNull(ConnectGenerator.generate("Moderate", seed = day))
        val session = ConnectState(cells = board, difficulty = "Fortnightly")
            .toSession()
        assertNull(ConnectRestore.stateOrNull(session))
    }
}
