package com.example.lightapp.games.sudoku

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

class SudokuDefinition : GameDefinition {
    override val id = ID

    override val name = "sudoku"
    override val description = "classic 9×9 number puzzle"
    // 9 dots for 9×9 — the game's defining dimension, not a decorative checkerboard.
    // Rule G1: dots must encode. The 5×5 grid holds a centred 3×3 block: 3×3 = 9.
    // `GameIcon` divides its 24dp box by rows.size on both axes, so the 9 dots read
    // as a miniature board, not as a numeral that needs 5×7.
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(false, false, false, false, false),
            listOf(false, true, true, true, false),
            listOf(false, true, true, true, false),
            listOf(false, true, true, true, false),
            listOf(false, false, false, false, false),
        )
    )

    override val difficulties: List<String>
        get() = SudokuViewModel.DIFFICULTIES.map { it.name }

    override val dailyLabel: String
        get() = SudokuViewModel.DAILY_NAME

    // The rules content the shell's HOW TO PLAY page renders. Owned here, not in
    // the shell: a standalone build must teach its own game, and the shell must
    // not hard-code a genre it resolves through the registry. Short lines, not
    // prose — this page teaches by pointing at behaviour the player can reproduce
    // in one tap (decision D26, amended).
    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the puzzle",
                lines = listOf(
                    "Nine rows, nine columns, nine 3×3 boxes — eighty-one cells.",
                    "Fill the empty cells so every row, column, and box holds the digits 1–9, each exactly once.",
                ),
            ),
            GameRulesSection(
                title = "givens",
                lines = listOf(
                    "The printed digits are given and fixed — nothing can change or overwrite them.",
                    "Every other cell is yours. Tap one, then choose a digit from the pad.",
                ),
            ),
            GameRulesSection(
                title = "reading the board",
                lines = listOf(
                    "Tap a cell and its row, column, and box light up — the places where its digit must fit.",
                    "Tap a filled cell and every other cell holding that digit lights up as well.",
                    "Enter a digit that already sits in its row, column, or box and the cell turns red. Correct it or erase it and keep going.",
                ),
            ),
            GameRulesSection(
                title = "notes",
                lines = listOf(
                    "NOTES turns the pad into pencil marks — candidate digits stored in the selected cell.",
                    "Marks are hints, never entries. A cell is filled only when you enter a real digit.",
                    "With auto-clean on, placing a digit erases that digit from the peers' marks — it could never have gone there anyway.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "ERASE clears the selected cell. UNDO steps back through the last fifty moves.",
                    "NEW starts a different puzzle — pick any difficulty.",
                    "DAILY is the day's puzzle — one board per UTC day, resumed where you left it.",
                    "TIME counts the moments you spend on the board. Leave, and it waits.",
                ),
            ),
            GameRulesSection(
                title = "mistakes & stats",
                lines = listOf(
                    "With MISTAKES on, a third wrong entry ends the run. Off, mistakes count but never end it.",
                    "STATS remembers played, solved, rate, and best and average time for every difficulty.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        SudokuScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         *
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "sudoku"
    }
}
