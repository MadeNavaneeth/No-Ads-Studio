package com.example.lightapp.games.nonogram

import com.example.lightapp.studio.persistence.NonogramSession

/**
 * Saved-session validation. A well-formed save can still describe a puzzle that
 * never shipped — an empty solution (instant win), an unknown difficulty, a
 * negative clock — and restoring one gives the player a grid they can never
 * clear or one already cleared. Null means "generate instead".
 */
object NonogramRestore {

    /**
     * The daily label (decision D32) — not a generator difficulty, but a session may
     * legitimately carry it: the day's puzzle is generated at a fixed engine setting
     * and stored under its own label so the stats row reads `DAILY`.
     */
    private val KNOWN_LABELS = NonogramGenerator.DIFFICULTIES + NonogramViewModel.DAILY_NAME

    fun stateOrNull(session: NonogramSession): NonogramState? {
        if (session.solution.count { it == 1 } !in
            NonogramGenerator.MIN_FILLS..NonogramGenerator.MAX_FILLS
        ) return null
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null
        return NonogramState(
            solution = session.solution,
            cells = session.cells,
            difficulty = session.difficulty,
            elapsedMs = session.elapsedMs,
            mistakes = session.mistakes,
        )
    }
}
