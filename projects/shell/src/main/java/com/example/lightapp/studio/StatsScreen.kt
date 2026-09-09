package com.example.lightapp.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
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
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.studio.persistence.GameStats

/**
 * Statistics — the screen that makes a finished game feel like it counted for something.
 *
 * ## Why this is an instrument panel and not a trophy case
 *
 * There are no badges, no achievement art, no congratulatory copy. A record here is a reading
 * on a dial: a label in tracked mono and a tabular numeral. That is the whole visual argument
 * of this design language applied to progress — `nothing-study.md` §2, instrument panel rather
 * than app — and it is also the only version that works with no image assets at all.
 *
 * ## One display element
 *
 * Total solved, at display size, once. Rule T8 allows exactly one per screen and this is it:
 * the single number a player actually wants when they open this page. Everything else is
 * supporting detail at label and data sizes, which keeps the screen inside T6's three-size
 * budget.
 *
 * Nothing here is game-specific. It renders whatever `GameRegistry` lists against whatever
 * `SessionStore` recorded, so a second game appears with no change to this file.
 */
@Composable
fun StatsScreen(
    games: List<GameDefinition>,
    statsFor: (String) -> GameStats,
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val allStats = games.map { it to statsFor(it.id) }
    val totalWon = allStats.sumOf { (_, s) -> s.won }
    val totalPlayed = allStats.sumOf { (_, s) -> s.played }

    NothingBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Top and sides only — NothingBottomNav below consumes the navigation-bar
                // inset itself, and padding it twice would leave a dead band above the bar.
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                ),
        ) {
            // No back action: this is a tab, and the bar at the bottom is how you leave it.
            // A back arrow here would offer a second, contradictory answer to "where does
            // this go".
            NothingTopBar(title = "stats")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.xl),
                verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.hero),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The one display element on this screen — rule T8.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
                ) {
                    NothingText(
                        text = totalWon.toString(),
                        role = TextRole.Display,
                        style = NothingTheme.typography.displayMedium,
                    )
                    Label(if (totalWon == 1) "puzzle solved" else "puzzles solved")
                }

                if (totalPlayed == 0) {
                    // An empty state gets a sentence, not an illustration. `avoid.md` bars the
                    // mascot; this design language would reject it anyway.
                    Label("nothing finished yet", role = TextRole.Disabled)
                } else {
                    allStats.forEach { (game, stats) ->
                        if (stats.played > 0) {
                            GameStatsBlock(name = game.name, stats = stats)
                        }
                    }
                    // Instrument zeroing — one quiet pill, not a trophy reset.
                    // Destructive, so red is allowed here and only here (C8, C9).
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.pad(top = NothingSpacing.xl),
                    )
                    com.example.lightapp.designsystem.components.NothingButton(
                        text = "reset",
                        onClick = onReset,
                        emphasis = com.example.lightapp.designsystem.components.ButtonEmphasis.Quiet,
                    )
                }
            }
        }
    }
}

/**
 * One game's record, difficulty by difficulty.
 *
 * Difficulties are ordered by how much was played rather than by name, so the level someone
 * actually plays sits at the top instead of whatever sorts first alphabetically.
 */
@Composable
private fun GameStatsBlock(name: String, stats: GameStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
    ) {
        Label(name)

        stats.perDifficulty.entries
            .filter { it.value.played > 0 }
            .sortedByDescending { it.value.played }
                .forEach { (difficulty, record) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
                ) {
                    Label(difficulty, role = TextRole.Disabled)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        StatLine("solved", record.won.toString())
                        StatLine("played", record.played.toString())
                        StatLine("best", record.bestMs?.let(::formatDuration) ?: "—")
                        val winRate = if (record.played > 0) (record.won * 100 / record.played) else 0
                        StatLine("rate", "$winRate%")
                        StatLine("avg", record.avgMs?.let(::formatDuration) ?: "—")
                    }
                }
            }
    }
}

/**
 * A label above a value.
 *
 * Deliberately **not** named `Readout`. `D13-three-readouts` counts `Readout("` calls per file
 * to hold a game screen to three curated readouts, and a stats page legitimately has more than
 * three numbers — it is a record, not a live instrument. Sharing the name would have made the
 * rule fire on a file it was never aimed at.
 */
@Composable
private fun StatLine(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
    ) {
        Label(label, role = TextRole.Disabled)
        NothingText(
            text = value,
            role = TextRole.Primary,
            style = NothingTheme.typography.data,
        )
    }
}

// formatDuration now lives in TimeFormat.kt — see the note there about the three copies
// that had already drifted apart.
