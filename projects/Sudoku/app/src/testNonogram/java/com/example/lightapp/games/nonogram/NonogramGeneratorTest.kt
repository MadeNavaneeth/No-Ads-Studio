package com.example.lightapp.games.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramGeneratorTest {

    @Test
    fun sameSeedSamePuzzle() {
        val first = NonogramGenerator.generate("Moderate", seed = 42L)
        val second = NonogramGenerator.generate("Moderate", seed = 42L)
        assertNotNull(first)
        assertEquals(first, second)
    }

    @Test
    fun everyLevelProducesASolvablePuzzle() {
        for (level in NonogramGenerator.DIFFICULTIES) {
            val puzzle = requireNotNull(
                NonogramGenerator.generate(level, seed = 1000L + level.length),
                { "no puzzle at $level" },
            )
            val fills = puzzle.count { it == 1 }
            assertTrue(
                "$level fills $fills outside range",
                fills in NonogramGenerator.MIN_FILLS..NonogramGenerator.MAX_FILLS,
            )
            assertTrue("$level puzzle not deducible", NonogramRules.isDeducible(puzzle))
        }
    }

    @Test
    fun cluesCoverEveryLine() {
        val puzzle = requireNotNull(NonogramGenerator.generate("Moderate", seed = 7L))
        val clues = NonogramRules.deriveClues(puzzle)
        assertEquals(NonogramRules.SIZE, clues.rows.size)
        assertEquals(NonogramRules.SIZE, clues.cols.size)
        val rowFills = clues.rows.sumOf { it.sum() }
        val colFills = clues.cols.sumOf { it.sum() }
        val fills = puzzle.count { it == 1 }
        assertEquals(fills, rowFills)
        assertEquals(fills, colFills)
    }

    /**
     * Roadmap §3 admission: the default difficulty generates in ≤ 2 s, slowest
     * of 10 runs, on the reference machine. The acceptance loop is included —
     * this is the whole cost the player waits for.
     */
    @Test
    fun defaultSlowestOfTenWithinTwoSeconds() {
        val elapsed = (0 until 10).map { run ->
            val t0 = System.nanoTime()
            val puzzle = NonogramGenerator.generate(
                NonogramGenerator.DEFAULT_DIFFICULTY,
                seed = 5000L + run,
            )
            val elapsedMs = (System.nanoTime() - t0) / 1_000_000
            println("run=$run elapsed=${elapsedMs}ms fills=${puzzle?.count { it == 1 }}")
            assertNotNull("run $run produced no puzzle", puzzle)
            elapsedMs
        }
        val slowest = elapsed.max()
        println("slowestOf10=${slowest}ms")
        assertTrue("slowest default generation ${slowest}ms exceeds the 2000ms budget", slowest < 2_000)
    }
}
