package com.example.lightapp.games.binairo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's observable contract for `binairo(10)`. The uniqueness proof is
 * the generator's own deduction solver — it ships no board it cannot re-derive by
 * forced logic — so these tests hold the contract at the seams a caller touches:
 determinism, the difficulty lever, and the start state's shape.
 */
class BinairoGeneratorTest {

    private fun givensCount(board: List<Int>) = board.count { it != BinairoRules.EMPTY }

    @Test
    fun `every difficulty generates a start board`() {
        for (difficulty in BinairoGenerator.DIFFICULTIES) {
            val board = requireNotNull(BinairoGenerator.generate(difficulty, seed = 11L)) {
                "$difficulty must generate within the attempt budget"
            }
            assertEquals(BinairoRules.CELLS, board.size)
            // A start board holds givens and undecided cells — nothing else.
            assertTrue(
                "$difficulty ships a foreign value",
                board.all { it == BinairoRules.EMPTY || it == BinairoRules.ONE || it == BinairoRules.ZERO },
            )
            assertTrue("$difficulty ships no givens", givensCount(board) > 0)
        }
    }

    @Test
    fun `the same seed generates the same board`() {
        assertEquals(
            BinairoGenerator.generate("Moderate", seed = 11L),
            BinairoGenerator.generate("Moderate", seed = 11L),
        )
    }

    @Test
    fun `a looser removal budget leaves more givens`() {
        // Same seed, same shuffled removal order — the budget only caps how far
        // the strip runs, so the kept-givens sets are nested by difficulty.
        val easy = givensCount(requireNotNull(BinairoGenerator.generate("Easy", seed = 11L)))
        val moderate = givensCount(requireNotNull(BinairoGenerator.generate("Moderate", seed = 11L)))
        val hard = givensCount(requireNotNull(BinairoGenerator.generate("Hard", seed = 11L)))
        assertTrue("Easy ($easy) must keep at least Moderate's ($moderate) givens", easy >= moderate)
        assertTrue("Moderate ($moderate) must keep at least Hard's ($hard) givens", moderate >= hard)
    }

    @Test
    fun `each difficulty stays within its removal budget`() {
        // Easy strips at most 40 of 100; Moderate at most 60. Hard strips greedily
        // and has no floor beyond deduction-solvability itself.
        val easy = givensCount(requireNotNull(BinairoGenerator.generate("Easy", seed = 11L)))
        val moderate = givensCount(requireNotNull(BinairoGenerator.generate("Moderate", seed = 11L)))
        assertTrue(easy >= BinairoRules.CELLS - 40)
        assertTrue(moderate >= BinairoRules.CELLS - 60)
    }

    @Test
    fun `an unknown difficulty falls back to the default budget`() {
        // The fallback is the Moderate budget with the same random stream — the
        // same board, deterministically, not a surprise.
        assertEquals(
            BinairoGenerator.generate(BinairoGenerator.DEFAULT_DIFFICULTY, seed = 11L),
            BinairoGenerator.generate("Absurd", seed = 11L),
        )
    }
}