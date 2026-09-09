package com.example.lightapp.games.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's contract, asserted rather than assumed (roadmap §3: measured, not
 * assumed). First-tap safety is the load-bearing guarantee, and it is structural —
 * these tests prove it holds for corners, edges, centres, and all three difficulty
 * levels, on the JVM, in milliseconds.
 */
class MinesweeperGeneratorTest {

    @Test
    fun `every mine count matches its difficulty`() {
        assertEquals(8, MinesweeperGenerator.mineCountFor("Simple"))
        assertEquals(10, MinesweeperGenerator.mineCountFor("Moderate"))
        assertEquals(13, MinesweeperGenerator.mineCountFor("Hard"))
        assertEquals(10, MinesweeperGenerator.mineCountFor("anything else"))
    }

    @Test
    fun `first tap on the corner never lands on a mine and opens at worst a number`() {
        for (difficulty in MinesweeperGenerator.DIFFICULTIES) {
            val mines = requireNotNull(MinesweeperGenerator.layoutFor(0, difficulty, seed = 1)) { "unexpected null" }
            assertFalse("corner mined at $difficulty", mines[0])
            // The tap itself must be safe; the neighbourhood exclusion means the
            // corner's three neighbours are safe too whenever the board can afford it.
            assertEquals(0, MinesweeperRules.adjacencyOf(mines, 0))
        }
    }

    @Test
    fun `first tap on the centre never lands on a mine`() {
        val centre = 40
        for (difficulty in MinesweeperGenerator.DIFFICULTIES) {
            val mines = requireNotNull(MinesweeperGenerator.layoutFor(centre, difficulty, seed = 2)) { "unexpected null" }
            assertFalse("centre mined at $difficulty", mines[centre])
        }
    }

    @Test
    fun `layout is deterministic per seed`() {
        val a = MinesweeperGenerator.layoutFor(40, "Moderate", seed = 7)
        val b = MinesweeperGenerator.layoutFor(40, "Moderate", seed = 7)
        assertEquals(a, b)
    }

    @Test
    fun `every difficulty places exactly its mine count`() {
        for (difficulty in MinesweeperGenerator.DIFFICULTIES) {
            val mines = requireNotNull(MinesweeperGenerator.layoutFor(4, difficulty, seed = 3)) { "unexpected null" }
            assertEquals(MinesweeperGenerator.mineCountFor(difficulty), mines.count { it })
        }
    }

    @Test
    fun `the exclusion always fits the board at this project's mine counts`() {
        for (difficulty in MinesweeperGenerator.DIFFICULTIES) {
            for (tap in listOf(0, 4, 40, 80)) {
                assertNotNull("layout for $difficulty at tap $tap", MinesweeperGenerator.layoutFor(tap, difficulty, seed = 4))
            }
        }
    }

    @Test
    fun `the tap and its neighbourhood are excluded whenever the board can afford it`() {
        // At every shipped mine count the board can afford the full exclusion
        // (81 - 13 = 68 open cells, a neighbourhood is at most 9), so the tap's
        // entire neighbourhood must be mine-free — that is what makes the opening
        // read as a zero region or a single number, never a surprise.
        for (difficulty in MinesweeperGenerator.DIFFICULTIES) {
            for (tap in listOf(0, 4, 40, 80)) {
                val mines = requireNotNull(MinesweeperGenerator.layoutFor(tap, difficulty, seed = 6)) { "unexpected null" }
                for (n in MinesweeperRules.neighbours(tap)) {
                    assertFalse("mine at $n next to tap $tap at $difficulty", mines[n])
                }
            }
        }
    }
}
