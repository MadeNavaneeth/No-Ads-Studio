package com.example.lightapp.games.connect

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

class ConnectDefinition : GameDefinition {
    override val id = ID

    override val name = "connect(7)"
    override val description = "join every pair, spend every cell"
    // The parenthetical is the identity: a dot-matrix "7", drawn from the same rule
    // as sudoku's "9" and nonogram's "10". Rule G1: dots must encode. Five rows —
    // the GameIcon's practical ceiling at icon scale (spec, GameIcon).
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(true, true, true, true, true),
            listOf(false, false, false, true, false),
            listOf(false, false, false, true, false),
            listOf(false, false, false, true, false),
            listOf(false, false, false, true, false),
        )
    )

    override val difficulties: List<String>
        get() = ConnectGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = ConnectViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the puzzle",
                lines = listOf(
                    "Seven pairs of numbers sit on a seven-by-seven grid. Join every pair with a path of the same number.",
                    "Paths travel between touching cells — sideways or up and down, never diagonally.",
                ),
            ),
            GameRulesSection(
                title = "the whole board",
                lines = listOf(
                    "A solved grid leaves no cell unused: every path matters, and so does every gap you leave behind.",
                    "Paths may never cross or share a cell, even with themselves.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "TAP a number to begin its path, then step cell by cell to its partner.",
                    "TAP your own path to take a step back, or lift your finger to leave it as is.",
                    "Paths are never wrong — retake any of them at any time. Nothing here turns red.",
                ),
            ),
            GameRulesSection(
                title = "logic, not luck",
                lines = listOf(
                    "Every puzzle is solvable by deduction alone — no guessing is ever required.",
                    "STATS remembers played, solved, rate, and best and average time for every difficulty.",
                    "DAILY is the day's grid — one board per UTC day, resumed where you left it.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        ConnectScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         *
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "connect"
    }
}
