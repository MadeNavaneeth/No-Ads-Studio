package com.example.lightapp.games.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules for `wordsearch(12)`. Pure JVM tests, milliseconds each. The diagonal cases
 * are the load-bearing ones: a line that bends is not a word, but a long diagonal
 * that runs straight absolutely is — a regression here would make half the
 * generator's placements unfindable while the board still looks perfectly normal.
 */
class WordsearchRulesTest {

    private fun blankLetters() = List(WordsearchRules.CELLS) { ('A' + it % 26) }

    @Test
    fun `line walks straight and stops at the edge`() {
        assertEquals(listOf(0, 1, 2), WordsearchRules.line(0, 0, 1, 3))
        assertEquals(listOf(0, 13, 26), WordsearchRules.line(0, 1, 1, 3))
        // North from the top row leaves the board.
        assertEquals(null, WordsearchRules.line(0, -1, 0, 2))
        // West from column 0 wraps? No — it must refuse.
        assertEquals(null, WordsearchRules.line(12, 0, -1, 2))
    }

    @Test
    fun `spell reads letters in walk order`() {
        val letters = blankLetters().toMutableList()
        // Spell "TEST" east from 0: cells 0,1,2,3.
        for ((i, cell) in listOf(0, 1, 2, 3).withIndex()) letters[cell] = "TEST"[i]
        assertEquals(
            "TEST",
            WordsearchRules.spell(letters, listOf(0, 1, 2, 3)),
        )
    }

    @Test
    fun `evaluate matches an eastward word from either end`() {
        val cells = listOf(0, 1, 2, 3)
        val placement = Placement(word = "TEST", cells = cells, reversed = false)

        assertEquals(listOf(placement), WordsearchRules.evaluate(listOf(placement), 0, 3))
        // Traced last-letter-first is the same word.
        assertEquals(listOf(placement), WordsearchRules.evaluate(listOf(placement), 3, 0))
    }

    @Test
    fun `evaluate matches a long diagonal`() {
        // The regression: |dr|>1 && |dc|>1 must stay legal for diagonals, and the
        // straightness test below is what actually rejects bends.
        val cells = listOf(0, 13, 26, 39)
        val placement = Placement(word = "DIAG", cells = cells, reversed = false)

        assertEquals(listOf(placement), WordsearchRules.evaluate(listOf(placement), 0, 39))
        assertEquals(listOf(placement), WordsearchRules.evaluate(listOf(placement), 39, 0))
    }

    @Test
    fun `evaluate refuses bends and single cells`() {
        val placement = Placement(word = "TEST", cells = listOf(0, 1, 2, 3), reversed = false)

        // A knight-shaped drag: neither axis-flat nor a perfect diagonal.
        assertTrue(WordsearchRules.evaluate(listOf(placement), 0, 14).isEmpty())
        // Two-down-one-over: same refusal.
        assertTrue(WordsearchRules.evaluate(listOf(placement), 0, 25).isEmpty())
        // A tap is not a drag.
        assertTrue(WordsearchRules.evaluate(listOf(placement), 5, 5).isEmpty())
    }

    @Test
    fun `evaluate finds two words sharing one line`() {
        val shared = listOf(0, 1, 2, 3)
        val first = Placement(word = "POST", cells = shared, reversed = false)
        val second = Placement(word = "TSOP", cells = shared.asReversed(), reversed = false)

        assertEquals(
            listOf(first, second),
            WordsearchRules.evaluate(listOf(first, second), 0, 3),
        )
    }

    @Test
    fun `applyFound lights every cell of every line`() {
        val cells = List(WordsearchRules.CELLS) { WordsearchRules.HIDDEN }
        val lit = WordsearchRules.applyFound(cells, listOf(listOf(0, 1), listOf(1, 2)))

        assertEquals(WordsearchRules.FOUND, lit[0])
        assertEquals(WordsearchRules.FOUND, lit[1])
        assertEquals(WordsearchRules.FOUND, lit[2])
        assertEquals(WordsearchRules.HIDDEN, lit[3])
    }

    @Test
    fun `win requires every placement found`() {
        val first = Placement(word = "POST", cells = listOf(0, 1, 2), reversed = false)
        val second = Placement(word = "REST", cells = listOf(4, 5, 6), reversed = false)

        val partly = List(WordsearchRules.CELLS) { WordsearchRules.HIDDEN }
            .let { WordsearchRules.applyFound(it, listOf(listOf(0, 1, 2))) }
        assertEquals(false, WordsearchRules.isWin(partly, listOf(first, second)))

        val all = WordsearchRules.applyFound(partly, listOf(listOf(4, 5, 6)))
        assertEquals(true, WordsearchRules.isWin(all, listOf(first, second)))
    }

    @Test
    fun `completion is words found over words placed`() {
        val placements = listOf(
            Placement(word = "POST", cells = listOf(0, 1, 2), reversed = false),
            Placement(word = "REST", cells = listOf(4, 5, 6), reversed = false),
        )
        assertEquals(0.5f, WordsearchRules.completion(placements, setOf("POST")), 0.001f)
        assertEquals(0f, WordsearchRules.completion(emptyList(), emptySet()), 0.001f)
    }
}
