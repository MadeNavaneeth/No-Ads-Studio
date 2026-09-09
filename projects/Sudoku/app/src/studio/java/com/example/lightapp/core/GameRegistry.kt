package com.example.lightapp.core

import com.example.lightapp.games.akari.AkariDefinition
import com.example.lightapp.games.binairo.BinairoDefinition
import com.example.lightapp.games.blockpuzzle.BlockpuzzleDefinition
import com.example.lightapp.games.connect.ConnectDefinition
import com.example.lightapp.games.minesweeper.MinesweeperDefinition
import com.example.lightapp.games.nonogram.NonogramDefinition
import com.example.lightapp.games.sudoku.SudokuDefinition
import com.example.lightapp.games.wordsearch.WordsearchDefinition

/**
 * The catalog for the **studio** flavour — every game, presented as a library.
 *
 * ## Why this file exists twice
 *
 * There is a second copy in `src/sudoku/java`. That is deliberate, and it is the only file
 * the two flavours disagree about. A product flavour selects one source set, so this is how
 * one code tree produces both a multi-game studio app and a standalone single-game app
 * without a second repository, a published design-system artifact, or any copied UI.
 *
 * The alternative — separate projects per game — means a token change becomes N releases and
 * the shared design system has to be published or duplicated. Duplicating it is precisely
 * what `design-canon/` exists to prevent.
 *
 * Adding a game touches this file and `ideas/games-roadmap.md`, and the two must agree —
 * `M5-roadmap-agreement` fails the build otherwise, because a game in code but not in the
 * roadmap has skipped the admission criteria.
 */
object GameRegistry {
    val games: List<GameDefinition> = listOf(
        SudokuDefinition(),
        NonogramDefinition(),
        MinesweeperDefinition(),
        ConnectDefinition(),
        WordsearchDefinition(),
        BlockpuzzleDefinition(),
        AkariDefinition(),
        BinairoDefinition(),
    )

    /**
     * Null, because this build ships a library rather than one game.
     *
     * The shell reads this to pick its start destination: a catalog opens on Home, a
     * standalone build opens on the game itself.
     */
    val standalone: GameDefinition? = null

    fun getById(id: String): GameDefinition? = games.find { it.id == id }
}