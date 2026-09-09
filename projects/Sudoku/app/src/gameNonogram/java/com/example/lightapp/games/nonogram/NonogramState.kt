package com.example.lightapp.games.nonogram

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.NonogramSession

/**
 * A playable position.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * There are no givens here — every cell is the player's — so one tri-state list holds
 * the board (rule M8 has nothing to separate). The [solution] is the puzzle itself;
 * unlike `sudoku(9)` this game must hold it, because the win condition is an exact
 * match (parity §2). A wrong fill is still never shown (method §6): it is only
 * counted toward the optional lives limit.
 */
@Immutable
data class NonogramState(
    val solution: List<Int>,
    val cells: List<Int>,
    val difficulty: String,
    val selected: Int? = null,
    val elapsedMs: Long = 0L,
    val mistakes: Int = 0,

    /**
     * The UTC day this run belongs to, when it is the day's puzzle (decision D32).
     * Rides on the state — never persisted inside the session — so persist/finish
     * route to the daily slot exactly when a daily is being played.
     */
    val dailyDay: Long? = null,
) {
    /** Clues for the strips, derived from the solution. No bundled data. */
    val clues: NonogramClues = NonogramRules.deriveClues(solution)

    /** The LINE readout: solved rows + solved columns, of 20. */
    val solvedLines: Int = NonogramRules.solvedLineCount(cells, solution)

    val isComplete: Boolean = NonogramRules.isWin(cells, solution)

    /** The one quantity (method §2): correct fills over solution fills. */
    val completion: Float = NonogramRules.completion(cells, solution)

    fun toSession() = NonogramSession(
        solution = solution,
        cells = cells,
        elapsedMs = elapsedMs,
        difficulty = difficulty,
        mistakes = mistakes,
        progress = completion,
    )

    companion object {
        fun fresh(solution: List<Int>, difficulty: String, dailyDay: Long? = null) = NonogramState(
            solution = solution,
            cells = List(NonogramRules.CELLS) { NonogramRules.UNKNOWN },
            difficulty = difficulty,
            dailyDay = dailyDay,
        )
    }
}

/** One reversible change. In memory only — decision D17. */
data class NonoMove(
    val index: Int,
    val previousCell: Int,
    val previousMistakes: Int,
    val previousSelected: Int?,
) {
    companion object {
        const val MAX_HISTORY = 50
    }
}
