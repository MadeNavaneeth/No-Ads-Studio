package com.example.lightapp.games.minesweeper

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.MineCellValue
import com.example.lightapp.designsystem.components.MinesweeperCell
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
import com.example.lightapp.studio.persistence.MinesweeperSessionStore

/**
 * `minesweeper(10)` — the minefield in this language.
 *
 * Three readouts and no more (decision D13): time, completion, mines not yet flagged.
 * There is deliberately no [DotMatrixReadout] behind the grid — the covered field is
 * itself a field of hidden state, and a second dot layer behind it would compete
 * (method §5).
 *
 * **Red appears here, exactly once per lost run.** The detonated cell is the game's
 * one red budget (method §6) and the first live in-play use in the studio: it is
 * terminal, never present during normal play, and always paired with the completion
 * screen naming the loss in words, so the state survives the grayscale test.
 */
@Composable
fun MinesweeperScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { MinesweeperSessionStore(context.applicationContext) }
    val viewModel: MinesweeperViewModel = viewModel { MinesweeperViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptics = rememberNothingHaptics()

    // Gameplay preferences live in the store and are read here, at their one call
    // site, rather than threaded down through the shell.
    val showRemaining by store.showRemaining.collectAsStateWithLifecycle(initialValue = true)
    val showTimer by store.showTimer.collectAsStateWithLifecycle(initialValue = true)
    val showPeers by store.showPeers.collectAsStateWithLifecycle(initialValue = true)

    // The clock counts play time, so it follows the resumed lifecycle rather than
    // composition — which also covers the app being swiped away without the screen
    // ever being disposed.
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
    // new-game picker. Consumed once; later recompositions must not regenerate.
    val requested = remember { initialDifficulty }
    LaunchedEffect(requested) {
        if (requested != null) viewModel.newGame(requested)
    }

    NothingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NothingTopBar(
                title = "minesweeper(10)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { viewModel.showDifficultyPicker() },
            )

            when (val current = ui) {
                is MinesweeperUi.ChoosingDifficulty -> DifficultyPicker(
                    // DAILY leads (decision D32) — the day's puzzle, not a difficulty.
                    options = listOf(MinesweeperViewModel.DAILY_NAME) + MinesweeperViewModel.DIFFICULTIES,
                    current = current.currentDifficulty,
                    onSelect = { viewModel.newGame(it) },
                    onCancel = { viewModel.cancelDifficultyPicker() },
                )

                is MinesweeperUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    showRemaining = showRemaining,
                    showTimer = showTimer,
                    showPeers = showPeers,
                    onPress = { index ->
                        // The only haptic for a reveal: only this call site knows
                        // whether it was the mine, and those feel different.
                        val detonated = viewModel.press(index)
                        haptics.perform(
                            if (detonated) HapticEvent.Conflict else HapticEvent.Tick
                        )
                    },
                    onMark = { index ->
                        viewModel.mark(index)
                        haptics.perform(HapticEvent.Tick)
                    },
                    onUndo = { viewModel.undo() },
                )

                is MinesweeperUi.Complete -> {
                    val cleared = current.state.isComplete
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
                                if (cleared) "cleared · ${current.state.difficulty.lowercase()}"
                                else "mine detonated · ${current.state.difficulty.lowercase()}"
                            )
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
    state: MinesweeperState,
    undoAvailable: Boolean,
    showRemaining: Boolean,
    showTimer: Boolean,
    showPeers: Boolean,
    onPress: (Int) -> Unit,
    onMark: (Int) -> Unit,
    onUndo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // A 9×9 board overflows a short viewport, a landscape phone, or a large
            // font scale — the alternative is controls the player cannot reach.
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        Readouts(state, showTimer, showRemaining)

        Column(modifier = Modifier.fillMaxWidth()) {
            for (r in 0 until MinesweeperRules.SIZE) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until MinesweeperRules.SIZE) {
                        val index = r * MinesweeperRules.SIZE + c
                        val revealed = state.cells[index] == MinesweeperRules.REVEALED
                        val detonated = state.cells[index] == MinesweeperRules.DETONATED
                        MinesweeperCell(
                            value = when (state.cells[index]) {
                                MinesweeperRules.REVEALED -> MineCellValue.Revealed
                                MinesweeperRules.FLAGGED -> MineCellValue.Flagged
                                MinesweeperRules.DETONATED -> MineCellValue.Detonated
                                else -> MineCellValue.Unknown
                            },
                            count = if (revealed || detonated) state.adjacency[index] else 0,
                            selected = showPeers && state.selected == index,
                            onTap = { onPress(index) },
                            onLongPress = { onMark(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
        ) {
            NothingButton(
                text = "undo",
                onClick = onUndo,
                enabled = undoAvailable,
            )
            Label("tap reveals · hold flags", role = TextRole.Disabled)
        }
    }
}

/** Rule M10: exactly three readout calls in this file. A fourth one fails the build. */
@Composable
private fun Readouts(state: MinesweeperState, showTimer: Boolean, showRemaining: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Readout("time", if (showTimer) formatTime(state.elapsedMs) else "--:--")
        Readout("done", "${(state.completion * 100).toInt()}%")
        Readout(
            "mine",
            if (showRemaining) state.minesRemaining.toString() else "--",
        )
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
        NothingText(text = value, role = TextRole.Primary, style = NothingTheme.typography.data)
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
