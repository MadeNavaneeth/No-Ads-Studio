package com.example.lightapp.games.connect

import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.ConnectCell
import com.example.lightapp.designsystem.components.ConnectCellValue
import com.example.lightapp.designsystem.components.ConnectSide
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
 * `connect(7)` — route-drawing in this language.
 *
 * Three readouts and no more (decision D13): time, completion, lines joined. **There is
 * no red on this screen, ever** — every move is retractable, so there is nothing to be
 * wrong about (method §6), and the run ends one way: solved.
 *
 * The drag lives on the grid, not the cells, exactly as in nonogram: cells own taps,
 * the drag claims the gesture only once touch slop is passed. Segments are laid as the
 * walk passes, so lifting the finger anywhere commits what was drawn.
 */
@Composable
fun ConnectScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    val viewModel: ConnectViewModel = viewModel { ConnectViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptics = rememberNothingHaptics()

    // Gameplay preferences live in the store and are read here, at their one call
    // site, rather than threaded down through the shell.
    val showTimer by store.showTimer.collectAsStateWithLifecycle(initialValue = true)

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
        if (requested != null) viewModel.generate(requested)
    }

    NothingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NothingTopBar(
                title = "connect(7)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { viewModel.showDifficultyPicker() },
            )

            when (val current = ui) {
                ConnectUi.Generating -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("generating")
                        DotPulse()
                    }
                }

                ConnectUi.GenerationFailed -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        Label("generation failed")
                        NothingButton(
                            text = "retry",
                            onClick = { viewModel.retryGeneration() },
                        )
                    }
                }

                is ConnectUi.ChoosingDifficulty -> DifficultyPicker(
                    // DAILY leads (decision D32) — the day's puzzle, not a difficulty.
                    options = listOf(ConnectViewModel.DAILY_NAME) + ConnectViewModel.DIFFICULTIES,
                    current = current.currentDifficulty,
                    onSelect = { viewModel.generate(it) },
                    onCancel = { viewModel.cancelDifficultyPicker() },
                )

                is ConnectUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    showTimer = showTimer,
                    onWalkStart = { haptics.perform(HapticEvent.Tick) },
                    onDrag = { index -> viewModel.dragTo(index) },
                    onPress = { index -> viewModel.press(index) },
                    onWalkEnd = { viewModel.endWalk() },
                    onUndo = { viewModel.undo() },
                )

                is ConnectUi.Complete -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        NothingText(
                            text = formatTime(current.state.elapsedMs),
                            role = TextRole.Display,
                            style = NothingTheme.typography.displayMedium,
                        )
                        Label("solved · ${current.state.difficulty.lowercase()}")
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

@Composable
private fun Board(
    state: ConnectState,
    undoAvailable: Boolean,
    showTimer: Boolean,
    onWalkStart: () -> Unit,
    onDrag: (Int) -> Unit,
    onPress: (Int) -> Unit,
    onWalkEnd: () -> Unit,
    onUndo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // A 7×7 board overflows a landscape phone or a large font scale — the
            // alternative is controls the player cannot reach.
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        Readouts(state, showTimer)

        var gridPx by remember { mutableFloatStateOf(0f) }
        // The drag block below outlives the composition that created it, so it must
        // read the board through this rather than the composition-time `state` —
        // otherwise a walk laid by an earlier drag step would still read as absent.
        val latestCells by rememberUpdatedState(state.cells)

        fun indexAt(offset: Offset): Int? {
            if (gridPx <= 0f) return null
            val cell = gridPx / ConnectRules.SIZE
            val c = (offset.x / cell).toInt()
            val r = (offset.y / cell).toInt()
            if (c !in 0 until ConnectRules.SIZE || r !in 0 until ConnectRules.SIZE) return null
            return r * ConnectRules.SIZE + c
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // The grid owns the drag gesture. Cells own taps: the drag only claims the
            // gesture once touch slop is passed, so single touches always reach the
            // cell and walks always reach the grid.
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { gridPx = it.width.toFloat() }
                    .pointerInput(Unit) {
                        while (true) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val origin = indexAt(offset) ?: return@detectDragGestures
                                    if (latestCells[origin] != ConnectRules.EMPTY) {
                                        onWalkStart()
                                        onPress(origin)
                                    }
                                },
                                onDrag = { change, _ ->
                                    indexAt(change.position)?.let(onDrag)
                                    change.consume()
                                },
                                onDragEnd = onWalkEnd,
                                onDragCancel = onWalkEnd,
                            )
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (r in 0 until ConnectRules.SIZE) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until ConnectRules.SIZE) {
                                val index = r * ConnectRules.SIZE + c
                                val value = state.cells[index]
                                val pair = ConnectRules.endpointPair(value)
                                    ?: ConnectRules.pathPair(value)
                                ConnectCell(
                                    value = when {
                                        ConnectRules.isEndpoint(value) -> ConnectCellValue.Endpoint
                                        ConnectRules.isPath(value) -> ConnectCellValue.Path
                                        else -> ConnectCellValue.Empty
                                    },
                                    pair = pair,
                                    connections = channels(latestCells, index),
                                    selected = state.path.lastOrNull() == index,
                                    onTap = { onPress(index) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
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
            Label("drag between numbers", role = TextRole.Disabled)
        }
    }
}

/**
 * Which sides a cell's route continues toward: every orthogonally adjacent cell holding
 * the *same pair* — an endpoint connects into its own laid segments, a segment into its
 * neighbours on the same route. The same-value-only rule is what keeps two adjacent
 * routes readable without colour (spec: `ConnectCell`).
 */
private fun channels(cells: List<Int>, index: Int): Set<ConnectSide> {
    val value = cells[index]
    val pair = ConnectRules.endpointPair(value) ?: ConnectRules.pathPair(value) ?: return emptySet()
    val out = HashSet<ConnectSide>()
    for (n in ConnectRules.neighbours(index)) {
        val nPair = ConnectRules.endpointPair(cells[n]) ?: ConnectRules.pathPair(cells[n])
        if (nPair == pair) {
            out += when (n) {
                index - ConnectRules.SIZE -> ConnectSide.North
                index + ConnectRules.SIZE -> ConnectSide.South
                index - 1 -> ConnectSide.West
                else -> ConnectSide.East
            }
        }
    }
    return out
}

/** Rule M10: exactly three readout calls in this file. A fourth one fails the build. */
@Composable
private fun Readouts(state: ConnectState, showTimer: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Readout("time", if (showTimer) formatTime(state.elapsedMs) else "--:--")
        Readout("done", "${(state.completion * 100).toInt()}%")
        Readout("line", "${state.solvedLines}/${state.totalPairs}")
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
