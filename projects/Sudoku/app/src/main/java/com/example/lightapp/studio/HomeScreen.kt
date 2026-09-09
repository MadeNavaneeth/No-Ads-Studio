package com.example.lightapp.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.designsystem.components.GameIcon as GameIconGlyph
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingCard
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.SlideDotIndicator
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics

/**
 * A game in progress, as the shell sees it.
 *
 * Deliberately game-agnostic: five plain values a resume card can render without knowing what
 * kind of puzzle produced them. A second game populates this the same way and the card needs
 * no change.
 */
data class ResumeState(
    val gameId: String,
    val difficulty: String,
    val progress: Float,
    val elapsedMs: Long,
    val mistakes: Int,
)

/**
 * The game library — Requirement 16 criterion 2. Layout is exactly the Home spec in
 * component-specs.md.
 *
 * Everything here comes from `GameRegistry`. Adding a game to that list makes it appear
 * with no change to this file, which is the whole point of the game module pattern.
 *
 * Note the composition: **one** display element, and group separation by `space32` or larger
 * rather than by any divider line.
 *
 * Insets are top and sides only. The bottom belongs to `NothingBottomNav`, which consumes the
 * navigation-bar inset itself — padding it in both places would leave a dead band above the
 * bar. See `NothingGamesApp`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    games: List<GameDefinition>,
    onGameSelected: (String) -> Unit,
    onPlay: (String) -> Unit = onGameSelected,
    modifier: Modifier = Modifier,
    standalone: GameDefinition? = null,
    resume: ResumeState? = null,
    resumes: List<ResumeState> = listOfNotNull(resume),
    activeDailies: Set<String> = emptySet(),
    onRules: (() -> Unit)? = null,
) {
    NothingBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            val effectiveResumes = resumes.ifEmpty { listOfNotNull(resume) }

            // Whether the fixed bottom button exists below. The rules link lives under
            // it when it does; otherwise it is the last item of the scrollable content,
            // so exactly one link renders either way.
            val playGameId = standalone?.id ?: effectiveResumes.singleOrNull()?.gameId

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .pad(horizontal = NothingSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.pad(top = NothingSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
                ) {
                    Label(if (standalone != null) "no ads studio" else "no ads")
                    NothingText(
                        text = (standalone?.name ?: "studio").uppercase(),
                        role = TextRole.Display,
                        style = NothingTheme.typography.displayMedium,
                    )
                    if (standalone != null) {
                        Label(standalone.description, role = TextRole.Disabled)
                    }
                }

                // ── In progress ── Nothing language: whitespace separates groups (S8),
                // cards are stacked with md gaps; when there are several, they become a
                // horizontal slide with a dot readout. Dots always encode information (G1)
                // — here "which of N" — Canvas-drawn (G2) on a spacing-token pitch (G3).
                // Single-card keeps the simple centred card; multi-card becomes a pager
                // so vertical length stays bounded and swipe + dots signal "more".
                if (effectiveResumes.isNotEmpty()) {
                    if (effectiveResumes.size == 1) {
                        Column(
                            modifier = Modifier.pad(top = NothingSpacing.hero),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val item = effectiveResumes.first()
                            ResumeCard(
                                resume = item,
                                gameName = if (standalone == null) {
                                    games.firstOrNull { it.id == item.gameId }?.name
                                } else {
                                    null
                                },
                                onClick = { onGameSelected(item.gameId) },
                                showHeader = true,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.pad(top = NothingSpacing.hero),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                        ) {
                            Label("in progress · ${effectiveResumes.size}")
                            val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                                pageCount = { effectiveResumes.size },
                            )
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                            ) { page ->
                                val item = effectiveResumes[page]
                                // Horizontal padding inside the page keeps the card off the
                                // screen edge without a literal width (S9); card itself remains
                                // fillMaxWidth inside the page.
                                // Slightly smaller card — lg inset (24dp each side) peeks
                                // the neighbour and signals swipe without a literal width.
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pad(horizontal = NothingSpacing.lg),
                                ) {
                                    ResumeCard(
                                        resume = item,
                                        gameName = if (standalone == null) {
                                            games.firstOrNull { it.id == item.gameId }?.name
                                        } else {
                                            null
                                        },
                                        onClick = { onGameSelected(item.gameId) },
                                        showHeader = false,
                                    )
                                }
                            }
                            SlideDotIndicator(
                                total = effectiveResumes.size,
                                current = pagerState.currentPage,
                                modifier = Modifier.pad(top = NothingSpacing.sm),
                            )
                        }
                    }
                }

                if (standalone != null) {
                    // No inline play button — the fixed-bottom one below handles it.
                } else {
                    Column(
                        modifier = Modifier.pad(top = NothingSpacing.xl),
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        if (resume != null) {
                            Label("all games")
                        }
                        games.forEach { game ->
                            GameCard(
                                game = game,
                                dailyInProgress = game.id in activeDailies,
                                onClick = { onGameSelected(game.id) },
                            )
                        }
                        // Library with no fixed button below: the rules link closes the page.
                        if (onRules != null && playGameId == null) {
                            RulesLink(onClick = onRules)
                        }
                    }
                }

                Column(modifier = Modifier.pad(bottom = NothingSpacing.xl)) {}
            }

            // Fixed PLAY button at the bottom center, above the bottom navigation.
            // With several resumes the card itself is the affordance; the fixed button
            // would be ambiguous (which of the three would it continue?). So it appears
            // only for the single-resume or standalone case where "play" has one answer.
            if (playGameId != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                            )
                        )
                        .pad(NothingSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NothingButton(
                        text = "play",
                        onClick = { onPlay(playGameId) },
                    )
                    if (onRules != null) {
                        RulesLink(
                            onClick = onRules,
                            modifier = Modifier.pad(top = NothingSpacing.sm),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The quiet route into the rules page — words only, no pill.
 *
 * The top bar's BACK/NEW affordances set the precedent: an action can be a bare
 * clickable label. That is the volume this link is supposed to have — present, but
 * not a second button competing with PLAY. D26's original refusal was that nobody
 * reads a tutorial; the amendment keeps the door open without making it loud.
 */
