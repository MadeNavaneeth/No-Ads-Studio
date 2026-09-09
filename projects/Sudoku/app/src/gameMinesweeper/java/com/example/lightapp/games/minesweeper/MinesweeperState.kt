package com.example.lightapp.games.minesweeper

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.MinesweeperSession

/**
 * A playable position.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * [mines] is empty at the start of every run — the layout does not exist until the
 * first reveal lands (MinesweeperGenerator), which is what makes first-tap safety
 * structural. [cells] is quad-state; there are no givens, so rule M8 has nothing to
 * separate.
 */
@Immutable
data class MinesweeperState(
    val mines: List<Boolean>,
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
    /** Adjacency counts, derived once per position — never recomputed per cell per frame. */
    val adjacency: List<Int> = MinesweeperRules.adjacency(mines)

    /** The run is lost the moment a mine detonates; red belongs to this cell alone. */
    val detonated: Boolean = cells.any { it == MinesweeperRules.DETONATED }

    val isComplete: Boolean = MinesweeperRules.isWin(cells, mines)

    /** The MINE readout: mines not yet flagged. */
    val minesRemaining: Int = MinesweeperRules.minesRemaining(mines, cells)

    /** The one quantity (method §2): revealed safe cells over total safe cells. */
    val completion: Float = MinesweeperRules.completion(cells, mines)

    /** True once the layout exists — after that, a flag is a guess about real ground. */
    val layoutPlaced: Boolean = mines.any { it }

    fun toSession() = MinesweeperSession(
        mines = mines.map { if (it) 1 else 0 },
        cells = cells,
        difficulty = difficulty,
        elapsedMs = elapsedMs,
        mistakes = mistakes,
        progress = completion,
    )

    companion object {
        /** A fresh board: no layout, all unknown. Mines arrive at first tap. */
        fun fresh(difficulty: String, dailyDay: Long? = null) = MinesweeperState(
            mines = List(MinesweeperRules.CELLS) { false },
            cells = List(MinesweeperRules.CELLS) { MinesweeperRules.UNKNOWN },
            difficulty = difficulty,
            dailyDay = dailyDay,
        )
    }
}

/** One reversible change. In memory only — decision D17. */
data class MineMove(
    val index: Int,
    val previousCell: Int,
    val previousMistakes: Int,
    val previousSelected: Int?,
) {
    companion object {
        const val MAX_HISTORY = 50
    }
}
