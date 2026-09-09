package com.example.lightapp.games.sudoku

import com.example.lightapp.studio.persistence.SudokuSession

/**
 * Turns a saved session back into a playable position, or rejects it.
 *
 * ## Why this is not part of the codec
 *
 * `SudokuCodec` validates *shape*: 81 cells, every character a digit, note masks in
 * range. It cannot validate *meaning*, and the two fail differently. A perfectly
 * well-formed 81-character string can still describe a board that is not a Sudoku — no
 * givens at all, givens that contradict each other, or a player value sitting on top of a
 * given. Restoring one of those produces a grid the player can never clear and has no way
 * to recognise as broken, which is a worse failure than a crash because it is silent.
 *
 * So the split is: persistence guarantees the bytes decode, this guarantees the result is
 * a puzzle. Keeping it here rather than in the codec also keeps Sudoku's rules out of the
 * persistence layer, which serves every game.
 *
 * A rejected save means "start a fresh puzzle". It never means an error shown to the user
 * — there is nothing they could do about it.
 */
internal object SudokuRestore {

    /**
     * The proven minimum number of givens for a uniquely solvable 9×9 grid.
     *
     * A board with fewer cannot have come from the generator, so it is corrupt rather
     * than merely hard.
     */
    const val MIN_GIVENS = 17

    /** [session] as playable state, or `null` if it does not describe a real puzzle. */
    fun stateOrNull(session: SudokuSession): SudokuState? {
        val givens = session.givens
        val givenCount = givens.count { it != 0 }

        if (givenCount < MIN_GIVENS) return null

        // A board with every cell given is not a puzzle, it is a printout.
        if (givenCount >= Conflicts.CELLS) return null

        // The givens alone must be internally consistent.
        if (Conflicts.allConflicts(givens).isNotEmpty()) return null

        // A player value may never sit on top of a given — Requirement 16 criterion 10.
        // This is the invariant that makes a given uneditable across a restart, so it is
        // checked rather than assumed.
        if (givens.indices.any { givens[it] != 0 && session.entries[it] != 0 }) return null

        return SudokuState(
            givens = givens,
            entries = session.entries,
            notes = session.notes,
            difficulty = session.difficulty,
            // Clamped rather than rejected: a negative clock or mistake count is a
            // nonsense number attached to an otherwise valid board, and losing the board
            // over it would be the wrong trade.
            elapsedMs = session.elapsedMs.coerceAtLeast(0L),
            mistakes = session.mistakes.coerceAtLeast(0),
        )
    }
}
