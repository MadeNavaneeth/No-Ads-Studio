package com.example.lightapp.games.wordsearch

import com.example.lightapp.studio.persistence.WordsearchCodec
import com.example.lightapp.studio.persistence.WordsearchSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Restore validation and the codec round-trip. Restore's job: accept exactly the
 * boards legal play can produce, reject everything else to "start fresh". The one
 * check that makes it a validation rather than a formality is spelling — a saved
 * placement triple must still spell its word on the saved letter grid.
 */
class WordsearchRestoreTest {

    private fun session(board: Board, difficulty: String = "Moderate"): WordsearchSession =
        WordsearchState.fresh(board, difficulty).toSession()

    @Test
    fun `a fresh generated board restores`() {
        val board = requireNotNull(WordsearchGenerator.generate("Moderate", seed = 11))
        val state = requireNotNull(WordsearchRestore.stateOrNull(session(board)))

        assertEquals(board.letters, state.letters)
        assertEquals("Moderate", state.difficulty)
        // Geometry survives exactly; the `reversed` flag does not (a stored triple
        // re-derives it as false) — so compare words and cells, not whole records.
        assertEquals(
            board.placements.map { it.word },
            state.placements.map { it.word },
        )
        assertEquals(
            board.placements.map { it.cells },
            state.placements.map { it.cells },
        )
    }

    @Test
    fun `unknown difficulty is rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 2))
        assertNull(WordsearchRestore.stateOrNull(session(board, difficulty = "Impossible")))
    }

    @Test
    fun `negative clock is rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 2))
        val bad = session(board).copy(elapsedMs = -1L)
        assertNull(WordsearchRestore.stateOrNull(bad))
    }

    @Test
    fun `empty placement list is rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 2))
        val bad = session(board).copy(placements = emptyList())
        assertNull(WordsearchRestore.stateOrNull(bad))
    }

    @Test
    fun `a placement that does not spell on the grid is rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 2))
        val good = session(board)
        val forged = good.placements.first().split(":").toMutableList()
        // Same shape, wrong word: the triple no longer matches the letters.
        forged[0] = "ZZZZZZ"
        val bad = good.copy(placements = listOf(forged.joinToString(":")) + good.placements.drop(1))
        assertNull(WordsearchRestore.stateOrNull(bad))
    }

    @Test
    fun `duplicate words are rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 2))
        val good = session(board)
        val duplicated = good.placements.toList()
        val bad = good.copy(placements = duplicated + duplicated.first())
        assertNull(WordsearchRestore.stateOrNull(bad))
    }

    @Test
    fun `a complete grid is not an in-progress game`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 5))
        val found = board.placements.fold(
            List(WordsearchRules.CELLS) { WordsearchRules.HIDDEN },
        ) { acc, placement -> WordsearchRules.applyFound(acc, listOf(placement.cells)) }
        val bad = session(board).copy(found = found)
        assertNull(WordsearchRestore.stateOrNull(bad))
    }

    @Test
    fun `a backward-stored word restores as its first-letter-first placement`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 9))
        val placement = board.placements.first()
        // Store the triple last-letter-first: start and end swapped, so the walk
        // runs against the word's reading order — exactly what the encoder may
        // legitimately be handed if a future save path forgets to normalise.
        val backward = "${placement.word}:${placement.cells.last()}:${placement.cells.first()}"
        val other = requireNotNull(WordsearchGenerator.generate("Simple", seed = 9)).placements.drop(1)
        val restored = WordsearchRestore.stateOrNull(
            session(board).copy(
                placements = listOf(backward) + other.map {
                    "${it.word}:${it.cells.first()}:${it.cells.last()}"
                },
            ),
        )
        val decoded = requireNotNull(restored).placements.first { it.word == placement.word }
        assertEquals(placement.cells, decoded.cells)
    }

    @Test
    fun `malformed triples are rejected`() {
        val board = requireNotNull(WordsearchGenerator.generate("Simple", seed = 2))
        val good = session(board)
        assertNull(
            WordsearchRestore.stateOrNull(
                good.copy(placements = listOf("NO-SEPARATORS") + good.placements.drop(1)),
            ),
        )
        assertNull(
            WordsearchRestore.stateOrNull(
                good.copy(placements = listOf("WORD:999:999") + good.placements.drop(1)),
            ),
        )
    }

    // ── codec round-trips ─────────────────────────────────────────────────────

    @Test
    fun `letters survive the codec`() {
        val letters = List(WordsearchSession.CELLS) { 'A' + (it % 26) }.map { it.code }
        val encoded = WordsearchCodec.encodeLetters(letters)
        assertEquals(WordsearchSession.CELLS, encoded.length)
        assertEquals(letters, WordsearchCodec.decodeLetters(encoded))
    }

    @Test
    fun `letter decode rejects wrong shape`() {
        assertNull(WordsearchCodec.decodeLetters(null))
        assertNull(WordsearchCodec.decodeLetters("A"))
        assertNull(WordsearchCodec.decodeLetters("a".repeat(WordsearchSession.CELLS)))
    }

    @Test
    fun `found cells survive the codec`() {
        val found = List(WordsearchSession.CELLS) { it % 2 }
        assertEquals(found, WordsearchCodec.decodeFound(WordsearchCodec.encodeFound(found)))
        assertNull(WordsearchCodec.decodeFound("012"))
    }

    @Test
    fun `placements survive the codec`() {
        val placements = listOf("POST:0:3", "REST:26:38")
        val encoded = WordsearchCodec.encodePlacements(placements)
        assertEquals(placements, WordsearchCodec.decodePlacements(encoded))
        assertEquals(emptyList<String>(), WordsearchCodec.decodePlacements(""))
        assertTrue(WordsearchCodec.decodePlacements(null) == null)
    }
}
