package com.example.lightapp.games.wordsearch

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.lightapp.designsystem.components.DotPulse
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.components.WordCellValue
import com.example.lightapp.designsystem.components.WordsearchCell
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics
import com.example.lightapp.studio.persistence.WordsearchSessionStore

/**
 * `wordsearch(12)` — the hunt in this language.
 *
 * Three readouts and no more (decision D13): time, completion, words left. The grid
 * is lettered from the first frame, so there is deliberately no [DotMatrixReadout]
 * behind it — a second mark layer would compete with the one surface that is already
 * all content (method §5).
 *
 * **There is no red on this screen, ever.** A wrong drag simply does not lock; the
 * only cost is the seconds the trace took, and nothing here is urgent enough to
 * spend the alarm on (method §6).
 */
@Composable
fun WordsearchScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { WordsearchSessionStore(context.applicationContext) }
    val viewModel: WordsearchViewModel = viewModel { WordsearchViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val haptics = rememberNothingHaptics()

    // Gameplay preferences live in the store and are read here, at their one call
    // site, rather than threaded down through the shell.
    val showTimer by store.showTimer.collectAsStateWithLifecycle(initialValue = true)
    val showRemaining by store.showRemaining.collectAsStateWithLifecycle(initialValue = true)

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
                title = "wordsearch(12)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { viewModel.showDifficultyPicker() },
            )

            when (val current = ui) {
                WordsearchUi.Generating -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("generating")
                        DotPulse()
                    }
                }

                WordsearchUi.GenerationFailed -> Centred {
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

                is WordsearchUi.ChoosingDifficulty -> DifficultyPicker(
                    // DAILY leads (decision D32) — the day's puzzle, not a difficulty.
                    options = listOf(WordsearchViewModel.DAILY_NAME) + WordsearchViewModel.DIFFICULTIES,
                    current = current.currentDifficulty,
                    onSelect = { viewModel.generate(it) },
                    onCancel = { viewModel.cancelDifficultyPicker() },
                )

                is WordsearchUi.Playing -> Board(
                    state = current.state,
                    selection = selection,
                    undoAvailable = viewModel.undoAvailable,
                    showTimer = showTimer,
                    showRemaining = showRemaining,
                    onDragStart = { index ->
                        viewModel.dragStart(index)
                        haptics.perform(HapticEvent.Tick)
                    },
                    onDragTo = { index -> viewModel.dragTo(index) },
                    onDragEnd = {
                        // The find haptic fires here because only the rules know
                        // whether the trace spelled a word — and only the caller
                        // should feel it.
                        val found = viewModel.dragEnd()
                        if (found.isNotEmpty()) haptics.perform(HapticEvent.Tick)
                    },
                    onDragCancel = { viewModel.dragEnd() },
                    onUndo = { viewModel.undo() },
                )

                is WordsearchUi.Complete -> Centred {
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
    state: WordsearchState,
    selection: List<Int>?,
    undoAvailable: Boolean,
    showTimer: Boolean,
    showRemaining: Boolean,
    onDragStart: (Int) -> Unit,
    onDragTo: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onUndo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // A 12×12 lettered board plus the word list overflows a short viewport,
            // a landscape phone, or a large font scale — the alternative is controls
            // the player cannot reach.
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        Readouts(state, showTimer, showRemaining)

        var gridPx by remember { mutableFloatStateOf(0f) }
        fun indexAt(offset: androidx.compose.ui.geometry.Offset): Int? {
            if (gridPx <= 0f) return null
            val cell = gridPx / WordsearchRules.SIZE
            val c = (offset.x / cell).toInt()
            val r = (offset.y / cell).toInt()
            if (c !in 0 until WordsearchRules.SIZE || r !in 0 until WordsearchRules.SIZE) return null
            return r * WordsearchRules.SIZE + c
        }

        // The grid owns the drag gesture — the same split as nonogram: cells have no
        // click handler, the trace is the grid's gesture, and the highlight follows
        // the finger through the cells without ever being a decision.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { gridPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    while (true) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                indexAt(offset)?.let(onDragStart)
                            },
                            onDrag = { change, _ ->
                                indexAt(change.position)?.let(onDragTo)
                                change.consume()
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (r in 0 until WordsearchRules.SIZE) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until WordsearchRules.SIZE) {
                            val index = r * WordsearchRules.SIZE + c
                            WordsearchCell(
                                letter = state.letters[index],
                                value = if (state.foundCells[index] == WordsearchRules.FOUND) {
                                    WordCellValue.Found
                                } else {
                                    WordCellValue.Hidden
                                },
                                selected = index in selection.orEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        WordList(state)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
        ) {
            NothingButton(
                text = "undo",
                onClick = onUndo,
                enabled = undoAvailable,
            )
            Label("drag a line to find a word", role = TextRole.Disabled)
        }
    }
}

/**
 * The hunt list — every placed word, found ones demoted to [TextRole.Disabled].
 *
 * The list never shrinks: a found word stays visible so the panel keeps its size and
 * the grid keeps its lettering, and the demotion is the only acknowledgement a find
 * needs. Three per row keeps even the longest pool word on one line at the largest
 * supported font scale.
 */
@Composable
private fun WordList(state: WordsearchState) {
    val found = state.foundWords
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
    ) {
        state.placements.map { it.word }.chunked(3).forEach { rowWords ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowWords.forEachIndexed { i, word ->
                    if (i > 0) Spacer(Modifier.weight(0.2f))
                    NothingText(
                        text = word,
                        role = if (word in found) TextRole.Disabled else TextRole.Secondary,
                        style = NothingTheme.typography.data,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Rule M10: exactly three readout calls in this file. A fourth one fails the build. */
@Composable
private fun Readouts(state: WordsearchState, showTimer: Boolean, showRemaining: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Readout("time", if (showTimer) formatTime(state.elapsedMs) else "--:--")
        Readout("done", "${(state.completion * 100).toInt()}%")
        Readout(
            "left",
            if (showRemaining) (state.placements.size - state.foundWords.size).toString() else "--",
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
