package com.example.lightapp.games.blockpuzzle

import com.example.lightapp.studio.persistence.BlockSession

/**
 * Saved-run validation for `blockpuzzle(8)`. A well-formed save can still describe a
 * run that never shipped — a tray triple naming a family the catalog does not have,
 * a negative clock, an already-finished run. Null means "start fresh".
 *
 * Semantic checks live here, not in [BlockSession]'s `init`: the session class is
 * shared persistence vocabulary, the game owns the meaning of its own board.
 */
object BlockRestore {

    /**
     * The daily label (decision D32) — carried like any other game's, though this
     * game's only "difficulty" today is `Classic`.
     */
    private val KNOWN_LABELS = BlockGenerator.DIFFICULTIES + BlockViewModel.DAILY_NAME

    fun stateOrNull(session: BlockSession): BlockState? {
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null

        val tray = ArrayList<BlockRules.Piece>(session.tray.size)
        for (triple in session.tray) {
            val parts = triple.split(":")
            if (parts.size != 3) return null
            val variant = parts[1].toIntOrNull() ?: return null
            val density = parts[2].toIntOrNull() ?: return null
            val variants = BlockRules.CATALOG[parts[0]] ?: return null
            if (variant !in variants.indices) return null
            if (density !in BlockRules.DENSITIES.indices) return null
            tray += BlockRules.Piece(family = parts[0], variant = variant, density = density)
        }

        val state = BlockState(
            cells = session.cells,
            tray = tray,
            score = session.score,
            difficulty = session.difficulty,
            elapsedMs = session.elapsedMs,
        )
        // A finished run is not an in-progress game — and "finished" is a semantic
        // fact here, not a stored flag: an empty tray (the deal was consumed and no
        // refill was possible) or a tray with no legal landing anywhere is a dead
        // run by definition. Restoring one would hand the player a board they can
        // only stare at.
        if (state.tray.isEmpty()) return null
        if (!BlockRules.anyPlacement(state.cells, state.tray)) return null
        return state
    }
}
