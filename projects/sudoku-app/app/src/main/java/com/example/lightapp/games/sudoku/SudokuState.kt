package com.example.lightapp.games.sudoku

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.SudokuCodec

/**
 * A playable Sudoku position.
 *
 * `@Immutable` so Compose can skip recomposition when the instance has not changed —
 * `avoid.md` bars redundant recomposition, and that rule is only enforceable if state is
 * actually immutable. Every mutation returns a new instance.
 *
 * [givens] and [entries] stay separate for the same reason they do in persistence: there
 * is no code path by which a player value overwrites a given.
 */
@Immutable
data class SudokuState(
    val givens: List<Int>,
    val entries: List<Int>,
    val notes: List<Int>,
    val difficulty: String,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val elapsedMs: Long = 0L,
    val mistakes: Int = 0,
    /**
     * The most recently entered cell. Requirement 14 criterion 10 gives the single red
     * accent to the most recently entered *conflicting* cell, so this is what decides
     * which conflict is accented and which are muted.
     */
    val lastEntered: Int? = null,
    /**
     * The UTC day this puzzle belongs to, or `null` for a regular game.
     *
     * Non-null exactly when [difficulty] is the `Daily` label. It rides on the state
     * rather than living in the view model so that persistence, completion and undo all
     * route by the state itself: a daily session saves to and clears from the daily
     * slot, never the regular one, and a puzzle started before a UTC midnight keeps the
     * day it was generated on however long it is played.
     */
    val dailyDay: Long? = null,
) {
    /** Givens and player entries as one grid, for conflict and completion checks. */
    val merged: List<Int> = List(Conflicts.CELLS) { i ->
        if (givens[i] != 0) givens[i] else entries[i]
    }

    val conflicts: Set<Int> = Conflicts.allConflicts(merged)

    val isComplete: Boolean = Conflicts.isSolved(merged)

    /** Digit → how many of it are still to be placed. Drives the number-pad counts. */
    val remaining: Map<Int, Int> = Conflicts.remainingCounts(merged)

    /**
     * Completion as a fraction — decision D20.
     *
     * Player-filled cells over initially-empty cells, so a fresh puzzle reads 0% and a
     * finished one reads 100%. Givens are excluded from both terms, otherwise a puzzle
     * would start at roughly 40% and the number would mean nothing.
     */
    val completion: Float = run {
        val emptyAtStart = givens.count { it == 0 }
        if (emptyAtStart == 0) 1f
        else entries.count { it != 0 }.toFloat() / emptyAtStart
    }

    fun isGiven(index: Int): Boolean = givens[index] != 0

    fun valueAt(index: Int): Int = merged[index]

    fun notesAt(index: Int): List<Int> = SudokuCodec.notesIn(notes[index])

    companion object {
        fun fromPuzzle(
            puzzle: IntArray,
            difficulty: String,
            dailyDay: Long? = null,
        ): SudokuState = SudokuState(
            givens = puzzle.toList(),
            entries = SudokuCodec.emptyGrid(),
            notes = SudokuCodec.emptyGrid(),
            difficulty = difficulty,
            dailyDay = dailyDay,
        )
    }
}

/**
 * One reversible change — decision D17.
 *
 * Captures the previous entry and note mask for a single cell. Bounded at 50 and held in
 * memory only: undo does not survive a process restart, which keeps the persisted
 * encoding small and avoids a migration when this format changes.
 */
data class Move(
    val index: Int,
    val previousEntry: Int,
    val previousNotes: Int,
    val previousMistakes: Int,
    val previousLastEntered: Int?,
    /**
     * Peer pencil masks the auto-clean assist removed when this entry was placed,
     * peer index → the mask that held the digit. Empty for every move that removed
     * no marks — including all moves made before the assist existed — so undo only
     * rewrites the peers it actually touched.
     */
    val previousPeerNotes: Map<Int, Int> = emptyMap(),
) {
    companion object {
        const val MAX_HISTORY = 50
    }
}
