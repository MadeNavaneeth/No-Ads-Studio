package com.example.lightapp.games.akari

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
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.AkariCell
import com.example.lightapp.designsystem.components.AkariCellValue
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
import com.example.lightapp.studio.persistence.AkariSessionStore

/**
 * `akari(10)` — the light-up board in this language.
 *
 * Two readouts and no more (decision D13): time, and the share of white cells lit.
 * The grid carries the rest of the story — walls as voids, lit ground raised, bulbs
 * as dots — so a third readout would narrate what the board already says (method §5).
 *
 * **There is no red on this screen, ever.** A clash is two lit bulbs; an
 * over-numbered wall is a numeral over too many adjacent dots. Both are visible
 * contradictions on the board itself, found by contradiction rather than announced
 * (method §6), so the game spends no red anywhere.
 */
@Composable
fun AkariScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { AkariSessionStore(context.applicationContext) }
    val viewModel: AkariViewModel = viewModel { AkariViewModel(store) }
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
                title = "akari(10)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { pickerVisible = true },
            )

            if (pickerVisible) {
                DifficultyPicker(
                    // DAILY leads (decision D32) — the day's deal, not a difficulty.
                    options = listOf(AkariViewModel.DAILY_NAME) + AkariGenerator.DIFFICULTIES,
                    current = (ui as? AkariUi.Playing)?.state?.difficulty,
                    onSelect = {
                        pickerVisible = false
                        viewModel.generate(it)
                    },
                    onCancel = { pickerVisible = false },
                )
            } else when (val current = ui) {
                AkariUi.Generating -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("lighting")
                        DotPulse()
                    }
                }

                AkariUi.GenerationFailed -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("no unique puzzle found — try again")
                        NothingButton(
                            text = "retry",
                            onClick = viewModel::retryGeneration,
                        )
                    }
                }

                is AkariUi.ChoosingDifficulty -> Unit // handled above; unreachable here

                is AkariUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    onUndo = viewModel::undo,
                    onTap = { index ->
                        viewModel.tap(index)
                        haptics.perform(HapticEvent.Tick)
                    },
                )

                is AkariUi.Complete -> Centred {
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
                            if (current.state.dailyDay != null) "all lit · daily"
                            else "all lit · ${current.state.difficulty.lowercase()}",
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
 * The playing board: two readouts, the grid, one control. The grid owns the gap —
 * rows and columns spaced at `xs`, cells carry no padding of their own, the same
 * construction as every sibling board.
 */
@Composable
private fun Board(
    state: AkariState,
    undoAvailable: Boolean,
    onUndo: () -> Unit,
    onTap: (Int) -> Unit,
) {
    val lit = remember(state.cells) { AkariRules.litCells(state.cells) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // The readouts size to their content and space evenly — minesweeper's
            // exact row construction; the token scale does not dissolve past md.
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Readout(
                label = "time",
                text = formatElapsed(state.elapsedMs),
            )
            Readout(
                label = "lit",
                text = "${(state.completion * 100).toInt()}%",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
        ) {
            for (r in 0 until AkariRules.SIZE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
                ) {
                    for (c in 0 until AkariRules.SIZE) {
                        val index = r * AkariRules.SIZE + c
                        val value = state.cells[index]
                        val display = when {
                            value == AkariRules.BULB -> AkariCellValue.Bulb
                            value == AkariRules.WALL -> AkariCellValue.Wall
                            AkariRules.clueNumber(value) != null -> AkariCellValue.Clue
                            lit[index] -> AkariCellValue.Lit
                            else -> AkariCellValue.Dark
                        }
                        AkariCell(
                            value = display,
                            clue = AkariRules.clueNumber(value) ?: 0,
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

/** Two readouts and no more (decision D13): a label over its data. */
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
