package com.example.lightapp.games.nonogram

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.DotPulse
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NonoCellValue
import com.example.lightapp.designsystem.components.NonogramCell
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
import com.example.lightapp.studio.persistence.NonogramSessionStore

/**
 * `nonogram(10)` — a picture-logic board in this language.
 *
 * Three readouts and no more (decision D13): time, completion, solved lines. The grid
 * itself is the dot matrix, so there is deliberately no [DotMatrixReadout] behind it —
 * a second dot field would compete with the one the player is filling (method §5).
 *
 * **There is no red on this screen, ever.** A wrong fill is found by contradiction,
 * not announced (method §6) — not even the lives limit spends red; the run simply
 * ends in words.
 */
@Composable
fun NonogramScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { NonogramSessionStore(context.applicationContext) }
    val viewModel: NonogramViewModel = viewModel { NonogramViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptics = rememberNothingHaptics()

    // Gameplay preferences live in the store and are read here, at their one call
    // site, rather than threaded down through the shell.
    val showRemaining by store.showRemaining.collectAsStateWithLifecycle(initialValue = true)
    val showTimer by store.showTimer.collectAsStateWithLifecycle(initialValue = true)
    val showPeers by store.showPeers.collectAsStateWithLifecycle(initialValue = true)
    val mistakeLimit by store.mistakeLimit.collectAsStateWithLifecycle(initialValue = 0)

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
                title = "nonogram(10)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { viewModel.showDifficultyPicker() },
            )

            when (val current = ui) {
                NonogramUi.Generating -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("generating")
                        DotPulse()
                    }
                }

                NonogramUi.GenerationFailed -> Centred {
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

                is NonogramUi.ChoosingDifficulty -> DifficultyPicker(
                    // DAILY leads (decision D32) — the day's puzzle, not a difficulty.
                    options = listOf(NonogramViewModel.DAILY_NAME) + NonogramViewModel.DIFFICULTIES,
                    current = current.currentDifficulty,
                    onSelect = { viewModel.generate(it) },
                    onCancel = { viewModel.cancelDifficultyPicker() },
                )

                is NonogramUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    showRemaining = showRemaining,
                    showTimer = showTimer,
                    showPeers = showPeers,
                    onPress = { index ->
                        // The only haptic for a fill: only this call site knows whether
                        // the entry was a mistake, and those feel different.
                        val mistake = viewModel.press(index)
                        haptics.perform(
                            if (mistake) HapticEvent.Conflict else HapticEvent.Tick
                        )
                    },
                    onMark = { index ->
                        viewModel.mark(index)
                        haptics.perform(HapticEvent.Tick)
                    },
                    onPaintStart = { haptics.perform(HapticEvent.Tick) },
                    onPaintCell = { index, value -> viewModel.paint(index, value) },
                    onUndo = { viewModel.undo() },
                )

                is NonogramUi.Complete -> {
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
                                else "mistake limit · ${current.state.difficulty.lowercase()}"
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
    state: NonogramState,
    undoAvailable: Boolean,
    showRemaining: Boolean,
    showTimer: Boolean,
    showPeers: Boolean,
    onPress: (Int) -> Unit,
    onMark: (Int) -> Unit,
    onPaintStart: () -> Unit,
    onPaintCell: (Int, Int) -> Unit,
    onUndo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // A 10×10 board plus clues overflows a short viewport, a landscape phone,
            // or a large font scale — the alternative is controls the player cannot reach.
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        Readouts(state, showTimer)

        var gridPx by remember { mutableFloatStateOf(0f) }
        // The drag block below outlives the composition that created it, so it must
        // read the board through this rather than the composition-time `state` —
        // otherwise a mark laid by a long-press would still read as unknown when
        // the same finger starts dragging a mark-run.
        val latestCells by rememberUpdatedState(state.cells)
        fun indexAt(offset: androidx.compose.ui.geometry.Offset): Int? {
            if (gridPx <= 0f) return null
            val cell = gridPx / NonogramRules.SIZE
            val c = (offset.x / cell).toInt()
            val r = (offset.y / cell).toInt()
            if (c !in 0 until NonogramRules.SIZE || r !in 0 until NonogramRules.SIZE) return null
            return r * NonogramRules.SIZE + c
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Column clues, bottom-aligned over their columns.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Empty corner where the row clues will sit.
                androidx.compose.foundation.layout.Spacer(Modifier.weight(CLUE_STRIP_WEIGHT))
                val depth = state.clues.cols.maxOf { it.size }.coerceAtLeast(1)
                for (c in 0 until NonogramRules.SIZE) {
                    ClueColumn(
                        runs = state.clues.cols[c],
                        depth = depth,
                        dimmed = showRemaining &&
                            NonogramRules.lineSolved(state.cells, state.solution, c, false),
                        emphasised = showPeers && state.selected?.let { NonogramRules.colOf(it) == c } == true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // The grid itself owns the drag gesture. Cells own tap and long-press:
            // the drag only claims the gesture once touch slop is passed, so single
            // touches always reach the cell and runs always reach the grid.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { gridPx = it.width.toFloat() }
                    .pointerInput(Unit) {
                        while (true) {
                            var mode = NonogramRules.FILLED
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val origin = indexAt(offset) ?: return@detectDragGestures
                                    // The run's mode comes from its origin cell: starting
                                    // on a mark extends the mark, anywhere else fills.
                                    // A long-press that turns into a drag therefore
                                    // extends its own mark-run for free.
                                    mode = if (latestCells[origin] == NonogramRules.MARKED) {
                                        NonogramRules.MARKED
                                    } else {
                                        NonogramRules.FILLED
                                    }
                                    onPaintStart()
                                    if (mode == NonogramRules.FILLED) onPress(origin)
                                    else onMark(origin)
                                },
                                onDrag = { change, _ ->
                                    indexAt(change.position)?.let { onPaintCell(it, mode) }
                                    change.consume()
                                },
                            )
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (r in 0 until NonogramRules.SIZE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClueRow(
                                runs = state.clues.rows[r],
                                dimmed = showRemaining &&
                                    NonogramRules.lineSolved(state.cells, state.solution, r, true),
                                emphasised = showPeers &&
                                    state.selected?.let { NonogramRules.rowOf(it) == r } == true,
                                modifier = Modifier.weight(CLUE_STRIP_WEIGHT),
                            )
                            for (c in 0 until NonogramRules.SIZE) {
                                val index = r * NonogramRules.SIZE + c
                                NonogramCell(
                                    value = when (state.cells[index]) {
                                        NonogramRules.FILLED -> NonoCellValue.Filled
                                        NonogramRules.MARKED -> NonoCellValue.Marked
                                        else -> NonoCellValue.Unknown
                                    },
                                    peer = showPeers && (state.selected?.let {
                                        NonogramRules.rowOf(it) == r || NonogramRules.colOf(it) == c
                                    } == true),
                                    selected = showPeers && state.selected == index,
                                    onTap = { onPress(index) },
                                    onLongPress = { onMark(index) },
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
            Label("tap fills · hold marks · drag paints", role = TextRole.Disabled)
        }
    }
}

/** Rule M10: exactly three readout calls in this file. A fourth one fails the build. */
@Composable
private fun Readouts(state: NonogramState, showTimer: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Readout("time", if (showTimer) formatTime(state.elapsedMs) else "--:--")
        Readout("done", "${(state.completion * 100).toInt()}%")
        Readout("line", "${state.solvedLines}/20")
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
private fun ClueColumn(
    runs: List<Int>,
    depth: Int,
    dimmed: Boolean,
    emphasised: Boolean,
    modifier: Modifier = Modifier,
) {
    val role = when {
        dimmed -> TextRole.Disabled
        emphasised -> TextRole.Display
        else -> TextRole.Secondary
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
    ) {
        // Pad short columns with empty lines so every column's runs sit on the grid.
        // The filler carries the same style, so its line height matches the numbers.
        repeat(depth - runs.size) { NothingText(text = "", role = role, style = NothingTheme.typography.data) }
        runs.forEach { run ->
            NothingText(text = run.toString(), role = role, style = NothingTheme.typography.data)
        }
    }
}

@Composable
private fun ClueRow(
    runs: List<Int>,
    dimmed: Boolean,
    emphasised: Boolean,
    modifier: Modifier = Modifier,
) {
    val role = when {
        dimmed -> TextRole.Disabled
        emphasised -> TextRole.Display
        else -> TextRole.Secondary
    }
    // One line of text, runs separated by spaces: every run in a line shares one
    // state, so per-number composables would buy nothing but spacing risk.
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NothingText(
            text = runs.joinToString(" "),
            role = role,
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
    ) { content() }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private const val CLUE_STRIP_WEIGHT = 2.6f
