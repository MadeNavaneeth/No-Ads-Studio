package com.example.lightapp.games.binairo

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.BinairoCodec
import com.example.lightapp.studio.persistence.BinairoSession

/**
 * A playable position for `binairo(10)`.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * **Givens and entries stay separate** — rule M8, the sudoku scaffold's shape. A given
 * is uneditable because there is no code path by which a player value overwrites one,
 * including across a process restart. Akari could fold everything into one array because
 * its walls are structurally distinct from bulbs; binairo's givens look exactly like
 * player entries (both are ONE/ZERO), so the separation is load-bearing here.
 */
@Immutable
data class BinairoState(
    /** The puzzle as shipped: [BinairoRules.ONE]/[BinairoRules.ZERO] at givens, EMPTY elsewhere. */
    val givens: List<Int>,
    /** The player's values only: ONE/ZERO where decided, EMPTY elsewhere. */
    val entries: List<Int>,
    val difficulty: String = BinairoGenerator.DEFAULT_DIFFICULTY,
    val elapsedMs: Long = 0L,

    /**
     * The UTC day this run belongs to, when it is the day's deal (decision D32).
     * Rides on the state — never persisted inside the session — so persist and finish
     * route to the daily slot exactly when it is non-null.
     */
    val dailyDay: Long? = null,
) {
    /** Givens and player values as one grid, for rule checks and rendering. Never persisted as such. */
    val merged: List<Int> = List(BinairoRules.CELLS) { i ->
        if (givens[i] != BinairoRules.EMPTY) givens[i] else entries[i]
    }

    /** The board's visible contradiction — three dots in a row — found by looking, never announced. */
    val runViolations: Set<Int> = BinairoRules.runViolationCells(merged)

    val isComplete: Boolean get() = BinairoRules.isWin(merged)

    /** The one quantity (method §2): player-decided cells over the cells the player was asked for. */
    val completion: Float get() = BinairoRules.completion(entries)

    /** Rows and columns fully decided and run-free — the LINE readout. */
    val linesSolved: Int get() = BinairoRules.linesSolved(merged, runViolations)

    fun isGiven(index: Int): Boolean = givens[index] != BinairoRules.EMPTY

    fun toSession() = BinairoSession(
        cells = BinairoCodec.encodeCells(merged),
        givens = BinairoCodec.encodeGivens(givens),
        difficulty = difficulty,
        elapsedMs = elapsedMs,
        mistakes = 0,
        progress = BinairoRules.completion(entries),
    )

    companion object {
        /** The fresh start: nothing decided by the player yet. */
        fun start(givens: List<Int>, difficulty: String, dailyDay: Long? = null) = BinairoState(
            givens = givens,
            entries = List(BinairoRules.CELLS) { BinairoRules.EMPTY },
            difficulty = difficulty,
            dailyDay = dailyDay,
        )
    }
}