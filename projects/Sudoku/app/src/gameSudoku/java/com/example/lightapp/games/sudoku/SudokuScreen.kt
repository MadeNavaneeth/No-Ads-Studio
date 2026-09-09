package com.example.lightapp.games.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.DotMatrixReadout
import com.example.lightapp.designsystem.components.DotPitch
import com.example.lightapp.designsystem.components.DotPulse
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.components.NumberPad
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics
import com.example.lightapp.studio.formatDuration
import com.example.lightapp.studio.persistence.SessionStore

/**
 * `sudoku(9)` — layout is the game screen spec in component-specs.md.
 *
 * Three readouts and no more (decision D13): time, completion, mistakes. Nothing's
 * transparency is art-directed, not a state dump — three chosen well beat twelve dumped
 * honestly. That is the iFixit correction in nothing-study.md §11.
 *
 * **In normal play there is no red on this screen.** Decision D2.
 *
 * ## Layout constraints worth knowing
 *
 * The board is a square that fills the width, so the screen's height requirement is
 * roughly `width + 300dp`. On a short phone, in landscape, or at a large font scale that
 * exceeds the viewport, hence the scroll: the alternative is a number pad the player
 * cannot reach. Insets are applied to this content while [NothingBackground] and the dot
 * matrix behind it stay full-bleed.
 */
