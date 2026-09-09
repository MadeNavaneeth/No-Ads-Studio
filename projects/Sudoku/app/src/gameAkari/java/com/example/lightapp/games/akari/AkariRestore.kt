package com.example.lightapp.games.akari

import com.example.lightapp.studio.persistence.AkariCodec
import com.example.lightapp.studio.persistence.AkariSession

/**
 * Saved-session validation for `akari(10)`. A well-formed save can still describe a
 * board that never shipped — an unknown label, a negative clock, a wall layout that
 * never generated, a board already solved. Restoring one gives the player a game
 * they cannot finish or one already over. Null means "start fresh".
 *
 * Semantic checks live here, not in [AkariSession]'s `init`: the session class is
 * shared persistence vocabulary, the game owns the meaning of its own board.
 */
object AkariRestore {

    /**
     * The daily label (decision D32) — not a generator difficulty, but a session may
     * legitimately carry it: the day's puzzle is generated at a fixed engine setting
     * and stored under its own label so the stats row reads `DAILY`.
     */
    private val KNOWN_LABELS = AkariGenerator.DIFFICULTIES + AkariViewModel.DAILY_NAME

    fun stateOrNull(session: AkariSession): AkariState? {
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null

        val cells = AkariCodec.decodeCells(session.cells) ?: return null
        val walls = AkariCodec.decodeWalls(session.walls) ?: return null
        val state = AkariState(cells = cells, difficulty = session.difficulty, elapsedMs = session.elapsedMs)

        // A finished board is not an in-progress game.
        if (state.isComplete) return null

        // The mask is the layout's authority, cell by cell: a masked wall must be
        // blank or numbered (the generator's only wall kinds), ground must be dark
        // or hold a bulb. A clue on ground, a bulb inside a wall, or a wall the
        // mask does not know about is a puzzle no generator produced.
        for (i in 0 until AkariRules.CELLS) {
            val v = cells[i]
            if (walls[i]) {
                if (v != AkariRules.WALL && AkariRules.clueNumber(v) == null) return null
            } else {
                if (v != AkariRules.EMPTY && v != AkariRules.BULB) return null
            }
        }
        return state
    }
}
