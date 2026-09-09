package com.example.lightapp.games.binairo

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

/**
 * `binairo(10)` — admitted from the roadmap backlog (candidate #8). A grid of two
 * states, monochrome by definition: the puzzle's own subject is a binary grid, which
 * is exactly what this design language draws. The board *is* a dot field — solid
 * dots and hollow rings are the two states, the D33 dot-density device doing real
 * work in a game where it is the vocabulary rather than the identity.
 */
class BinairoDefinition : GameDefinition {
    override val id = ID

    override val name = "binairo(10)"
    override val description = "balance every line"
    // A 5×5 field of dots in the game's own rhythm — pairs and gaps, no two rows
    // alike — the balance constraint made visible as dot placement, not decoration.
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(true, false, true, false, false),
            listOf(false, false, false, false, true),
            listOf(true, false, false, true, false),
            listOf(false, false, false, false, false),
            listOf(false, true, false, true, false),
        )
    )

    override val difficulties: List<String>
        get() = BinairoGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = BinairoViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the puzzle",
                lines = listOf(
                    "A ten-by-ten grid of undecided cells, with some values given.",
                    "Fill every cell with a dot (one) or a ring (zero).",
                ),
            ),
            GameRulesSection(
                title = "the rules",
                lines = listOf(
                    "No three consecutive cells in any row or column may agree.",
                    "Every row and column holds exactly five dots and five rings.",
                    "No two rows are identical, and no two columns are identical.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "TAP an undecided cell to place a dot; TAP again for a ring, again to clear.",
                    "A given cell refuses the tap — it is part of the puzzle, not the play.",
                ),
            ),
            GameRulesSection(
                title = "logic, not luck",
                lines = listOf(
                    "Every puzzle is solvable by deduction alone, proven before it ships — which is also proof it has exactly one solution.",
                    "A run of three is visible on the board, never announced; the win check simply refuses until it resolves.",
                    "TIME, DONE and LINE read like every game here; UNDO reverts one tap, up to fifty.",
                ),
            ),
            GameRulesSection(
                title = "the daily",
                lines = listOf(
                    "DAILY is the day's puzzle — one board per UTC day, generated the same on every device.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        BinairoScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "binairo"
    }
}