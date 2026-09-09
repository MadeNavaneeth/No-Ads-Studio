package com.example.lightapp.games.nonogram

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

class NonogramDefinition : GameDefinition {
    override val id = ID

    override val name = "nonogram(10)"
    override val description = "ten-by-ten picture logic"
    // Row-plus-column logic, drawn as a plus: the intersection this game is solved on.
    // Rule G1: dots must encode. Distinct from sudoku(9)'s centred block on purpose.
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(false, false, true, false, false),
            listOf(false, false, true, false, false),
            listOf(true, true, true, true, true),
            listOf(false, false, true, false, false),
            listOf(false, false, true, false, false),
        )
    )

    override val difficulties: List<String>
        get() = NonogramGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = NonogramViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the puzzle",
                lines = listOf(
                    "A ten-by-ten grid sits between two sets of numbers — one per row, one per column.",
                    "The numbers are the blueprint. Fill the cells they describe and a picture resolves.",
                ),
            ),
            GameRulesSection(
                title = "reading the clues",
                lines = listOf(
                    "Each number is an unbroken run of filled cells in that line, in order.",
                    "Runs are separated by at least one empty cell — a clue of 2 1 is never 3.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "TAP fills a cell. HOLD marks a cell as definitely empty — the dot is a decision, not a scribble.",
                    "DRAG paints a whole run at once; the run's mode comes from where you started it.",
                ),
            ),
            GameRulesSection(
                title = "logic, not luck",
                lines = listOf(
                    "Every puzzle is solvable by deduction alone — no guessing is ever required.",
                    "When a clue is fully satisfied it dims, and the line is done.",
                    "A wrong fill is found by contradiction, not announced: nothing here turns red.",
                ),
            ),
            GameRulesSection(
                title = "mistakes & stats",
                lines = listOf(
                    "With MISTAKES on, too many wrong fills end the run. Off, they only cost time.",
                    "STATS remembers played, solved, rate, and best and average time for every difficulty.",
                    "DAILY is the day's puzzle — one picture per UTC day, resumed where you left it.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        NonogramScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         *
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "nonogram"
    }
}
