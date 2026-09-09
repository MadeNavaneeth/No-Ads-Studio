package com.example.lightapp.games.nonogram

import com.example.lightapp.studio.persistence.NonogramSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramRestoreTest {

    private fun session(
        solution: List<Int> = requireNotNull(NonogramGenerator.generate("Moderate", seed = 11L)),
        cells: List<Int> = List(NonogramRules.CELLS) { NonogramRules.UNKNOWN },
        difficulty: String = "Moderate",
        elapsedMs: Long = 0L,
        mistakes: Int = 0,
    ) = NonogramSession(
        solution = solution,
        cells = cells,
        elapsedMs = elapsedMs,
        difficulty = difficulty,
        mistakes = mistakes,
        // Bound to the board, never a constant — see the G1 note in NonogramScreen.
        progress = NonogramRules.completion(cells, solution),
    )

    @Test
    fun wellFormedSessionRestores() {
        val restored = requireNotNull(NonogramRestore.stateOrNull(session()))
        assertEquals("Moderate", restored.difficulty)
    }

    @Test
    fun emptySolutionIsRejected() {
        assertNull(NonogramRestore.stateOrNull(session(solution = List(NonogramRules.CELLS) { 0 })))
    }

    @Test
    fun unknownDifficultyIsRejected() {
        assertNull(NonogramRestore.stateOrNull(session(difficulty = "Extreme")))
    }

    @Test
    fun negativeClockIsRejected() {
        assertNull(NonogramRestore.stateOrNull(session(elapsedMs = -1L)))
    }

    @Test
    fun completedBoardRestoresForTheViewModelToReplace() {
        val solution = requireNotNull(NonogramGenerator.generate("Moderate", seed = 11L))
        val cells = solution.map { if (it == 1) NonogramRules.FILLED else NonogramRules.UNKNOWN }
        val restored = requireNotNull(NonogramRestore.stateOrNull(session(solution = solution, cells = cells)))
        // The view model clears a completed restore and generates; restore itself
        // stays total and reports what the save holds.
        assertEquals(true, restored.isComplete)
    }
}
