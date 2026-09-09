package com.example.lightapp.games.binairo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.BinairoCell
import com.example.lightapp.designsystem.components.BinairoCellValue
import com.example.lightapp.designsystem.components.ButtonEmphasis
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.DotPulse
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics
import com.example.lightapp.studio.persistence.SessionStore

/**
 * `binairo(10)` — the binary board in this language.
 *
 * Three readouts (decision D13): time, the share of undecided cells the player has
 * settled, and the rows plus columns fully decided. The grid carries the rest of the
 * story — ONEs as solid dots, ZEROs as hollow rings — so the board is the game's own
 * subject: a binary dot field. No background readout, the nonogram and akari ruling:
 * the grid *is* the dot matrix, and a second one behind it would compete.
 *
 * **There is no red on this screen, ever.** A run of three is three dots in a row —
 * visible on the board itself, found by contradiction rather than announced
 * (method §6), so the game spends no red anywhere.
 */
@Composable
fun BinairoScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    val viewModel: BinairoViewModel = viewModel { BinairoViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptics = rememberNothingHaptics()

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { viewModel.onScreenPaused() }
    }

    // Fires once per finished run. Collected as an event, not derived from the
    // Complete state, which would repeat every time this screen came back into view.
    LaunchedEffect(viewModel) {
        viewModel.completions.collect { result ->
            haptics.perform(HapticEvent.Complete)
            onComplete(result)
        }
    }

    // A difficulty carried by navigation means "start new here" — the shell's
    // new-game picker. Consumed once; later recompositions must not restart the run.
    val requested = remember { initialDifficulty }
    LaunchedEffect(requested) {
        if (requested != null) viewModel.generate(requested)
    }

    // The in-game NEW picker (decision D18 as amended): DAILY leads, then the
    // difficulties. Held as screen state — the run continues underneath, and a
    // dismissal returns to it exactly as the other games' pickers do.
    var pickerVisible by remember { mutableStateOf(false) }

    NothingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                ),
        ) {
            NothingTopBar(
                title = "binairo(10)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { pickerVisible = true },
            )

            if (pickerVisible) {
                DifficultyPicker(
                    // DAILY leads (decision D32) — the day's deal, not a difficulty.
                    options = listOf(BinairoViewModel.DAILY_NAME) + BinairoGenerator.DIFFICULTIES,
                    current = (ui as? BinairoUi.Playing)?.state?.difficulty,
                    onSelect = {
                        pickerVisible = false
                        viewModel.generate(it)
                    },
                    onCancel = { pickerVisible = false },
                )
            } else when (val current = ui) {
                BinairoUi.Generating -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("deciding")
                        DotPulse()
                    }
                }

                BinairoUi.GenerationFailed -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("no logical puzzle found — try again")
                        NothingButton(
                            text = "retry",
                            onClick = viewModel::retryGeneration,
                        )
                    }
                }

                is BinairoUi.ChoosingDifficulty -> Unit // handled above; unreachable here

                is BinairoUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    onUndo = viewModel::undo,
                    onTap = { index ->
                        viewModel.tap(index)
                        haptics.perform(HapticEvent.Tick)
                    },
                )

                is BinairoUi.Complete -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        NothingText(
                            text = "solved",
                            role = TextRole.Display,
                            style = NothingTheme.typography.displayMedium,
                        )
                        Label(
                            if (current.state.dailyDay != null) "all lines balanced · daily"
                            else "all lines balanced · ${current.state.difficulty.lowercase()}",
                        )
                        NothingButton(
                            text = "again",
                            onClick = { viewModel.generate(current.state.difficulty) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/**
 * The playing board: three readouts, the grid, one control. The grid owns the gap —
 * rows and columns spaced at `xs`, cells carry no padding of their own, the same
 * construction as every sibling board.
 */
@Composable
private fun Board(
    state: BinairoState,
    undoAvailable: Boolean,
    onUndo: () -> Unit,
    onTap: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // The readouts size to their content and space evenly — akari's
            // exact row construction; the token scale does not dissolve past md.
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Readout(
                label = "time",
                text = formatElapsed(state.elapsedMs),
            )
            Readout(
                label = "done",
                text = "${(state.completion * 100).toInt()}%",
            )
            Readout(
                label = "line",
                text = "${state.linesSolved}/${BinairoRules.LINE_COUNT}",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pad(horizontal = NothingSpacing.xs),
            verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
        ) {
            for (r in 0 until BinairoRules.SIZE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
                ) {
                    for (c in 0 until BinairoRules.SIZE) {
                        val index = r * BinairoRules.SIZE + c
                        val value = state.merged[index]
                        val display = when (value) {
                            BinairoRules.ONE -> BinairoCellValue.One
                            BinairoRules.ZERO -> BinairoCellValue.Zero
                            else -> BinairoCellValue.Undecided
                        }
                        BinairoCell(
                            value = display,
                            given = state.isGiven(index),
                            onTap = { onTap(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NothingButton(
                text = "undo",
                emphasis = ButtonEmphasis.Quiet,
                enabled = undoAvailable,
                onClick = onUndo,
            )
        }
    }
}

/** Three readouts and no more (decision D13): a label over its data. */
@Composable
private fun Readout(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs)) {
        Label(label)
        NothingText(
            text = text,
            role = TextRole.Primary,
            style = NothingTheme.typography.data,
        )
    }
}

/** The clock's readout, minutes over padded seconds — the sibling format. */
private fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}