package com.example.lightapp.games.binairo

import com.example.lightapp.studio.persistence.BinairoCodec
import com.example.lightapp.studio.persistence.BinairoSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Restore validation and the codec round-trip for `binairo(10)`. Restore's job:
 * accept exactly the boards legal play can produce, reject everything else to
 * "start fresh" — a player entry that overwrites a given is corruption, not a move
 * (rule M8: there is no code path by which that can happen, so a save describing
 * one is a save that never shipped).
 *
 * The fixture is generated once per class. Binairo's generation is cheap — the
 * deduction proof is polynomial, unlike akari's exponential solution counter — so
 * the suite can afford a provenance-real board without akari's hand-built fixture
 * discipline.
 */
class BinairoRestoreTest {

    companion object {
        private val puzzle = requireNotNull(BinairoGenerator.generate("Moderate", seed = 7L)) {
            "the fixture seed must generate"
        }
    }

    /**
     * A complete valid grid, hand-built — restore has no solver, and a finished
     * board must be rejected without one. Construction: every row is a cyclic
     * shift of a balanced, run-free, aperiodic base pattern, which makes every
     * column a cyclic shift too — all four Takuzu constraints by construction.
     */
    private fun solvedBoard(): List<Int> {
        val base = listOf(0, 1, 1, 0, 1, 0, 0, 1, 1, 0)
        return List(BinairoRules.CELLS) { i ->
            val bit = base[(BinairoRules.rowOf(i) + BinairoRules.colOf(i)) % BinairoRules.SIZE]
            if (bit == 1) BinairoRules.ONE else BinairoRules.ZERO
        }
    }

    @Test
    fun `a realistic puzzle round-trips through the production path`() {
        val session = BinairoState.start(puzzle, "Moderate").toSession()
        val restored = requireNotNull(BinairoRestore.stateOrNull(session))
        assertEquals(puzzle, restored.givens)
        assertEquals("Moderate", restored.difficulty)
        assertTrue("a start state has no player entries yet", restored.entries.all { it == BinairoRules.EMPTY })
    }

    @Test
    fun `a session with player entries restores`() {
        val ground = puzzle.indexOfFirst { it == BinairoRules.EMPTY }
        val entries = List(BinairoRules.CELLS) { BinairoRules.EMPTY }.toMutableList()
        entries[ground] = BinairoRules.ONE
        val state = BinairoState(
            givens = puzzle,
            entries = entries,
            difficulty = "Moderate",
        )
        val restored = requireNotNull(BinairoRestore.stateOrNull(state.toSession()))
        assertEquals(BinairoRules.ONE, restored.entries[ground])
    }

    @Test
    fun `an unknown label is rejected`() {
        val session = BinairoState.start(puzzle, "Fortnightly").toSession()
        assertNull(BinairoRestore.stateOrNull(session))
    }

    @Test
    fun `a negative clock is rejected`() {
        val session = BinairoState.start(puzzle, "Moderate")
            .toSession()
            .copy(elapsedMs = -1L)
        assertNull(BinairoRestore.stateOrNull(session))
    }

    @Test
    fun `a player entry overwriting a given is rejected`() {
        // The corruption model: the merged board is damaged, the given mask is not.
        // Mask says one value, the board says the opposite — rule M8 says that
        // save never shipped.
        val given = puzzle.indexOfFirst { it != BinairoRules.EMPTY }
        val flipped = if (puzzle[given] == BinairoRules.ONE) BinairoRules.ZERO else BinairoRules.ONE
        val corrupted = puzzle.mapIndexed { i, v -> if (i == given) flipped else v }
        val session = BinairoSession(
            cells = BinairoCodec.encodeCells(corrupted),
            givens = BinairoCodec.encodeGivens(puzzle),
            difficulty = "Moderate",
            elapsedMs = 0L,
            mistakes = 0,
            progress = 0f,
        )
        assertNull(BinairoRestore.stateOrNull(session))
    }

    @Test
    fun `a given erased from the board is rejected`() {
        val given = puzzle.indexOfFirst { it != BinairoRules.EMPTY }
        val corrupted = puzzle.mapIndexed { i, v -> if (i == given) BinairoRules.EMPTY else v }
        val session = BinairoSession(
            cells = BinairoCodec.encodeCells(corrupted),
            givens = BinairoCodec.encodeGivens(puzzle),
            difficulty = "Moderate",
            elapsedMs = 0L,
            mistakes = 0,
            progress = 0f,
        )
        assertNull(BinairoRestore.stateOrNull(session))
    }

    @Test
    fun `a finished board is not restored as in-progress`() {
        val solved = solvedBoard()
        assertTrue("the hand-built board must be a real win", BinairoRules.isWin(solved))
        val session = BinairoSession(
            cells = BinairoCodec.encodeCells(solved),
            givens = BinairoCodec.encodeGivens(List(BinairoRules.CELLS) { BinairoRules.EMPTY }),
            difficulty = "Moderate",
            elapsedMs = 0L,
            mistakes = 0,
            progress = 1f,
        )
        assertNull(BinairoRestore.stateOrNull(session))
    }

    // ─── codec ────────────────────────────────────────────────────────────────

    @Test
    fun `the codec round-trips the board and the given mask`() {
        val state = BinairoState.start(puzzle, "Moderate")
        val session = state.toSession()
        assertEquals(state.merged, BinairoCodec.decodeCells(session.cells))
        assertEquals(state.givens, BinairoCodec.decodeGivens(session.givens))
    }

    @Test
    fun `malformed codec input decodes to null, never a crash`() {
        assertNull(BinairoCodec.decodeCells(null))
        assertNull(BinairoCodec.decodeCells("0101")) // wrong length
        assertNull(BinairoCodec.decodeCells("3".repeat(BinairoSession.CELLS))) // out of vocabulary
        assertNull(BinairoCodec.decodeGivens("3".repeat(BinairoSession.CELLS)))
    }
}