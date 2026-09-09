package com.example.lightapp.games.binairo

import com.example.lightapp.studio.persistence.BinairoCodec
import com.example.lightapp.studio.persistence.BinairoSession

/**
 * Saved-session validation for `binairo(10)`. A well-formed save can still describe a
 * board that never shipped — an unknown label, a negative clock, a clue set that never
 * generated, a board already solved. Restoring one gives the player a game they cannot
 * finish or one already over. Null means "start fresh".
 *
 * Semantic checks live here, not in [BinairoSession]'s `init`: the session class is
 * shared persistence vocabulary, the game owns the meaning of its own board.
 */
object BinairoRestore {

    /**
     * The daily label (decision D32) — not a generator difficulty, but a session may
     * legitimately carry it: the day's puzzle is generated at a fixed engine setting
     * and stored under its own label so the stats row reads `DAILY`.
     */
    private val KNOWN_LABELS = BinairoGenerator.DIFFICULTIES + BinairoViewModel.DAILY_NAME

    fun stateOrNull(session: BinairoSession): BinairoState? {
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null

        val cells = BinairoCodec.decodeCells(session.cells) ?: return null
        val givens = BinairoCodec.decodeGivens(session.givens) ?: return null
        val state = BinairoState(
            givens = givens,
            entries = List(BinairoRules.CELLS) { i ->
                if (givens[i] != BinairoRules.EMPTY) BinairoRules.EMPTY else cells[i]
            },
            difficulty = session.difficulty,
            elapsedMs = session.elapsedMs,
        )

        // A finished board is not an in-progress game.
        if (state.isComplete) return null

        // The given mask is the layout's authority, cell by cell: a given holds
        // exactly the value the mask names, a non-given is either undecided or a
        // player value. A given whose cell disagrees with its own mask is a puzzle
        // no generator produced.
        for (i in 0 until BinairoRules.CELLS) {
            if (givens[i] != BinairoRules.EMPTY && cells[i] != givens[i]) return null
        }
        return state
    }
}