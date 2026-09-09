package com.example.lightapp.games.blockpuzzle

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

/**
 * `blockpuzzle(8)` — the studio's first endless game, admitted Tier 3 by decision
 * D33 with the motion question already ruled: the piece follows the finger (input),
 * then resolves by opacity (S13). Piece distinction is dot density — the language's
 * own data-bearing device — never colour.
 */
class BlockpuzzleDefinition : GameDefinition {

    override val id: String
        get() = ID

    override val name: String
        get() = "blockpuzzle"

    override val description: String
        get() = "land pieces, clear lines, chase the score — endless"

    override val icon: GameIcon
        get() = GameIcon.DotMatrix(
            // Dot-matrix "8" — the parenthetical is the identity, same rule as every
            // game's launcher glyph. Full rows top, middle, bottom; sides between.
            rows = listOf(
                listOf(true, true, true, true, true),
                listOf(true, false, false, false, true),
                listOf(true, true, true, true, true),
                listOf(true, false, false, false, true),
                listOf(true, true, true, true, true),
            ),
        )

    override val difficulties: List<String>
        get() = BlockGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = BlockViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the run",
                lines = listOf(
                    "An eight-by-eight board and a tray of three pieces. Land every piece; when the tray empties, three more are dealt.",
                    "There is no win and no time limit — the run ends when nothing left in the tray fits.",
                ),
            ),
            GameRulesSection(
                title = "the lines",
                lines = listOf(
                    "Fill a whole row or column and it clears. Rows and columns clear together, at the same moment.",
                    "A cell where a full row and a full column cross clears once.",
                ),
            ),
            GameRulesSection(
                title = "the pieces",
                lines = listOf(
                    "Tray pieces are drawn solid, dotted, or hollow — a piece's density is its identity, not decoration.",
                    "Every dealt set can be placed somewhere on the board it was dealt onto. The deal never kills a run; only the board can.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "DRAG a piece from the tray onto the board — it follows your finger and lands where it fits.",
                    "A piece that fits nowhere refuses to land; nothing is lost, nothing turns red.",
                    "SCORE counts landed cells plus ten per cleared line. TIME and FILL % read like every game here.",
                ),
            ),
            GameRulesSection(
                title = "the daily",
                lines = listOf(
                    "DAILY is the day's deal — one tray per UTC day, dealt the same on every device.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        BlockScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         *
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "block"
    }
}
