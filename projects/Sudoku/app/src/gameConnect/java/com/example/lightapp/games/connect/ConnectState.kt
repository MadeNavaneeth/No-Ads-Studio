package com.example.lightapp.games.connect

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.ConnectSession

/**
 * A playable position for `connect(7)`.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * [path] is the live walk — the route the player's finger is currently laying. It is
 * input state, not board state: it is never persisted (a drag does not survive the
 * screen, any more than a key does), and restore always comes back with an empty walk.
 * Laid segments live in [cells] the moment [ConnectRules.step] commits them, so a save
 * taken mid-drag is consistent without the walk.
 *
 * There is no mistake counter: every move is retractable, so nothing here can be wrong.
 */
@Immutable
data class ConnectState(
    val cells: List<Int>,
    val difficulty: String,
    val path: List<Int> = emptyList(),
    val elapsedMs: Long = 0L,

    /**
     * The UTC day this run belongs to, when it is the day's puzzle (decision D32).
     * Rides on the state — never persisted inside the session — so persist/finish
     * route to the daily slot exactly when a daily is being played.
     */
    val dailyDay: Long? = null,
) {
    /** The LINE readout: pairs whose laid route joins their endpoints. */
    val solvedLines: Int get() = ConnectRules.solvedPairs(cells)

    /** Total endpoint pairs on the board — the LINE readout's denominator. */
    val totalPairs: Int get() = ConnectRules.endpointPairs(cells).size

    /** The one quantity (method §2): used cells over all cells. */
    val completion: Float get() = ConnectRules.completion(cells)

    val isComplete: Boolean get() = ConnectRules.isWin(cells)

    /** True while a walk is under way — the undo button yields to it. */
    val walkActive: Boolean get() = path.isNotEmpty()

    fun toSession() = ConnectSession(
        board = cells,
        difficulty = difficulty,
        elapsedMs = elapsedMs,
        mistakes = 0,
        progress = completion,
    )

    companion object {
        /**
         * A fresh position from a generated board. The generator's answer sheet holds
         * endpoints and empty ground only — the solution interiors were never stored,
         * so [cells] here is already exactly what the player should see.
         */
        fun fresh(board: List<Int>, difficulty: String, dailyDay: Long? = null) = ConnectState(
            cells = board,
            difficulty = difficulty,
            dailyDay = dailyDay,
        )
    }
}

/** One reversible board change — decision D17, in memory only. */
data class ConnectMove(
    val index: Int,
    val previousValue: Int,
) {
    companion object {
        const val MAX_HISTORY = 50
    }
}
