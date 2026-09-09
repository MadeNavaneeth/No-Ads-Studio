package com.example.lightapp.games.minesweeper

import com.example.lightapp.studio.persistence.MinesweeperCodec
import com.example.lightapp.studio.persistence.MinesweeperSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The restore gate is what stands between a malformed or stale save and a board the
 * player cannot win. Every case here is one a real device can produce: a save from an
 * older build, a truncated write, a finished game the completion handler had not yet
 * cleared.
 */
class MinesweeperRestoreTest {

    private fun session(
        difficulty: String = "Moderate",
        mineIndexes: List<Int> = (0 until 10).toList(),
        cells: List<Int>? = null,
        elapsedMs: Long = 30_000L,
        mistakes: Int = 0,
        progress: Float = 0.2f,
    ): MinesweeperSession {
        val mines = List(MinesweeperSession.CELLS) { if (it in mineIndexes) 1 else 0 }
        return MinesweeperSession(
            mines = mines,
            cells = cells ?: List(MinesweeperSession.CELLS) { MinesweeperRules.UNKNOWN },
            difficulty = difficulty,
            elapsedMs = elapsedMs,
            mistakes = mistakes,
            progress = progress,
        )
    }

    @Test
    fun `a valid in-progress board restores`() {
        val state = MinesweeperRestore.stateOrNull(session()) ?: throw AssertionError("null")
        assertEquals("Moderate", state.difficulty)
        assertEquals(30_000L, state.elapsedMs)
        assertEquals(10, state.mines.count { it })
    }

    @Test
    fun `an unknown difficulty is refused`() {
        assertNull(MinesweeperRestore.stateOrNull(session(difficulty = "Expert")))
    }

    @Test
    fun `a mine count that matches no difficulty is refused`() {
        assertNull(MinesweeperRestore.stateOrNull(session(mineIndexes = (0 until 4).toList())))
    }

    @Test
    fun `a negative clock or negative mistakes are refused`() {
        assertNull(MinesweeperRestore.stateOrNull(session(elapsedMs = -1L)))
        assertNull(MinesweeperRestore.stateOrNull(session(mistakes = -1)))
    }

    @Test
    fun `a completed board is not restored as in-progress`() {
        // Every safe cell revealed except the ten mines.
        val cells = List(MinesweeperSession.CELLS) { i ->
            if (i in 0 until 10) MinesweeperRules.UNKNOWN else MinesweeperRules.REVEALED
        }
        assertNull(MinesweeperRestore.stateOrNull(session(cells = cells)))
    }

    @Test
    fun `a detonated board is not restored as in-progress`() {
        val cells = List(MinesweeperSession.CELLS) { i ->
            if (i == 0) MinesweeperRules.DETONATED else MinesweeperRules.UNKNOWN
        }
        assertNull(MinesweeperRestore.stateOrNull(session(cells = cells)))
    }
}
