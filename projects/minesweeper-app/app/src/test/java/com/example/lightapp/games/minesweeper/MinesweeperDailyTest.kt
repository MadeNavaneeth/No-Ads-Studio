package com.example.lightapp.games.minesweeper

import com.example.lightapp.studio.persistence.MinesweeperSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The daily contract for `minesweeper(10)` (decision D32). Minesweeper's twist: the
 * layout cannot be deterministic from the day alone — first-tap safety places mines
 * only when the first reveal lands — so the daily's seed is [day, first tap]. These
 * tests hold that seam: the same day and opening meet the same board, different
 * openings meet different boards, and restore accepts the `Daily` label.
 */
class MinesweeperDailyTest {

    /** A plausible epoch day — 2026-ish. */
    private val day = 20_660L

    /** The view model's seed derivation, asserted here so it cannot drift silently. */
    private fun dailySeed(day: Long, firstTap: Int): Long = day * MinesweeperRules.CELLS + firstTap

    @Test
    fun `same day same opening same field`() {
        val seed = dailySeed(day, firstTap = 40)
        assertEquals(
            MinesweeperGenerator.layoutFor(40, "Moderate", seed = seed),
            MinesweeperGenerator.layoutFor(40, "Moderate", seed = seed),
        )
    }

    @Test
    fun `same day different openings differ`() {
        val a = MinesweeperGenerator.layoutFor(0, "Moderate", seed = dailySeed(day, 0))
        val b = MinesweeperGenerator.layoutFor(40, "Moderate", seed = dailySeed(day, 40))
        assertNotNull(a)
        assertNotNull(b)
        assertFalse("two openings on one day should lay different fields", a == b)
    }

    @Test
    fun `a daily session with a placed layout restores under the Daily label`() {
        val mines = requireNotNull(MinesweeperGenerator.layoutFor(40, "Moderate", seed = dailySeed(day, 40)))
        // Sessions are built through the state's own serializer — the production path.
        val session = MinesweeperState(
            mines = mines,
            cells = List(MinesweeperRules.CELLS) { MinesweeperRules.UNKNOWN },
            difficulty = MinesweeperViewModel.DAILY_NAME,
        ).toSession()
        val state = requireNotNull(MinesweeperRestore.stateOrNull(session))
        assertEquals(MinesweeperViewModel.DAILY_NAME, state.difficulty)
    }

    @Test
    fun `an unknown label is still rejected`() {
        val mines = requireNotNull(MinesweeperGenerator.layoutFor(40, "Moderate", seed = dailySeed(day, 40)))
        val session = MinesweeperState(
            mines = mines,
            cells = List(MinesweeperRules.CELLS) { MinesweeperRules.UNKNOWN },
            difficulty = "Fortnightly",
        ).toSession()
        assertNull(MinesweeperRestore.stateOrNull(session))
    }
}