@Composable
fun SudokuScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    val viewModel: SudokuViewModel = viewModel { SudokuViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // A difficulty carried by navigation means "start new here" — the shell's
    // new-game picker. Consumed once: later recompositions must not regenerate.
    // On a fresh view model this follows the init restore by one fast generation;
    // generate() cancels the in-flight work, so the waste is bounded and small.
    val requested = remember { initialDifficulty }
    LaunchedEffect(requested) {
        if (requested != null) viewModel.generate(requested)
    }
    val haptics = rememberNothingHaptics()

    // Read straight from the store rather than threaded down through the shell. These are
    // gameplay preferences, so the game is the right owner — and the haptics setting already
    // demonstrated what happens when a preference has to survive a long parameter chain to
    // reach its one call site: it silently did not arrive.
    val showRemaining by store.showRemaining.collectAsStateWithLifecycle(initialValue = true)
    val showTimer by store.showTimer.collectAsStateWithLifecycle(initialValue = true)
    val showPeers by store.showPeers.collectAsStateWithLifecycle(initialValue = true)
    val mistakeLimit by store.mistakeLimit.collectAsStateWithLifecycle(initialValue = 0)

    // The clock counts play time, so it follows the resumed lifecycle rather than
    // composition. Requirement 16 criterion 9 — in-progress state is persisted before
    // leaving — is handled on the pause side, which also covers the app being swiped away
    // without the screen ever being disposed.
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { viewModel.onScreenPaused() }
    }

    // Fires once per solve. Collected as an event, not derived from the Complete state,
    // which would repeat every time this screen came back into view.
    LaunchedEffect(viewModel) {
        viewModel.completions.collect { result ->
            haptics.perform(HapticEvent.Complete)
            onComplete(result)
        }
    }

    NothingBackground {
        // The Glyph readout sits behind everything, bound to completion — never
        // decorative. Decision D12. Deliberately outside the insets padding: the texture
        // reaches the screen edges, only the content is inset. Wide pitch keeps the
        // texture from competing with the 81-cell grid that sits in front of it.
        DotMatrixReadout(
            progress = (ui as? SudokuUi.Playing)?.state?.completion
                ?: (ui as? SudokuUi.Complete)?.state?.completion
                ?: 0f,
            pitch = DotPitch.Wide,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NothingTopBar(
                title = "sudoku",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { viewModel.showDifficultyPicker() },
            )

            when (val current = ui) {
                SudokuUi.Generating -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("generating")
                        DotPulse()
                    }
                }

                SudokuUi.GenerationFailed -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        Label("generation failed")
                        NothingButton(
                            text = "retry",
                            // Retry reproduces the original request — a failed daily
                            // generation must retry the same day's board, not wander
                            // into a regular Moderate game.
                            onClick = { viewModel.retryGeneration() },
                        )
                    }
                }

                is SudokuUi.ChoosingDifficulty -> DifficultyPicker(
                    // The day's puzzle leads: it is the one option that is not a
                    // difficulty, and it is the newest surface. The component stays
                    // generic — it is handed a list of strings.
                    options = listOf(SudokuViewModel.DAILY_NAME) +
                        SudokuViewModel.DIFFICULTIES.map { it.name },
                    current = current.currentDifficulty,
                    onSelect = { viewModel.generate(it) },
                    onCancel = { viewModel.cancelDifficultyPicker() },
                )

                is SudokuUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    showRemaining = showRemaining,
                    showTimer = showTimer,
                    showPeers = showPeers,
                    mistakeLimit = mistakeLimit,
                    hint = null,
                    onCellClick = viewModel::select,
                    onDigit = { digit ->
                        // The only haptic for a digit: NumberPad deliberately fires none,
                        // because only this call site knows whether the entry conflicted,
                        // and decision D15 wants those to feel different.
                        val conflicted = viewModel.enter(digit)
                        haptics.perform(
                            if (conflicted) HapticEvent.Conflict else HapticEvent.Tick
                        )
                    },
                    onErase = viewModel::erase,
                    onUndo = viewModel::undo,
                    onToggleNotes = viewModel::toggleNotesMode,
                    onHint = viewModel::hint,
                )

                is SudokuUi.Hinting -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    showRemaining = showRemaining,
                    showTimer = showTimer,
                    showPeers = showPeers,
                    mistakeLimit = mistakeLimit,
                    hint = current.hint,
                    onCellClick = viewModel::select,
                    onDigit = { digit ->
                        // Placing the hinted digit straight off the overlay is the flow
                        // the hint exists for; the commit dismisses it as a side effect.
                        val conflicted = viewModel.enter(digit)
                        haptics.perform(
                            if (conflicted) HapticEvent.Conflict else HapticEvent.Tick
                        )
                    },
                    onErase = viewModel::erase,
                    onUndo = viewModel::undo,
                    onToggleNotes = viewModel::toggleNotesMode,
                    onHint = viewModel::hint,
                )

                is SudokuUi.Complete -> {
                    val solved = current.state.isComplete
                    Centred {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                        ) {
                            NothingText(
                                text = formatTime(current.state.elapsedMs),
                                role = TextRole.Display,
                                style = NothingTheme.typography.displayMedium,
                            )
                            Label(
                                if (solved) "solved · ${current.state.difficulty.lowercase()}"
                                else "failed · ${current.state.difficulty.lowercase()} · ${current.state.mistakes}/3",
                            )
                            // Same Glyph breath as generating — instrument confirms the end,
                            // not a trophy. No scale, no bounce (S13).
                            DotPulse()
                            NothingButton(
                                text = "new game",
                                onClick = { viewModel.showDifficultyPicker() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Board(
    state: SudokuState,
    undoAvailable: Boolean,
    showRemaining: Boolean,
    showTimer: Boolean,
    showPeers: Boolean,
    mistakeLimit: Int,
    hint: Hint?,
    onCellClick: (Int) -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The board is as tall as it is wide, so readouts + board + pad can exceed a
            // short viewport. Scrolling keeps the pad reachable instead of clipping it.
            // Vertical breathing room is xl (32dp) — the same token that separates groups
            // on this screen (S8), so the scroll edge feels like part of the composition.
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.xl),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        Readouts(state, showTimer = showTimer, mistakeLimit = mistakeLimit)

        // Green for the number of 1's etc. — when a digit is selected, its key
        // and its remaining badge pick up the chosen accent so the count reads as
        // part of the highlight system, not as a separate badge.
        val selectedDigit = state.selected?.let { state.valueAt(it).takeIf { v -> v in 1..9 } }

        // The board is the hero, and it is *just* the board — no panel, no frame around
        // it. The 81 cells float directly on the background (Requirement 11 criterion 8:
        // no divider lines), and 3×3 structure is spacing rhythm alone: boxes `space8`
        // apart, cells `space4` within one (see SudokuGrid). The dot matrix behind stays
        // full-bleed, so the grid reads as cut from the page rather than mounted on it.
        // The hint names its deduction in one line; the board does the showing —
        // the hinted cell arrives selected, so the cross highlight lights the very
        // row, column and box that force the digit. Demystification, not a spoiler:
        // the player sees the mechanism and places the number themselves.
        if (hint != null) {
            Label(
                "hint · r${hint.index / Conflicts.SIZE + 1}c${hint.index % Conflicts.SIZE + 1} is ${hint.digit} — its row, column and box hold the rest",
                role = TextRole.Secondary,
            )
        }

        SudokuGrid(state = state, onCellClick = onCellClick, showPeers = showPeers)

        NumberPad(
            onDigit = onDigit,
            onErase = onErase,
            onToggleNotes = onToggleNotes,
            onUndo = onUndo,
            onHint = onHint,
            notesActive = state.notesMode,
            undoAvailable = undoAvailable,
            remaining = state.remaining,
            selectedDigit = selectedDigit,
            showRemaining = showRemaining,
        )
    }
}

/**
 * Three readouts, and never a fourth. Decision D13 — curated, not a state dump.
 *
 * `D13-three-readouts` counts `Readout("` calls in this file and fails the build on the
 * fourth, so the limit is a gate rather than an intention. When the clock is hidden its slot
 * shows a dash instead of vanishing: a row that changes from three columns to two shifts the
 * other two, and a readout that moves is worse than one that reads `—`.
 */
@Composable
private fun Readouts(state: SudokuState, showTimer: Boolean, mistakeLimit: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Readout("time", if (showTimer) formatTime(state.elapsedMs) else "—")
        Readout("done", "${(state.completion * 100).toInt()}%")
        val missText = if (mistakeLimit > 0) "${state.mistakes}/$mistakeLimit" else state.mistakes.toString()
        Readout("miss", missText)
    }
}

@Composable
private fun Readout(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
    ) {
        Label(label)
        // Tabular figures, so the value does not jitter as it counts.
        NothingText(
            text = value,
            role = TextRole.Primary,
            style = NothingTheme.typography.data,
        )
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

// The timer and the win display both go through the shared formatter in
// studio/TimeFormat.kt. The private version here stopped at 59:59 and then silently wrapped,
// which a long session would have shown as a wrong time.
private fun formatTime(millis: Long): String = formatDuration(millis)