@Composable
private fun RulesLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberNothingHaptics()
    Box(
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null, // our own feedback; no Material ripple
            role = Role.Button,
        ) {
            haptics.perform(HapticEvent.Tick)
            onClick()
        },
    ) {
        Label("how to play", role = TextRole.Primary)
    }
}

/**
 * The saved game, as a thing you can read rather than a button you have to trust.
 *
 * This replaced a lone button whose whole vocabulary was the word `CONTINUE` versus `PLAY`.
 * That told you a save existed and nothing else — not which puzzle, not how far in, not
 * whether it was the quick one you nearly finished or the hard one you abandoned. Those are
 * exactly the facts that decide whether you tap it.
 *
 * It is the `nothing-study.md` §1 argument at small scale: the state was already there, and
 * showing it costs one card. Three metrics and no more, for the same reason the game screen
 * shows three readouts (decision D13) — a curated view, not a state dump.
 *
 * The whole card is the target rather than a nested button. A button inside a clickable card
 * gives two overlapping hit areas for one action, and the card is the bigger, more forgiving
 * one.
 */
@Composable
private fun ResumeCard(
    resume: ResumeState,
    gameName: String?,
    onClick: () -> Unit,
    showHeader: Boolean = true,
) {
    NothingCard(onClick = onClick) {
        if (showHeader) Label("in progress")

        if (gameName != null) {
            NothingText(text = gameName, role = TextRole.Primary)
        }

        Label(
            text = listOf(
                resume.difficulty,
                "${(resume.progress * 100).toInt()}% done",
                formatDuration(resume.elapsedMs),
            ).joinToString(" · "),
            role = TextRole.Disabled,
        )

        Label("continue", role = TextRole.Primary)
    }
}

@Composable
private fun GameCard(
    game: GameDefinition,
    onClick: () -> Unit,
    dailyInProgress: Boolean = false,
) {
    NothingCard(onClick = onClick) {
        when (val icon = game.icon) {
            is GameIcon.DotMatrix -> GameIconGlyph(
                rows = icon.rows,
                role = TextRole.Secondary,
            )

            is GameIcon.Letter -> NothingText(
                text = icon.char.toString(),
                role = TextRole.Secondary,
                style = NothingTheme.typography.displaySmall,
            )
        }

        NothingText(text = game.name, role = TextRole.Primary)
        Label(text = game.description)

        // The day's puzzle, visible without opening the picker (decision D32's one
        // visibility gap). Words, not a dot or a pill: a pill would be a second button
        // inside a clickable card, and a dot here would carry no quantity (G1). The
        // key-presence signal in SessionStore is the whole truth — the slot exists only
        // while a daily run is unfinished.
        if (dailyInProgress) {
            Label("daily in progress", role = TextRole.Primary)
        }
    }
}
