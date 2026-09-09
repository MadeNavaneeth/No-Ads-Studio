package com.example.lightapp.games.akari

import com.example.lightapp.studio.persistence.AkariCodec
import com.example.lightapp.studio.persistence.AkariSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Restore validation and the codec round-trip for `akari(10)`. Restore's job:
 * accept exactly the boards legal play can produce, reject everything else to
 * "start fresh" — a clue attached to ground is corruption, not difficulty.
 */
class AkariRestoreTest {

    private val puzzle = requireNotNull(AkariGenerator.generate("Moderate", seed = 3L))

    @Test
    fun `a generated puzzle round-trips through the production path`() {
        val session = AkariState(cells = puzzle, difficulty = "Moderate").toSession()
        val restored = requireNotNull(AkariRestore.stateOrNull(session))
        assertEquals(puzzle, restored.cells)
        assertEquals("Moderate", restored.difficulty)
    }

    @Test
    fun `a session with bulbs restores`() {
        // A tap on a white cell of the real puzzle — legal play by construction.
        val ground = puzzle.indexOfFirst { it == AkariRules.EMPTY }
        val played = AkariState(
            cells = puzzle.toMutableList().also { it[ground] = AkariRules.BULB },
            difficulty = "Moderate",
        )
        assertNotNull(AkariRestore.stateOrNull(played.toSession()))
    }

    @Test
    fun `an unknown label is rejected`() {
        val session = AkariState(cells = puzzle, difficulty = "Fortnightly").toSession()
        assertNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `the Daily label is accepted`() {
        val session = AkariState(cells = puzzle, difficulty = AkariViewModel.DAILY_NAME).toSession()
        assertNotNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `a clue on ground is corruption, not a puzzle`() {
        // Rewrite one white cell of the puzzle into a clue while keeping the mask
        // honest — the mask now disagrees with the board, and restore must notice.
        val corrupted = puzzle.toMutableList()
        val ground = corrupted.indexOfFirst { it == AkariRules.EMPTY }
        corrupted[ground] = AkariRules.CLUE_2
        val state = AkariState(cells = corrupted, difficulty = "Moderate")
        assertNull(AkariRestore.stateOrNull(state.toSession()))
    }

    @Test
    fun `a bulb inside a wall is rejected`() {
        val corrupted = puzzle.toMutableList()
        val wall = corrupted.indexOfFirst { it == AkariRules.WALL }
        corrupted[wall] = AkariRules.BULB
        val state = AkariState(cells = corrupted, difficulty = "Moderate")
        assertNull(AkariRestore.stateOrNull(state.toSession()))
    }

    @Test
    fun `a finished board is not restored as in-progress`() {
        // Solve the puzzle for real: run the counter's own branch — no. Simpler and
        // honest: a solved board is any board the win predicate accepts; construct
        // the generator's own solution by placing bulbs is expensive, so instead
        // assert the semantic rule on a small solved board the rules test already
        // proves: restore of a complete board must be null regardless of label.
        val solved = AkariRules.run {
            val cells = List(CELLS) { EMPTY }.toMutableList()
            // An empty board with one bulb lighting everything: impossible on 10×10
            // without walls — so use the cross board from the rules suite's shape.
            for (i in 0 until CELLS) {
                val r = rowOf(i); val c = colOf(i)
                if (r == 3 || r == 7 || c == 3 || c == 7) cells[i] = WALL
            }
            for (b in listOf(0, 8, 11, 19, 22, 40, 51, 62, 88, 99)) cells[b] = BULB
            cells.toList()
        }
        assertTrue(AkariRules.isWin(solved))
        val session = AkariState(cells = solved, difficulty = "Moderate").toSession()
        assertNull(AkariRestore.stateOrNull(session))
    }

    // ─── codec ────────────────────────────────────────────────────────────────

    @Test
    fun `the codec round-trips the board and the mask`() {
        assertEquals(puzzle, AkariCodec.decodeCells(AkariCodec.encodeCells(puzzle)))
        val mask = requireNotNull(AkariCodec.decodeWalls(AkariCodec.encodeWalls(puzzle)))
        assertEquals(puzzle.map { it == AkariRules.WALL }, mask)
    }

    @Test
    fun `malformed codec input decodes to null, never a crash`() {
        assertNull(AkariCodec.decodeCells(null))
        assertNull(AkariCodec.decodeCells("0101")) // wrong length
        assertNull(AkariCodec.decodeCells("x".repeat(AkariSession.CELLS))) // out of vocabulary
        assertNull(AkariCodec.decodeWalls("2".repeat(AkariSession.CELLS))) // not 0/1
    }
}
