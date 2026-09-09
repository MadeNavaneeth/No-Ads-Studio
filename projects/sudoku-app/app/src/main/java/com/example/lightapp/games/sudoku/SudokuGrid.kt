package com.example.lightapp.games.sudoku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.lightapp.designsystem.components.CellState
import com.example.lightapp.designsystem.components.GridCell
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing

/**
 * The 9×9 grid — just the 81 cells, floating on the page.
 *
 * No panel, no frame. **Box grouping is spacing, not lines** — `space8` between boxes,
 * `space4` between cells within one. Requirement 11 criterion 8 forbids divider lines,
 * so the 3×3 structure is conveyed by rhythm alone. It reads more clearly than rules do,
 * and it is why the spacing scale needed both a 4dp and an 8dp step. The grid is drawn
 * directly on the screen background; each cell is its own square (`surface` fill, no
 * resting outline — see GridCell).
 *
 * No fixed cell size anywhere: the grid fills the width it is given, holds a 1:1 aspect
 * ratio, and each cell takes an equal weight. So it is correct on any screen, and the
 * cell radius stays near its intended proportion of the cell side.
 */
@Composable
fun SudokuGrid(
    state: SudokuState,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showPeers: Boolean = true,
) {
    val playerConflicts = playerConflicts(state)
    val accented = accentedConflict(state, playerConflicts)
    // Two layers, composed — sudoku-market-study.md §5A (amended D30): every selection
    // lights its cross (row/column/box), and a filled selection additionally lights the
    // sibling cells holding that digit. One rule covers every tap; the layers are told
    // apart by outline inside GridCell.
    val cross = if (showPeers) crossOf(state) else emptySet()
    val siblings = if (showPeers) siblingsOf(state) else emptySet()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
    ) {
        for (boxRow in 0 until Conflicts.BOX) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
            ) {
                for (boxCol in 0 until Conflicts.BOX) {
                    Box3x3(
                        state = state,
                        conflicts = playerConflicts,
                        accented = accented,
                        cross = cross,
                        siblings = siblings,
                        boxRow = boxRow,
                        boxCol = boxCol,
                        onCellClick = onCellClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Box3x3(
    state: SudokuState,
    conflicts: Set<Int>,
    accented: Int?,
    cross: Set<Int>,
    siblings: Set<Int>,
    boxRow: Int,
    boxCol: Int,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
    ) {
        for (r in 0 until Conflicts.BOX) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
            ) {
                for (c in 0 until Conflicts.BOX) {
                    val row = boxRow * Conflicts.BOX + r
                    val col = boxCol * Conflicts.BOX + c
                    val index = row * Conflicts.SIZE + col
                    val value = state.valueAt(index)

                    GridCell(
                        value = if (value == 0) null else value.toString(),
                        state = cellStateFor(state, index, conflicts, accented),
                        selected = state.selected == index,
                        cross = index in cross,
                        sibling = index in siblings,
                        notes = notesLabel(state, index),
                        onClick = { onCellClick(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The **cross** layer: the row, column and box of the selection, excluding itself.
 *
 * Lit on **every** tap, filled or empty — this is the constant base layer of the
 * highlight (market study §5A, amending D30). An empty cell asks *what can go here?*;
 * a filled one asks the same question before *where else is this digit?* gets asked, so
 * the constraint is always visible while the finger is down.
 *
 * The soft layer visually: raised fill with no outline (see GridCell).
 *
 * Returns an empty set when nothing is selected, so a fresh board is quiet.
 */
internal fun crossOf(state: SudokuState): Set<Int> {
    val selected = state.selected ?: return emptySet()
    return Conflicts.peersOf(selected).toSet()
}

/**
 * The **sibling** layer: every other cell holding the same digit as the selection.
 *
 * Lit only when the selection holds a digit (a given or an entry; the player is looking
 * for nines, not for who placed them), and it composes with the cross instead of
 * replacing it — a filled tap answers *where else is this digit?* on top of *what can go
 * here?*. At most nine cells, usually fewer. The stronger layer visually: raised fill
 * plus the hairline (or TE accent) outline (see GridCell).
 *
 * Returns an empty set when nothing is selected or the selection is empty.
 */
internal fun siblingsOf(state: SudokuState): Set<Int> {
    val selected = state.selected ?: return emptySet()
    val value = state.valueAt(selected)
    if (value == 0) return emptySet()
    return (0 until Conflicts.CELLS)
        .filter { it != selected && state.valueAt(it) == value }
        .toSet()
}

/**
 * Conflicts the player can actually resolve.
 *
 * `state.conflicts` includes givens, because a duplicate has two ends and one of them may
 * belong to the puzzle. Presenting that end as a conflict was wrong three ways: it marked
 * a cell the player cannot change as the problem; it made a given selectable, since
 * `GridCell` only refuses taps for [CellState.Given]; and being lowest in reading order it
 * usually won the single red accent, pointing at the given instead of at the mistake.
 *
 * The player's own cell is the one to flag. A given never changes, so it is never the
 * thing that went wrong.
 */
internal fun playerConflicts(state: SudokuState): Set<Int> =
    state.conflicts.filterNot { state.isGiven(it) }.toSet()

/**
 * Which conflicting cell carries the screen's single red accent — Requirement 14
 * criterion 10.
 *
 * The most recently entered cell if it is in conflict, otherwise the first in reading
 * order. Every other conflict renders muted, so the count of red elements is at most one
 * no matter how many duplicates exist.
 */
internal fun accentedConflict(state: SudokuState, conflicts: Set<Int>): Int? = when {
    conflicts.isEmpty() -> null
    state.lastEntered != null && state.lastEntered in conflicts -> state.lastEntered
    else -> conflicts.minOrNull()
}

/**
 * Given is tested first, and the order is the point.
 *
 * [CellState.Given] is the state that makes a cell reject input, so any state able to
 * outrank it turns a given into something the player can select. [conflicts] is already
 * filtered to player cells by [playerConflicts], which makes the conflict branches
 * unreachable for a given — the explicit ordering keeps it that way if that filter ever
 * moves or is called with an unfiltered set.
 */
internal fun cellStateFor(
    state: SudokuState,
    index: Int,
    conflicts: Set<Int>,
    accented: Int?,
): CellState = when {
    state.isGiven(index) -> CellState.Given
    index in conflicts && index == accented -> CellState.ConflictAccented
    index in conflicts -> CellState.ConflictMuted
    state.entries[index] != 0 -> CellState.Entered
    else -> CellState.Empty
}

/** Pencil marks, only when the cell has no value. */
private fun notesLabel(state: SudokuState, index: Int): String? {
    if (state.valueAt(index) != 0) return null
    val marks = state.notesAt(index)
    return if (marks.isEmpty()) null else marks.joinToString("")
}
