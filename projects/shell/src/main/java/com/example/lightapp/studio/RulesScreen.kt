package com.example.lightapp.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad

/**
 * How to play — decision D26, amended 2026-09-03.
 *
 * The original decision refused a rules page: anyone opening a Sudoku app knows
 * Sudoku, and a wall of instructional prose is a bad fit for three type sizes and
 * no illustrations. The amendment keeps both objections honest — this page is short
 * lines, not prose, and it leads with the signals the board already speaks (the
 * cross, the sibling set, the single red) rather than with abstract rules. It
 * teaches by pointing at behaviour the player can reproduce in one tap.
 *
 * Structure follows the composition language: each section is a Label caption over
 * one-line sentences, groups separated by whitespace (space32) only, never a
 * divider (Requirement 11 criterion 8). No display-level text, no red — this screen
 * must stay quieter than every surface it explains.
 *
 * The content is **the games' own**. Each definition carries its rules as data
 * (`GameDefinition.rules`), so a standalone build teaches exactly its one game and
 * the studio teaches all of them, grouped under the game's display name. The shell
 * hard-codes no genre — the one time it did, every non-sudoku standalone was
 * teaching sudoku.
 */
@Composable
fun RulesScreen(
    games: List<GameDefinition>,
    standalone: GameDefinition?,
    onBack: () -> Unit,
) {
    NothingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NothingTopBar(
                title = "how to play",
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
            ) {
                // One game, or every game. A standalone build renders exactly its own
                // definition; the studio renders the whole registry, each group led by
                // the game's display name so the page stays navigable at five games.
                val shown = standalone?.let { listOf(it) } ?: games
                shown.forEach { game ->
                    // The studio labels each group; a standalone build shows exactly one
                    // game, and the screen's title already says which.
                    if (standalone == null) {
                        NothingText(
                            text = game.name,
                            role = TextRole.Secondary,
                            style = NothingTheme.typography.data,
                        )
                    }
                    game.rules.forEach { section ->
                        RulesSection(title = section.title, lines = section.lines)
                    }
                }
            }
        }
    }
}

/**
 * One caption over its one-line sentences. The caption is a Label — supporting
 * voice, deliberately dimmer than the lines it introduces — and the lines are the
 * screen's only Primary text.
 */
@Composable
private fun RulesSection(
    title: String,
    lines: List<String>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
    ) {
        Label(title)
        lines.forEach { line ->
            NothingText(text = line, role = TextRole.Primary)
        }
    }
}
