package com.example.lightapp.games.blockpuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.BlockCell
import com.example.lightapp.designsystem.components.BlockPiece
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
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics
import com.example.lightapp.studio.persistence.BlockSessionStore

/**
 * `blockpuzzle(8)` — screen. Nonogram's board idiom, a tray, and an endless
 * scoreboard: this game has no WIN, so the complete panel is a record, not a trophy.
 *
 * ## The drag, decision D33
 *
 * The tray piece follows the finger as a *ghost* — direct manipulation, which D33
 * rules is input, not animation. The drop anchor is computed from real screen
 * geometry: the pointer's root position at release, the board's bounds, and the
 * piece's cell dimensions — the piece's centre goes to the finger, the anchor
 * rounds from there, and the rules validate. The piece lands only where it fits
 * (the cells simply appear filled — an opacity event), or refuses and stays in the
 * tray. Nothing travels after the finger lifts, which is what S13 has always
 * forbidden.
 *
 * **There is no red on this screen, ever.** A piece that does not fit refuses to
 * land — found by contradiction, not announced (method §6).
 */
@Composable
fun BlockScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
    initialDifficulty: String? = null,
) {
    val context = LocalContext.current
    val store = remember { BlockSessionStore(context.applicationContext) }
    val viewModel: BlockViewModel = viewModel { BlockViewModel(store) }
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
        if (requested != null) viewModel.newRun(requested)
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
                title = "blockpuzzle(8)",
                // No explicit save here: leaving the screen pauses it, and pausing saves.
                onBack = onBack,
                actionLabel = "new",
                onAction = { pickerVisible = true },
            )

            if (pickerVisible) {
                DifficultyPicker(
                    // DAILY leads (decision D32) — the day's deal, not a difficulty.
                    options = listOf(BlockViewModel.DAILY_NAME) + BlockGenerator.DIFFICULTIES,
                    current = (ui as? BlockUi.Playing)?.state?.difficulty,
                    onSelect = {
                        pickerVisible = false
                        viewModel.newRun(it)
                    },
                    onCancel = { pickerVisible = false },
                )
            } else when (val current = ui) {
                BlockUi.Dealing -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
                    ) {
                        Label("dealing")
                        DotPulse()
                    }
                }

                is BlockUi.Playing -> Board(
                    state = current.state,
                    undoAvailable = viewModel.undoAvailable,
                    onUndo = viewModel::undo,
                    onDrop = { trayIndex, row, col ->
                        val landed = viewModel.drop(trayIndex, row, col)
                        haptics.perform(if (landed) HapticEvent.Tick else HapticEvent.Conflict)
                    },
                )

                is BlockUi.Complete -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        NothingText(
                            text = current.state.score.toString(),
                            role = TextRole.Display,
                            style = NothingTheme.typography.displayMedium,
                        )
                        Label(
                            if (current.state.dailyDay != null) "run over · daily"
                            else "run over · ${current.state.difficulty.lowercase()}",
                        )
                        NothingButton(
                            text = "again",
                            onClick = { viewModel.newRun(current.state.difficulty) },
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

@Composable
private fun Board(
    state: BlockState,
    undoAvailable: Boolean,
    onUndo: () -> Unit,
    onDrop: (trayIndex: Int, row: Int, col: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        // Three curated readouts (D13): the score, the clock, the filled ground.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
        ) {
            Readout("score", state.score.toString())
            Readout("time", formatTime(state.elapsedMs))
            Readout("fill", "${(state.completion * 100).toInt()}%")
        }

        // The one aid the genre is allowed (parity G4): take the last placement
        // back. Quiet emphasis — a correction, not an action the board asks for.
        NothingButton(
            text = "undo",
            onClick = onUndo,
            enabled = undoAvailable,
            emphasis = ButtonEmphasis.Quiet,
        )

        // The board's root bounds are what a drop is measured against — captured
        // once per layout, read at drag end.
        var boardBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
        val latestCells by rememberUpdatedState(state.cells)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { boardBounds = it.boundsInRoot() },
        ) {
            for (r in 0 until BlockRules.SIZE) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until BlockRules.SIZE) {
                        BlockCell(
                            filled = latestCells[r * BlockRules.SIZE + c] == BlockRules.FILLED,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Tray(
            tray = state.tray,
            boardBounds = boardBounds,
            cells = { latestCells },
            onDrop = onDrop,
        )
    }
}

/**
 * The tray: three slots, one piece each, dealt under D33. A piece is draggable; the
 * drop anchor comes from the pointer's root position at release relative to the
 * board's root bounds — the piece is centred on the finger, the anchor rounds from
 * there, and the rules layer validates. An invalid anchor is refused (false from
 * [onDrop]'s handler keeps the piece in the tray).
 */
@Composable
private fun Tray(
    tray: List<BlockRules.Piece>,
    boardBounds: androidx.compose.ui.geometry.Rect,
    cells: () -> List<Int>,
    onDrop: (trayIndex: Int, row: Int, col: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
    ) {
        for ((i, piece) in tray.withIndex()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                var dragAccumulated by remember { mutableStateOf(Offset.Zero) }
                var slotBounds by remember {
                    mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .onGloballyPositioned { slotBounds = it.boundsInRoot() }
                        .pointerInput(piece, i, boardBounds) {
                            detectDragGestures(
                                onDragStart = { dragAccumulated = Offset.Zero },
                                onDrag = { change, amount ->
                                    dragAccumulated += amount
                                    change.consume()
                                },
                                onDragEnd = {
                                    val cell = boardBounds.width / BlockRules.SIZE
                                    if (cell > 0f && boardBounds.width > 0f && slotBounds.width > 0f) {
                                        // The drop point in root coordinates is the
                                        // piece's tray position plus the drag's total
                                        // offset — the piece effectively sits under
                                        // the finger. The anchor then backs out the
                                        // piece's half-extent in cells so the *shape*,
                                        // not its corner, is centred on the finger.
                                        val drop = slotBounds.center + dragAccumulated
                                        val shapeCols = piece.grid.first().size
                                        val shapeRows = piece.grid.size
                                        val anchorCol = ((drop.x - boardBounds.left) / cell -
                                            shapeCols / 2f).toInt()
                                        val anchorRow = ((drop.y - boardBounds.top) / cell -
                                            shapeRows / 2f).toInt()
                                        onDrop(
                                            i,
                                            anchorRow.coerceIn(0, BlockRules.SIZE - 1),
                                            anchorCol.coerceIn(0, BlockRules.SIZE - 1),
                                        )
                                    }
                                    dragAccumulated = Offset.Zero
                                },
                            )
                        },
                ) {
                    BlockPiece(
                        grid = piece.grid,
                        density = piece.density,
                        role = TextRole.Primary,
                    )
                }
            }
        }
    }
}

/** Rule D13: exactly three readout calls in this file. */
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

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
