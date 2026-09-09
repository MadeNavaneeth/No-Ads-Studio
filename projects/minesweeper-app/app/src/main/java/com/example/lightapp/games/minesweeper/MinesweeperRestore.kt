package com.example.lightapp.games.minesweeper

import com.example.lightapp.studio.persistence.MinesweeperSession

/**
 * Saved-session validation. A well-formed save can still describe a board that
 * never shipped — a mine count no difficulty produces, an unknown difficulty, a
 * negative clock, a board already detonated — and restoring one gives the player
 * a game they cannot win or one already lost. Null means "start fresh".
 */
object MinesweeperRestore {

    /**
     * The daily label (decision D32) — not a generator difficulty, but a session may
     * legitimately carry it: the day's board is laid at the default mine count and
     * stored under its own label so the stats row reads `DAILY`.
     */
    private val KNOWN_LABELS = MinesweeperGenerator.DIFFICULTIES + MinesweeperViewModel.DAILY_NAME

    fun stateOrNull(session: MinesweeperSession): MinesweeperState? {
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null

        val mines = session.mines.map { it == 1 }
        // The layout must be complete and must match the difficulty's mine count —
        // a partial layout is a board with invisible mines missing, which plays
        // easier than the game the player chose.
        if (mines.count { it } != MinesweeperGenerator.mineCountFor(session.difficulty)) return null

        val state = MinesweeperState(
            mines = mines,
            cells = session.cells,
            difficulty = session.difficulty,
            elapsedMs = session.elapsedMs,
            mistakes = session.mistakes,
        )
        // A fresh board is legal to restore (mines arrive at first tap), but a
        // finished one — cleared or detonated — is not an in-progress game.
        if (state.isComplete || state.detonated) return null
        return state
    }
}
