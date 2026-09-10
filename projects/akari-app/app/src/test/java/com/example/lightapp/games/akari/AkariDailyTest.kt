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

    companion object {
        /** A hand-built start board for the label-contract tests. Restore validates
         *  shape and label — not provenance — so these tests have no reason to pay
         *  the generator's uniqueness-proof cost (which the daily-seed tests pay
         *  where the seed itself is the subject). */
        private val startBoard: List<Int> = run {
            val cells = List(AkariRules.CELLS) { i ->
                val r = AkariRules.rowOf(i)
                val c = AkariRules.colOf(i)
                if (r == 3 || r == 7 || c == 3 || c == 7) AkariRules.WALL else AkariRules.EMPTY
            }.toMutableList()
            cells[7] = AkariRules.CLUE_1
            cells[33] = AkariRules.CLUE_2
            cells[47] = AkariRules.CLUE_0
            cells[74] = AkariRules.CLUE_1
            cells[87] = AkariRules.CLUE_2
            cells[97] = AkariRules.CLUE_0
            cells.toList()
        }
    }

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
        // Sessions are built through the state's own serializer — the production path.
        val session = AkariState(cells = startBoard, difficulty = AkariViewModel.DAILY_NAME).toSession()
        val state = requireNotNull(AkariRestore.stateOrNull(session))
        assertEquals(AkariViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is still rejected`() {
        val session = AkariState(cells = startBoard, difficulty = "Fortnightly").toSession()
        assertNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `the daily board is dark at the start`() {
        val board = requireNotNull(AkariGenerator.generate(AkariViewModel.DAILY_DIFFICULTY, seed = day))
        assertTrue("no bulbs in a daily start state", board.none { it == AkariRules.BULB })
    }
}
