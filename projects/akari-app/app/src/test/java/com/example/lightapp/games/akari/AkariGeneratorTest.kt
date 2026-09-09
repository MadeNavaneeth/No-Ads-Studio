package com.example.lightapp.games.akari

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's contract, asserted rather than assumed (roadmap §3). The
 * load-bearing guarantee is **uniqueness** — the sudoku admission bar — and these
 * tests run the generator's own bounded solution counter over its output to hold
 * that guarantee where it matters: on the boards it ships.
 */
class AkariGeneratorTest {

    @Test
    fun `a generated board has exactly one solution`() {
        val board = requireNotNull(AkariGenerator.generate("Moderate", seed = 1L)) {
            "the generator must produce a board"
        }
        assertEquals(1, AkariGenerator.countSolutions(board, cap = 2))
    }

    @Test
    fun `the same seed generates the same board`() {
        assertEquals(
            AkariGenerator.generate("Moderate", seed = 42L),
            AkariGenerator.generate("Moderate", seed = 42L),
        )
    }

    @Test
    fun `every difficulty generates a valid unique board`() {
        for (difficulty in AkariGenerator.DIFFICULTIES) {
            val board = requireNotNull(AkariGenerator.generate(difficulty, seed = 7L)) {
                "$difficulty must generate"
            }
            assertEquals(AkariRules.CELLS, board.size)
            assertEquals("countSolutions($difficulty) must be 1", 1, AkariGenerator.countSolutions(board, cap = 2))
        }
    }

    @Test
    fun `a generated start state is dark and its clues sit on walls`() {
        val board = requireNotNull(AkariGenerator.generate("Easy", seed = 9L))
        var clues = 0
        var walls = 0
        for (v in board) {
            assertTrue("no bulbs in a start state", v != AkariRules.BULB)
            if (AkariRules.clueNumber(v) != null) clues++
            if (v == AkariRules.WALL) walls++
        }
        assertTrue("an Easy board carries clues", clues > 0)
        assertTrue("an Easy board carries blank walls", walls > 0)
    }

    @Test
    fun `the solution count respects its cap`() {
        // A walled board with no clues at all still has astronomically many solutions
        // (on a fully empty board any permutation of one bulb per row/column covers
        // everything), so the counter must stop at the cap rather than enumerate the
        // space. A full wall column splits the board into two open halves whose short
        // runs keep the last-chance pruning effective, so the search terminates fast:
        // the assertion is about the cap, not the exact (huge) count.
        val board = List(AkariRules.CELLS) { i ->
            if (AkariRules.colOf(i) == 5) AkariRules.WALL else AkariRules.EMPTY
        }
        assertEquals(2, AkariGenerator.countSolutions(board, cap = 2))
    }

    @Test
    fun `clue exactness gates a leaf solution`() {
        // A corner CLUE_4 demands four adjacent bulbs but a corner touches two cells —
        // no leaf can ever be exact, so the count is exactly 0. Walled down to a 2x2
        // room so the search proves it instantly.
        val cells = List(AkariRules.CELLS) { AkariRules.WALL }.toMutableList().apply {
            this[0] = AkariRules.CLUE_4 // corner wall demanding four bulbs; corners touch two cells
            this[1] = AkariRules.EMPTY
            this[10] = AkariRules.EMPTY
        }
        assertEquals(0, AkariGenerator.countSolutions(cells, cap = 2))
    }
}
