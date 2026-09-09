package com.example.lightapp.games.connect

import com.example.lightapp.studio.persistence.ConnectCodec
import com.example.lightapp.studio.persistence.ConnectSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Restore validation and the codec round-trip. Restore's job: accept exactly the
 * boards legal play can produce, reject everything else to "start fresh".
 */
class ConnectRestoreTest {

    private fun session(
        board: List<Int>,
        difficulty: String = "Moderate",
        elapsedMs: Long = 65_000L,
    ) = ConnectSession(
        board = board,
        difficulty = difficulty,
        elapsedMs = elapsedMs,
        mistakes = 0,
        progress = ConnectRules.completion(board),
    )

    @Test
    fun `a legal in-progress board restores`() {
        // The solved board minus a segment: pair two's route incomplete, board legal.
        val midPlay = solvedBoard().toMutableList()
        midPlay[24] = ConnectRules.EMPTY // break one interior segment off the snake
        // Breaking the snake orphans its tail — illegal-play. Instead: a half-laid
        // pair on an otherwise empty board.
        val legal = List(ConnectRules.CELLS) { ConnectRules.EMPTY }.toMutableList()
        legal[10] = ConnectRules.endpointOf(1)
        legal[12] = ConnectRules.endpointOf(1)
        legal[11] = ConnectRules.pathOf(1)

        val state = ConnectRestore.stateOrNull(session(legal))
        assertTrue(state != null)
        assertEquals("Moderate", requireNotNull(state).difficulty)
    }

    @Test
    fun `an orphaned segment is rejected`() {
        // A segment with no route to its pair — corruption, not play.
        val orphaned = List(ConnectRules.CELLS) { ConnectRules.EMPTY }.toMutableList()
        orphaned[10] = ConnectRules.endpointOf(1)
        orphaned[12] = ConnectRules.endpointOf(1)
        orphaned[40] = ConnectRules.pathOf(1) // away from both endpoints

        assertNull(ConnectRestore.stateOrNull(session(orphaned)))
    }

    @Test
    fun `a solved board is not an in-progress game`() {
        assertNull(ConnectRestore.stateOrNull(session(solvedBoard())))
    }

    @Test
    fun `unknown difficulty and negative clock are rejected`() {
        val board = List(ConnectRules.CELLS) { ConnectRules.EMPTY }
        assertNull(ConnectRestore.stateOrNull(session(board, difficulty = "nope")))
        assertNull(ConnectRestore.stateOrNull(session(board, elapsedMs = -1L)))
    }

    @Test
    fun `codec round-trips and rejects malformed input`() {
        // Every value the encoding admits, all on one board.
        val board = List(ConnectRules.CELLS) { it % (ConnectRules.PATH_BASE + ConnectRules.PAIRS + 1) }
        assertEquals(board, ConnectCodec.decodeBoard(ConnectCodec.encodeBoard(board)))

        // A session-shaped round trip, to prove the game's own values survive.
        val legal = List(ConnectRules.CELLS) { ConnectRules.EMPTY }.toMutableList()
        legal[10] = ConnectRules.endpointOf(8)
        legal[11] = ConnectRules.pathOf(8)
        legal[12] = ConnectRules.endpointOf(8)
        val session = session(legal)
        val encoded = ConnectCodec.encodeBoard(session.board)
        assertEquals(session.board, ConnectCodec.decodeBoard(encoded))

        assertNull(ConnectCodec.decodeBoard(null))
        assertNull(ConnectCodec.decodeBoard("0101")) // wrong length
        assertNull(ConnectCodec.decodeBoard("z".repeat(ConnectSession.CELLS))) // bad digit
        assertNull(ConnectCodec.decodeBoard("h".repeat(ConnectSession.CELLS))) // past 'g'
    }

    @Test
    fun `codec encodes values above nine as letters`() {
        val board = List(ConnectRules.CELLS) { ConnectRules.PATH_BASE + 7 } // pair seven's segment = 15 → 'f'
        val encoded = ConnectCodec.encodeBoard(board)
        assertEquals(ConnectSession.CELLS, encoded.length)
        assertTrue(encoded.all { it == 'f' })
        assertEquals(board, ConnectCodec.decodeBoard(encoded))
    }
}
