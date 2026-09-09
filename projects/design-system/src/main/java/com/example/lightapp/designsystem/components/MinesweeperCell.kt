package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole

/**
 * One `minesweeper(10)` cell — the spec lives in `component-specs.md` (`MinesweeperCell`).
 *
 * Four values, two flags, and the project's single live red: the detonated cell that
 * ends a run. Revealed ground reads *lower* than unknown (`bg` under `surface`) — the
 * opposite of `NonogramCell`, where deciding a pixel raises it — because here opening
 * a cell is removing information hiding, not adding a picture pixel.
 *
 * The flag reuses `NonogramCell`'s decided-empty dot idiom: a centred dot in the
 * Glyph's own dot colour means "I believe something is here" in both games, which is
 * the motif doing real work rather than decoration.
 *
 * Like `NonogramCell`, taps fire no haptic of their own — only the caller knows
 * whether the reveal was the mine, and decision D15 wants those to feel different.
 */
enum class MineCellValue {
    Unknown,
    Revealed,
    Flagged,
    Detonated,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperCell(
    value: MineCellValue,
    count: Int,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetFill = when (value) {
        MineCellValue.Revealed -> NothingTheme.colors.surface(SurfaceRole.Background)
        MineCellValue.Detonated -> NothingTheme.colors.accentRed
        else -> NothingTheme.colors.surface(SurfaceRole.Surface)
    }
    val animatedFill by animateColorAsState(
        targetValue = targetFill,
        animationSpec = NothingMotion.microSpec(),
        label = "cell",
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(animatedFill)
            .semantics(mergeDescendants = true) {
                // Instrument readout: TalkBack hears state and count, not just
                // "button", so the board is navigable without colour (C11).
                val description = when (value) {
                    MineCellValue.Unknown -> "covered"
                    MineCellValue.Flagged -> "flagged"
                    MineCellValue.Revealed -> if (count > 0) "adjacent $count" else "clear"
                    MineCellValue.Detonated -> "mine detonated"
                }
                contentDescription = if (selected) "$description, selected" else description
            }
            .then(
                // Red fill is the alarm; nothing frames it — the same ordering
                // rule as GridCell, where a cosmetic outline never displaces the
                // screen's one urgent signal.
                if (selected && value != MineCellValue.Detonated) {
                    Modifier.border(
                        BorderStroke(
                            StrokeTokens.strokeHairline,
                            NothingTheme.colors.border(BorderRole.Visible)
                        ),
                        NothingTheme.shapes.cell,
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null, // our own feedback; no Material ripple
                role = Role.Button,
                onLongClick = onLongPress,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // The flag: the shared "something is here" dot. A dot, never a flag
            // glyph — there is no icon set in this project and the motif carries
            // the meaning in both games that use it.
            value == MineCellValue.Flagged -> Box(
                modifier = Modifier
                    .fillMaxSize(MARK_DOT_FRACTION)
                    .background(
                        NothingTheme.colors.border(BorderRole.Visible),
                        CircleShape,
                    )
            )
            // The adjacency numeral, one hue for every count: the digit already
            // distinguishes 1–8, so the colours were redundant (method §4). Over
            // the red fill the numeral switches to textDisplay — white over
            // #D71921 is the one pairing that clears 4.5:1.
            (value == MineCellValue.Revealed || value == MineCellValue.Detonated) && count > 0 ->
                NothingText(
                    text = count.toString(),
                    role = if (value == MineCellValue.Detonated) TextRole.Display else TextRole.Primary,
                    style = NothingTheme.typography.cellNumeral,
                )
        }
    }
}

private const val MARK_DOT_FRACTION = 0.32f
