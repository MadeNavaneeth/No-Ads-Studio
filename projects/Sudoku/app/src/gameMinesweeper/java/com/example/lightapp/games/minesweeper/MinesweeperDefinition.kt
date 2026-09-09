package com.example.lightapp.games.minesweeper

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

class MinesweeperDefinition : GameDefinition {
    override val id = ID

    override val name = "minesweeper(10)"
    override val description = "clear the field, spare the mines"
    // The minefield itself: the one deterministic mark minesweeper owns is where
    // the danger sits. Rule G1: dots must encode. Distinct from nonogram(10)'s
    // plus (its intersection logic) on purpose.
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(false, false, false, true, false),
            listOf(false, true, false, false, false),
            listOf(false, false, false, false, true),
            listOf(true, false, false, true, false),
            listOf(false, false, true, false, false),
        )
    )

    override val difficulties: List<String>
        get() = MinesweeperGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = MinesweeperViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the field",
                lines = listOf(
                    "A nine-by-nine field hides ten mines. Open every safe cell — and never open a mine.",
                ),
            ),
            GameRulesSection(
                title = "the first tap",
                lines = listOf(
                    "Your first tap is always safe. The field is laid only when you touch it, and never around your starting cell.",
                ),
            ),
            GameRulesSection(
                title = "the numbers",
                lines = listOf(
                    "A number counts the mines in the eight cells touching it.",
                    "A zero touches nothing, so its whole neighbourhood opens at once.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "TAP opens a cell. HOLD plants a flag where you believe a mine sits.",
                    "The MINE readout counts mines not yet accounted for by flags.",
                ),
            ),
            GameRulesSection(
                title = "the red cell",
                lines = listOf(
                    "If a mine opens, it is the one red cell on the screen, and the run ends.",
                    "The board stays exactly as you left it, so the loss can be read, not just felt.",
                ),
            ),
            GameRulesSection(
                title = "stats",
                lines = listOf(
                    "STATS remembers played, solved, rate, and best and average time for every difficulty.",
                    "DAILY is the day's field — one per UTC day, laid the same way for the same opening.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        MinesweeperScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         *
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "minesweeper"
    }
}
