package com.example.lightapp.core

import com.example.lightapp.games.binairo.BinairoDefinition

/**
 * The catalog for the standalone **binairo** flavour — one game.
 *
 * See the twin in `src/studio/java` for why this file exists once per flavour. The
 * short version: a product flavour picks one source set, so this is the single point
 * of divergence between a multi-game studio build and a standalone one.
 *
 * `gameBinairo` carries no vendored code — the generator and its deduction proof are
 * our own — so this listing has a clean licence story, same as every non-sudoku game.
 */
object GameRegistry {

    /**
     * Exactly one, and it must stay that way. A second entry here means this is no longer
     * a standalone build and belongs in the studio flavour instead.
     */
    val games: List<GameDefinition> = listOf(
        BinairoDefinition(),
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