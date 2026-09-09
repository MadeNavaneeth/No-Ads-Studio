package com.example.lightapp.games.nonogram

import com.example.lightapp.studio.persistence.NonogramSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The daily contract for `nonogram(10)` (decision D32), asserted at the two seams the
 * view model sits on: the generator must produce a board from a real epoch-day seed,
 * and restore must accept the `Daily` label a stored daily session legitimately
 * carries. The view model itself needs an Android context, so the JVM tests hold the
 * contract; the instrumented layer holds the wiring.
 */
class NonogramDailyTest {

    /** A plausible epoch day — 2026-ish. The daily seeds with values of this size. */
    private val day = 20_660L

    @Test
    fun `the day's seed generates a board`() {
        val board = requireNotNull(NonogramGenerator.generate(NonogramViewModel.DAILY_DIFFICULTY, seed = day))
        assertEquals(NonogramRules.CELLS, board.size)
    }

    @Test
    fun `the same day generates the same board`() {
        val a = NonogramGenerator.generate(NonogramViewModel.DAILY_DIFFICULTY, seed = day)
        val b = NonogramGenerator.generate(NonogramViewModel.DAILY_DIFFICULTY, seed = day)
        assertEquals(a, b)
    }

    @Test
    fun `a daily session restores under the Daily label`() {
        val solution = requireNotNull(NonogramGenerator.generate("Moderate", seed = day))
        // Sessions are built through the state's own serializer — the production path.
        val session = NonogramState.fresh(solution, NonogramViewModel.DAILY_NAME).toSession()
        val state = requireNotNull(NonogramRestore.stateOrNull(session))
        assertEquals(NonogramViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is still rejected`() {
        val solution = requireNotNull(NonogramGenerator.generate("Moderate", seed = day))
        val session = NonogramState.fresh(solution, "Fortnightly").toSession()
        assertNull(NonogramRestore.stateOrNull(session))
    }
}
