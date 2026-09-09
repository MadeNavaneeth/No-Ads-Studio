package com.example.lightapp.sudoku.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The determinism contract the local daily puzzle rests on.
 *
 * The daily puzzle seeds a fresh [QQWing] with the UTC day number and expects the same
 * board every time — this device, any device, any day. That only holds if the random
 * source is the board's own (it used to live in the companion object, shared by every
 * instance in the process, so two instances interleaving draws made a fixed seed
 * meaningless) and if the seed fully determines the output. These tests lock both.
 */
class QQWingSeedTest {

    /** Generates one unique puzzle from a fresh board seeded with [seed]. */
    private fun puzzleFor(seed: Int): IntArray {
        val qq = QQWing(GameType.Default9x9, GameDifficulty.Unspecified)
        qq.setRandom(seed)
        check(qq.generatePuzzle()) { "generation failed for seed $seed" }
        check(qq.hasUniqueSolution()) { "seeded generation $seed did not produce a unique solution" }
        return qq.puzzle.clone()
    }

    @Test
    fun sameSeedProducesTheSamePuzzle() {
        assertArrayEquals(puzzleFor(20260903), puzzleFor(20260903))
    }

    @Test
    fun differentSeedProducesADifferentPuzzle() {
        assertNotEquals(puzzleFor(20260903).toList(), puzzleFor(20260904).toList())
    }

    @Test
    fun seededPuzzleHasGivensInTheRealisticRange() {
        val puzzle = puzzleFor(20260903)
        val givenCount = puzzle.count { it != 0 }
        // 17 is the proven minimum for a unique 9×9; a full grid is a printout, not a puzzle.
        assertTrue("expected 17..80 givens, got $givenCount", givenCount in 17..80)
    }
}
