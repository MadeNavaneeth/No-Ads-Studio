package com.example.lightapp.core

import androidx.compose.runtime.Composable

// ═══════════════════════════════════════════════════════════════
// GAME MODULE INTERFACE
// Every game in the studio implements this single interface.
// The studio shell handles everything else.
// ═══════════════════════════════════════════════════════════════

interface GameDefinition {
    // ─── METADATA ───
    val id: String              // unique identifier, e.g. "sudoku"
    val name: String            // display name, e.g. "Sudoku"
    val description: String     // one-liner, e.g. "Classic number puzzle"
    val icon: GameIcon          // visual icon (no image assets needed)

    // ─── DIFFICULTY ───
    // Levels offered on the shell's new-game picker, e.g. ["Simple", ...].
    // Empty when the game has no levels; the shell then offers nothing to pick.
    // Defaults keep older games compiling; override to appear in the picker.
    val difficulties: List<String> get() = emptyList()

    // Optional daily-mode label, e.g. "Daily", offered first when non-null.
    // A mode, not a level: the game generates it under its own rules and slot.
    val dailyLabel: String? get() = null

    // ─── HOW TO PLAY ───
    // The game's own rules page content, as caption-plus-lines sections the shell
    // renders verbatim. Owned by the game — a standalone build shows only its one
    // game's rules, and a studio build shows one section group per game — so the
    // shell never hard-codes a genre it does not know. Short lines, not prose:
    // this page teaches by pointing at behaviour the player can reproduce in one
    // tap (decision D26, amended).
    val rules: List<GameRulesSection> get() = emptyList()

    // ─── FACTORY ───
    // Creates the game screen. The shell calls this when user taps the game.
    // onBack: navigate back to home
    // onGameComplete: call when game ends (win/lose)
    // initialDifficulty: the picker's choice ("start new here"), or null to resume.
    // Passed by name at the call site; games that ignore it just resume.
    @Composable
    fun CreateScreen(
        onBack: () -> Unit,
        onGameComplete: (GameResult) -> Unit,
        initialDifficulty: String? = null
    )
}

// ═══════════════════════════════════════════════════════════════
// GAME RESULT — what gets saved after each game
// ═══════════════════════════════════════════════════════════

/**
 * One caption over its one-line sentences, as rendered by the shell's rules screen.
 * A value type rather than a Compose type so the content stays data — testable,
 * sortable, and free of any dependency on how it is displayed.
 */
data class GameRulesSection(
    val title: String,
    val lines: List<String>,
)

// ═══════════════════════════════════════════════════════════════
// GAME RESULT — what gets saved after each game
// ═══════════════════════════════════════════════════════════════

data class GameResult(
    val gameId: String,
    val won: Boolean,
    val durationMs: Long,
    val difficulty: String,
    val moves: Int
)

// ═══════════════════════════════════════════════════════════════
// GAME ICON — pure Compose, no image assets
// ═══════════════════════════════════════════════════════════════

sealed class GameIcon {
    // Dot-matrix pixel art (Nothing signature look)
    data class DotMatrix(val rows: List<List<Boolean>>) : GameIcon()

    // Simple text/emoji fallback
    data class Letter(val char: Char) : GameIcon()
}
