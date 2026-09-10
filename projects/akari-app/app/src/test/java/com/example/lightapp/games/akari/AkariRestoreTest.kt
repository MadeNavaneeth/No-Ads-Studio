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
 *
 * The fixture is hand-built, not generated. Restore validates shape and label —
 * not provenance, by design — so the suite has no reason to pay the generator's
 * uniqueness-proof cost (which [AkariGeneratorTest] already pays where it is the
 * subject). The fixture is a board the generator could ship: the wall-cross
 * layout with numbered clues on some walls, dark before the first bulb.
 */
class AkariRestoreTest {

    companion object {
        /** Wall cross at rows and columns 3 and 7, six walls carrying clue numbers. */
        private val startBoard: List<Int> = run {
            val cells = List(AkariRules.CELLS) { i ->
                val r = AkariRules.rowOf(i)
                val c = AkariRules.colOf(i)
                if (r == 3 || r == 7 || c == 3 || c == 7) AkariRules.WALL else AkariRules.EMPTY
            }.toMutableList()
            cells[7] = AkariRules.CLUE_1
            cells[33] = AkariRules.CLUE_2
            cells[47] = AkariRules.CLUE_0
            cells[74] = AkariRules.CLUE_1
            cells[87] = AkariRules.CLUE_2
            cells[97] = AkariRules.CLUE_0
            cells.toList()
        }
    }

    @Test
    fun `a realistic puzzle round-trips through the production path`() {
        val session = AkariState(cells = startBoard, difficulty = "Moderate").toSession()
        val restored = requireNotNull(AkariRestore.stateOrNull(session))
        assertEquals(startBoard, restored.cells)
        assertEquals("Moderate", restored.difficulty)
    }

    @Test
    fun `a session with bulbs restores`() {
        // A tap on a white cell of the fixture — legal play by construction.
        val ground = startBoard.indexOfFirst { it == AkariRules.EMPTY }
        val played = AkariState(
            cells = startBoard.toMutableList().also { it[ground] = AkariRules.BULB },
            difficulty = "Moderate",
        )
        assertNotNull(AkariRestore.stateOrNull(played.toSession()))
    }

    @Test
    fun `an unknown label is rejected`() {
        val session = AkariState(cells = startBoard, difficulty = "Fortnightly").toSession()
        assertNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `the Daily label is accepted`() {
        val session = AkariState(cells = startBoard, difficulty = AkariViewModel.DAILY_NAME).toSession()
        assertNotNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `a clue on ground is corruption, not a puzzle`() {
        // A corrupted save flips one white cell into a clue while the saved mask
        // still says ground — the mask (written at save time from the honest board)
        // now disagrees with the board, and restore must notice.
        val corrupted = startBoard.toMutableList()
        val ground = corrupted.indexOfFirst { it == AkariRules.EMPTY }
        corrupted[ground] = AkariRules.CLUE_2
        val session = AkariSession(
            cells = AkariCodec.encodeCells(corrupted),
            walls = AkariCodec.encodeWalls(startBoard),
            difficulty = "Moderate",
            elapsedMs = 0L,
            mistakes = 0,
            progress = 0f,
        )
        assertNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `a bulb inside a wall is rejected`() {
        // Same corruption model: the board is damaged, the saved mask is not.
        val corrupted = startBoard.toMutableList()
        val wall = corrupted.indexOfFirst { it == AkariRules.WALL }
        corrupted[wall] = AkariRules.BULB
        val session = AkariSession(
            cells = AkariCodec.encodeCells(corrupted),
            walls = AkariCodec.encodeWalls(startBoard),
            difficulty = "Moderate",
            elapsedMs = 0L,
            mistakes = 0,
            progress = 0f,
        )
        assertNull(AkariRestore.stateOrNull(session))
    }

    @Test
    fun `a finished board is not restored as in-progress`() {
        // Solve the fixture's layout for real: the wall cross cuts the board into
        // nine regions, each covered by bulbs on its local diagonal (the 3×2 and
        // 2×3 regions use the paired pattern; the 2×2 needs both diagonals).
        // Restore of a complete board must be null regardless of label.
        val solved = AkariRules.run {
            val cells = List(CELLS) { EMPTY }.toMutableList()
            for (i in 0 until CELLS) {
                val r = rowOf(i); val c = colOf(i)
                if (r == 3 || r == 7 || c == 3 || c == 7) cells[i] = WALL
            }
            for (b in listOf(0, 11, 22, 4, 15, 26, 8, 19, 40, 51, 62, 44, 55, 66, 48, 59, 80, 91, 84, 95, 88, 99)) cells[b] = BULB
            cells.toList()
        }
        assertTrue(AkariRules.isWin(solved))
        val session = AkariState(cells = solved, difficulty = "Moderate").toSession()
        assertNull(AkariRestore.stateOrNull(session))
    }

    // ─── codec ────────────────────────────────────────────────────────────────

    @Test
    fun `the codec round-trips the board and the mask`() {
        assertEquals(startBoard, AkariCodec.decodeCells(AkariCodec.encodeCells(startBoard)))
        val mask = requireNotNull(AkariCodec.decodeWalls(AkariCodec.encodeWalls(startBoard)))
        // The mask carries every wall kind — blank walls and numbered clues alike —
        // because restore reads it as the layout's authority cell by cell.
        assertEquals(
            startBoard.map { it == AkariRules.WALL || AkariRules.clueNumber(it) != null },
            mask,
        )
    }

    @Test
    fun `malformed codec input decodes to null, never a crash`() {
        assertNull(AkariCodec.decodeCells(null))
        assertNull(AkariCodec.decodeCells("0101")) // wrong length
        assertNull(AkariCodec.decodeCells("x".repeat(AkariSession.CELLS))) // out of vocabulary
        assertNull(AkariCodec.decodeWalls("2".repeat(AkariSession.CELLS))) // not 0/1
    }
}
