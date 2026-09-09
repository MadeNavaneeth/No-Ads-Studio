package com.example.lightapp.games.wordsearch

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult
import com.example.lightapp.core.GameRulesSection

class WordsearchDefinition : GameDefinition {
    override val id = ID

    override val name = "wordsearch(12)"
    override val description = "twelve-by-twelve word hunt"
    // The hunt reads left-to-right or top-to-bottom: a two-cell "1 2", the game's
    // defining number, drawn on the same dot pitch as sudoku's "9", nonogram's
    // "10", and connect's "7". Rule G1: dots must encode — the parenthetical is
    // the identity, and this one stays distinct from every other glyph on the
    // shelf. `GameIcon` divides its 24dp box by rows.size, so the seven-row pitch
    // matches the launcher icons exactly.
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(true, false, false, false, true),
            listOf(true, false, false, false, false),
            listOf(true, false, false, false, true),
            listOf(true, false, false, false, true),
            listOf(true, false, false, false, true),
            listOf(true, false, false, false, true),
            listOf(true, false, false, false, true),
        )
    )

    override val difficulties: List<String>
        get() = WordsearchGenerator.DIFFICULTIES

    override val dailyLabel: String?
        get() = WordsearchViewModel.DAILY_NAME

    override val rules: List<GameRulesSection>
        get() = listOf(
            GameRulesSection(
                title = "the hunt",
                lines = listOf(
                    "A twelve-by-twelve grid of letters hides a list of words — forwards, backwards, up, down, and diagonally.",
                ),
            ),
            GameRulesSection(
                title = "controls",
                lines = listOf(
                    "DRAG from the first letter of a word to its last — or from its last to its first. The line lights as you trace it.",
                    "Only a straight line locks. A wiggle simply does not count, and nothing here turns red.",
                ),
            ),
            GameRulesSection(
                title = "when a word locks",
                lines = listOf(
                    "A found word's letters stay lit and readable in the grid; its entry in the list dims.",
                    "Crossing words share their lit letters — a single drag can find two words at once.",
                ),
            ),
            GameRulesSection(
                title = "stats",
                lines = listOf(
                    "STATS remembers played, solved, rate, and best and average time for every difficulty.",
                    "DAILY is the day's grid — one puzzle per UTC day, resumed where you left it.",
                ),
            ),
        )

    @Composable
    override fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String?,
    ) {
        WordsearchScreen(onBack = onBack, onComplete = onGameComplete, initialDifficulty = initialDifficulty)
    }

    companion object {
        /**
         * The registry id, and the `gameId` on every [GameResult] this game emits.
         *
         * A constant because two places need the same string and a saved navigation
         * destination holds it across process death — a typo would resolve to "unknown
         * game" rather than fail to compile.
         */
        const val ID = "wordsearch"
    }
}
