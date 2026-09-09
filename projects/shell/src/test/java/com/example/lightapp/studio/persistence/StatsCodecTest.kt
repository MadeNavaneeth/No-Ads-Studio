package com.example.lightapp.studio.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Statistics recording and its encoding.
 *
 * Two things are worth guarding here. The record-folding arithmetic, because a best time is
 * the one number a player would actually notice being wrong. And the decoder's tolerance,
 * because stats are never worth a crash — a record the app cannot read should mean "no
 * record", which is what a fresh install shows anyway.
 */
class StatsCodecTest {

    // ─── recording ────────────────────────────────────────────────────────────

    @Test
    fun `a win increments played and won and sets the first record`() {
        val stats = GameStats().recording("Moderate", won = true, durationMs = 240_000L)

        assertEquals(1, stats.played)
        assertEquals(1, stats.won)
        assertEquals(240_000L, stats.bestMs)
    }

    @Test
    fun `a loss counts as played but not won and leaves the record alone`() {
        val stats = GameStats()
            .recording("Moderate", won = true, durationMs = 240_000L)
            .recording("Moderate", won = false, durationMs = 10_000L)

        assertEquals(2, stats.played)
        assertEquals(1, stats.won)
        // The faster time was a loss, so it must not become the record.
        assertEquals(240_000L, stats.bestMs)
    }

    @Test
    fun `only a faster win replaces the record`() {
        val stats = GameStats()
            .recording("Moderate", won = true, durationMs = 240_000L)
            .recording("Moderate", won = true, durationMs = 300_000L)
            .recording("Moderate", won = true, durationMs = 180_000L)

        assertEquals(3, stats.won)
        assertEquals(180_000L, stats.bestMs)
    }

    @Test
    fun `a zero duration is never enshrined as a record`() {
        // A clock that never ran is a bug elsewhere, not a world record.
        val stats = GameStats().recording("Moderate", won = true, durationMs = 0L)

        assertEquals(1, stats.won)
        assertNull(stats.bestMs)
    }

    @Test
    fun `difficulties are recorded independently`() {
        val stats = GameStats()
            .recording("Easy", won = true, durationMs = 100_000L)
            .recording("Hard", won = true, durationMs = 500_000L)

        assertEquals(100_000L, stats.perDifficulty.getValue("Easy").bestMs)
        assertEquals(500_000L, stats.perDifficulty.getValue("Hard").bestMs)
        // The overall best is the fastest across all of them.
        assertEquals(100_000L, stats.bestMs)
        assertEquals(2, stats.played)
    }

    @Test
    fun `an untouched game has no record rather than a zero one`() {
        assertNull(GameStats().bestMs)
        assertEquals(0, GameStats().played)
        assertFalse(DifficultyStats().hasRecord)
    }

    // ─── personal best ────────────────────────────────────────────────────────

    @Test
    fun `the first win at a difficulty is a personal best`() {
        assertTrue(GameStats().isPersonalBest("Moderate", 240_000L))
    }

    @Test
    fun `only a strictly faster time is a personal best`() {
        val stats = GameStats().recording("Moderate", won = true, durationMs = 240_000L)

        assertTrue(stats.isPersonalBest("Moderate", 239_999L))
        assertFalse("equalling the record is not beating it", stats.isPersonalBest("Moderate", 240_000L))
        assertFalse(stats.isPersonalBest("Moderate", 240_001L))
    }

    // ─── encoding ─────────────────────────────────────────────────────────────

    @Test
    fun `stats round-trip`() {
        val original = GameStats()
            .recording("Moderate", won = true, durationMs = 240_000L)
            .recording("Moderate", won = false, durationMs = 10_000L)
            .recording("Hard", won = true, durationMs = 500_000L)

        val restored = StatsCodec.decode(StatsCodec.encode(original))

        assertEquals(original.perDifficulty, restored.perDifficulty)
    }

    @Test
    fun `a difficulty with no record round-trips as no record`() {
        val original = GameStats().recording("Moderate", won = false, durationMs = 5_000L)

        val restored = StatsCodec.decode(StatsCodec.encode(original))

        assertEquals(1, restored.played)
        assertEquals(0, restored.won)
        assertNull(restored.perDifficulty.getValue("Moderate").bestMs)
    }

    @Test
    fun `empty and null decode to empty stats rather than throwing`() {
        assertEquals(GameStats(), StatsCodec.decode(null))
        assertEquals(GameStats(), StatsCodec.decode(""))
        assertEquals(GameStats(), StatsCodec.decode("   "))
    }

    @Test
    fun `malformed rows are skipped and the readable ones survive`() {
        // Partial corruption must not cost the whole record.
        val encoded = "Moderate:12:9:240000;garbage;Hard:x:1:5;Easy:3:2:99000"

        val stats = StatsCodec.decode(encoded)

        assertEquals(setOf("Moderate", "Easy"), stats.perDifficulty.keys)
        assertEquals(9, stats.perDifficulty.getValue("Moderate").won)
        assertEquals(99_000L, stats.perDifficulty.getValue("Easy").bestMs)
    }

    @Test
    fun `an impossible record is rejected`() {
        // More wins than games played cannot have happened.
        assertEquals(GameStats(), StatsCodec.decode("Moderate:2:5:1000"))
        assertEquals(GameStats(), StatsCodec.decode("Moderate:-1:0:1000"))
    }

    @Test
    fun `a difficulty name containing a separator is dropped on encode`() {
        // Rather than writing a row that would decode into two.
        val hostile = GameStats(
            mapOf("Bad:Name" to DifficultyStats(1, 1, 1000L)) +
                mapOf("Good" to DifficultyStats(2, 2, 2000L))
        )

        val restored = StatsCodec.decode(StatsCodec.encode(hostile))

        assertEquals(setOf("Good"), restored.perDifficulty.keys)
    }
}
