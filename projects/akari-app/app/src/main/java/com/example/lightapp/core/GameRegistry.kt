package com.example.lightapp.core

import com.example.lightapp.games.akari.AkariDefinition

/**
 * The catalog for the standalone **akari** flavour — one game.
 *
 * A standalone build carries only its own game code. The shell provides the chrome;
 * this file is the single point of divergence between a multi-game studio build and
 * a standalone one.
 */
object GameRegistry {
    val games: List<GameDefinition> = listOf(
        AkariDefinition(),
    )

    /** The game this build *is*, so the shell opens straight into it. */
    val standalone: GameDefinition? = games.first()

    fun getById(id: String): GameDefinition? = games.find { it.id == id }
}