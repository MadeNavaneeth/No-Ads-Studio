package com.example.lightapp.games.blockpuzzle

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.BlockSession

/**
 * A playable position for `blockpuzzle(8)`.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * The [tray] holds at most three pieces; a placement removes its piece and, when the
 * tray empties, deals three more under the D33 contract against the *then-live* board.
 * [over] is terminal — there is no undo across it, and a finished run is never
 * restored as in-progress.
 */
@Immutable
data class BlockState(
    val cells: List<Int>,
    val tray: List<BlockRules.Piece>,
    val score: Int,
    val difficulty: String = BlockGenerator.DEFAULT_DIFFICULTY,
    val over: Boolean = false,
    val elapsedMs: Long = 0L,

    /**
     * The UTC day this run belongs to, when it is the day's deal (decision D32).
     * Rides on the state — never persisted inside the session — so persist and finish
     * route to the daily slot exactly when it is non-null.
     */
    val dailyDay: Long? = null,
) {
    /** The one quantity (method §2): filled ground over all cells. */
    val completion: Float get() = BlockRules.completion(cells)

    val isComplete: Boolean get() = over

    fun toSession() = BlockSession(
        cells = cells,
        tray = tray.map { "${it.family}:${it.variant}:${it.density}" },
        difficulty = difficulty,
        score = score,
        elapsedMs = elapsedMs,
        mistakes = 0,
        progress = completion,
    )

    companion object {
        /** A fresh run: empty board, one deal from the seed. */
        fun fresh(cells: List<Int>, tray: List<BlockRules.Piece>, difficulty: String, dailyDay: Long? = null) =
            BlockState(
                cells = cells,
                tray = tray,
                score = 0,
                difficulty = difficulty,
                dailyDay = dailyDay,
            )
    }
}
