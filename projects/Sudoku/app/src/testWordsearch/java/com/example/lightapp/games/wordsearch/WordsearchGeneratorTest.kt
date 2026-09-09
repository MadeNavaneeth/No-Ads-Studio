package com.example.lightapp.games.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's contract, asserted rather than assumed (roadmap §3: measured, not
 * assumed). The load-bearing guarantees: determinism per seed, a completely filled
 * grid, and every placement still spelling its word on the *final* grid — the last
 * one is what proves crossing words agree letter-for-letter on shared cells.
 */
class WordsearchGeneratorTest {

    @Test
    fun `same seed produces the same grid`() {
        val first = requireNotNull(WordsearchGenerator.generate("Moderate", seed = 7))
        val second = requireNotNull(WordsearchGenerator.generate("Moderate", seed = 7))

        assertEquals(first.letters, second.letters)
        assertEquals(first.placements, second.placements)
    }

    @Test
    fun `every cell is lettered`() {
        val board = requireNotNull(WordsearchGenerator.generate("Hard", seed = 3))

        assertEquals(WordsearchRules.CELLS, board.letters.size)
        assertTrue("filler must leave no blanks", board.letters.none { it == ' ' })
        assertTrue(board.letters.all { it in 'A'..'Z' })
    }

    @Test
    fun `every placement spells its word on the final grid`() {
        val board = requireNotNull(WordsearchGenerator.generate("Hard", seed = 3))

        val seen = HashSet<String>()
        for (placement in board.placements) {
            // Distinct words, all from the pool.
            assertTrue("words must be distinct", seen.add(placement.word))
            assertTrue(placement.word in WordList.words)

            // On-board and straight by construction.
            assertTrue(placement.cells.size == placement.word.length)
            assertTrue(placement.cells.all { it in 0 until WordsearchRules.CELLS })

            // The whole point of the legality rule: the word reads correctly off
            // the finished grid, crossings included. Placement cells always run
            // first-letter-first, so the forward spelling is the contract.
            assertEquals(placement.word, WordsearchRules.spell(board.letters, placement.cells))
        }
    }

    @Test
    fun `difficulty sets the word count`() {
        assertEquals(7, requireNotNull(WordsearchGenerator.generate("Simple", seed = 1)).placements.size)
        assertEquals(9, requireNotNull(WordsearchGenerator.generate("Moderate", seed = 1)).placements.size)
        assertEquals(12, requireNotNull(WordsearchGenerator.generate("Hard", seed = 1)).placements.size)
    }

    @Test
    fun `generation is fast enough to stay on the default dispatcher`() {
        // Not a stopwatch assertion — a tripwire. The scored-trials loop is
        // milliseconds per grid; if this ever starts taking seconds something in
        // the placement search went exponential and the timeout backstop is
        // about to become the user-visible behaviour.
        val start = System.nanoTime()
        repeat(20) { WordsearchGenerator.generate("Hard", seed = it.toLong()) }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("20 grids took ${elapsedMs}ms", elapsedMs < 5_000)
    }
}
