package com.example.lightapp.games.akari

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

/**
 * `akari(10)` — admitted Tier 2. A Nikoli classic about *placing lights*: bulbs are
 * dots, the design language's own motif, and the lit state is a raise in surface —
 * the game is monochrome-compatible by construction, not by adaptation. The board's
 * walls are the only voids in the studio that carry data (a clue number).
 */
class AkariDefinition : GameDefinition {
    override val id = ID

    override val name = "akari(10)"
    override val description = "light every white cell"
    // A bulb, dot-built: the game's one verb made visible. Distinct from the
    // nonogram plus (its beam would collide) — the glow ring and base say "light",
    // not "intersection".
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(false, false, true, false, false),
            listOf(false, true, false, true, false),
            listOf(false, false, true, false, false),
            listOf(false, true, true, true, false),
            listOf(false, false, true, false, false),
        )
    )

    override val difficulties: List<String>
        get() = AkariGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = AkariViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the puzzle",
                lines = listOf(
                    "A ten-by-ten grid of white ground, black walls, and numbered walls.",
                    "Place bulbs so every white cell is lit — a bulb lights its whole row and column until a wall stops it.",
                ),
            ),
            GameRulesSection(
                title = "the rules",
                lines = listOf(
                    "No two bulbs may see each other — a wall is the only thing that breaks the light.",
                    "A numbered wall must touch exactly that many bulbs, orthogonally. Fewer is unfinished; more is wrong.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "TAP a white cell to place a bulb; TAP it again to take the bulb back. Walls do not answer.",
                    "A clash or an over-numbered wall is never announced — it is visible on the board, and the win check simply refuses to fire.",
                ),
            ),
            GameRulesSection(
                title = "logic, not luck",
                lines = listOf(
                    "Every puzzle has exactly one solution, proven before it ships — solvable by deduction alone.",
                    "TIME and LIT % read like every game here; UNDO reverts one tap, up to fifty.",
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
        AkariScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "akari"
    }
}
