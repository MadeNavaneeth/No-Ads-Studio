package com.example.lightapp.games.akari

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.AkariCodec
import com.example.lightapp.studio.persistence.AkariSession

/**
 * A playable position for `akari(10)`.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * There is no `over` flag: the win is a pure predicate over the board
 * ([AkariRules.isWin]), so a state is "finished" exactly when the board satisfies it —
 * the same shape as sudoku and connect, and simpler than blockpuzzle's endless run.
 */
@Immutable
data class AkariState(
    val cells: List<Int>,
    val difficulty: String = AkariGenerator.DEFAULT_DIFFICULTY,
    val elapsedMs: Long = 0L,

    /**
     * The UTC day this run belongs to, when it is the day's deal (decision D32).
     * Rides on the state — never persisted inside the session — so persist and finish
     * route to the daily slot exactly when it is non-null.
     */
    val dailyDay: Long? = null,
) {
    /** The one quantity (method §2): lit white cells over all white cells. */
    val completion: Float get() = AkariRules.completion(cells)

    val isComplete: Boolean get() = AkariRules.isWin(cells)

    fun toSession() = AkariSession(
        cells = AkariCodec.encodeCells(cells),
        walls = AkariCodec.encodeWalls(cells),
        difficulty = difficulty,
        elapsedMs = elapsedMs,
        mistakes = 0,
        progress = completion,
    )
}
