package com.example.lightapp.games.connect

import com.example.lightapp.studio.persistence.ConnectSession

/**
 * Saved-session validation. A well-formed save can still describe a board that never
 * shipped — an unknown difficulty, a negative clock, segments orphaned from any route,
 * a board already solved. Restoring one gives the player a game they cannot finish or
 * one already over. Null means "start fresh".
 *
 * Semantic checks live here, not in [ConnectSession]'s `init`: the session class is
 * shared persistence vocabulary, the game owns the meaning of its own board.
 */
object ConnectRestore {

    /**
     * The daily label (decision D32) — not a generator difficulty, but a session may
     * legitimately carry it: the day's puzzle is generated at a fixed engine setting
     * and stored under its own label so the stats row reads `DAILY`.
     */
    private val KNOWN_LABELS = ConnectGenerator.DIFFICULTIES + ConnectViewModel.DAILY_NAME

    fun stateOrNull(session: ConnectSession): ConnectState? {
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null

        val state = ConnectState(
            cells = session.board,
            difficulty = session.difficulty,
            elapsedMs = session.elapsedMs,
        )
        // A finished board is not an in-progress game.
        if (state.isComplete) return null

        // Every laid segment must sit on a real route for its pair — a route the
        // player could have laid by legal play. An orphaned segment (disconnected
        // from its pair's endpoint chain) is corruption: accept it and the board
        // may be unwinnable, because the win check will never count that route done.
        val pairs = ConnectRules.endpointPairs(session.board)
        for (p in pairs) {
            if (ConnectRules.routeFor(session.board, p) == null) {
                // The pair has laid segments that do not form a routable chain.
                if (ConnectRules.laidCount(session.board, p) > 0) return null
            }
        }
        return state
    }
}
