package com.example.lightapp.core

import com.example.lightapp.games.connect.ConnectDefinition

/**
 * The catalog for the standalone **connect** flavour — one game.
 *
 * See the twin in `src/studio/java` for why this file exists once per flavour. The
 * short version: a product flavour picks one source set, so this is the single point
 * of divergence between a multi-game studio build and a standalone one.
 *
 * ## Why standalone at all
 *
 * Discoverability. People search stores for "numberlink" and "connect", not for
 * "puzzle studio", so a standalone listing can rank for the terms that have the
 * traffic and a bundled one cannot. The install is also smaller, and one game's
 * reviews cannot drag the others down. `gameConnect` carries no vendored code at
 * all — the generator is our own — so this listing has the cleanest licence story
 * in the catalogue.
 *
 * The cost, and it is real: with no network there is no cross-promotion between
 * separate apps. A studio build gets that for free.
 */
object GameRegistry {

    /**
     * Exactly one, and it must stay that way. A second entry here means this is no longer
     * a standalone build and belongs in the studio flavour instead.
     */
    val games: List<GameDefinition> = listOf(
        ConnectDefinition(),
    )

    /**
     * The game this build *is*, so the shell opens on its start page — CONTINUE when a
     * session is stored, PLAY when there is none — rather than on a one-item library.
     *
     * Back from the game still reaches Home, so Settings and the licence notice stay
     * reachable — the destination changes, the graph does not.
     */
    val standalone: GameDefinition? = games.first()

    fun getById(id: String): GameDefinition? = games.find { it.id == id }
}
