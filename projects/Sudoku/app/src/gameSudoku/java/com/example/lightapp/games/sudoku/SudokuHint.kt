package com.example.lightapp.games.sudoku

import com.example.lightapp.studio.persistence.SudokuCodec

/**
 * The next provable step, derived from the board alone.
 *
 * **This file is why sudoku can have hints at all.** The game deliberately never
 * holds the solved grid (Conflicts.kt): no branch can leak the answer, and a wrong
 * value that does not yet conflict goes unflagged — the honest behaviour of a puzzle
 * that refuses to know. A "reveal this cell" hint would require holding the solution
 * and would spend that principle for a shortcut. So the only hint this game offers
 * is a **deduction**: a cell whose row, column, and box already contain every other
 * digit, making its value provable from the board the player can already see.
 *
 * The player could always compute this themselves — which is exactly the standard
 * `showRemaining` set on the number pad: expose the mechanism, never replace it.
 */
object SudokuHint {

    /**
     * The next naked single: an empty cell with exactly one candidate digit.
     *
     * A selected cell is checked first — the player pointing at a cell is the
     * strongest statement of intent the interface has, so the hint answers *that
     * cell* when it has a provable answer before scanning the rest of the board.
     * Otherwise the scan runs in reading order, which is stable and therefore
     * learnable: the same board always hints the same cell.
     *
     * Null when no cell is forced. That is a real answer, not a failure — it means
     * the next step needs a technique this engine does not claim (pairs, pointing
     * lines, and up), and saying nothing is more honest than guessing.
     */
    fun nakedSingle(state: SudokuState): Hint? {
        state.selected?.let { selected ->
            hintAt(state, selected)?.let { return it }
        }
        for (index in 0 until Conflicts.CELLS) {
            hintAt(state, index)?.let { return it }
        }
        return null
    }

    /**
     * The provable value for one cell, or null.
     *
     * A cell's candidates are the digits its 20 peers do not already hold. One
     * candidate means the value is forced by elimination; two or more means this
     * technique proves nothing; zero means the board is already contradictory
     * under entries the player made, which the conflict highlight owns.
     */
    fun hintAt(state: SudokuState, index: Int): Hint? {
        if (state.valueAt(index) != 0) return null
        val taken = HashSet<Int>(20)
        for (peer in Conflicts.peersOf(index)) {
            val value = state.valueAt(peer)
            if (value in 1..Conflicts.SIZE) taken.add(value)
        }
        val candidates = (1..Conflicts.SIZE).filterNot { it in taken }
        if (candidates.size != 1) return null
        return Hint(index = index, digit = candidates.single())
    }
}

/** One provable step: [digit] is the only value [index] can legally hold. */
data class Hint(
    val index: Int,
    val digit: Int,
)

/**
 * Peer-note hygiene: for every peer of [index] still holding [digit] as a pencil
 * mark, the mask with that mark removed.
 *
 * This is the auto-clean assist. A placed digit proves its peers cannot hold it,
 * so leaving the stale marks standing makes every pencil grid lie a little more
 * each move — and erasing them by hand is the chore every modern sudoku app removes.
 * Computed purely so it is testable on the JVM; the view model applies it and undo
 * reverses it.
 */
internal fun cleanedPeerNotes(notes: List<Int>, index: Int, digit: Int): Map<Int, Int> {
    val out = LinkedHashMap<Int, Int>()
    for (peer in Conflicts.peersOf(index)) {
        val mask = notes[peer]
        if (SudokuCodec.hasNote(mask, digit)) out[peer] = SudokuCodec.toggleNote(mask, digit)
    }
    return out
}
