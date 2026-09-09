package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole

/**
 * One `binairo(10)` cell's display value. Owned here, in the design system, so the
 * component never imports game code — the screen maps the game's cell vocabulary
 * onto this at the call site, exactly as akari's does.
 */
enum class BinairoCellValue {
    /** Undecided — the player's tap target. */
    Undecided,

    /** A decided one — drawn as a solid dot. */
    One,

    /** A decided zero — drawn as a hollow ring. */
    Zero,
}

/**
 * One `binairo(10)` cell — the spec lives in `component-specs.md` (`BinairoCell`).
 *
 * The board *is* the studio's dot language: a decided one is a solid dot in
 * `textDisplay`, a decided zero a hollow ring in `textPrimary` — the D33 dot-density
 * device as the game's whole vocabulary, not its identity trick. Undecided ground is
 * `surface`; a given carries a `borderSubtle` hairline so the puzzle's fixed cells
 * read as part of the shipped board, not the play. There is no red and no error
 * state (parity A5): a run of three is three dots in a row, visible on the board,
 * never announced.
 *
 * Taps fire on every cell; the caller refuses givens in the view model, which is the
 * structural refusal — the component stays a pure display of state.
 */
@Composable
fun BinairoCell(
    value: BinairoCellValue,
    given: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = when (value) {
        BinairoCellValue.Undecided -> NothingTheme.colors.surface(SurfaceRole.Surface)
        BinairoCellValue.One, BinairoCellValue.Zero ->
            NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
    }
    val animatedFill by animateColorAsState(
        targetValue = fill,
        animationSpec = NothingMotion.microSpec(),
        label = "cell",
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(animatedFill)
            .then(
                if (given) {
                    // The given hairline: the puzzle's fixed cells frame themselves,
                    // the same borderSubtle ring GridCell's cross layer speaks.
                    Modifier.border(
                        BorderStroke(
                            StrokeTokens.strokeHairline,
                            NothingTheme.colors.border(BorderRole.Subtle),
                        ),
                        NothingTheme.shapes.cell,
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = when {
                    value == BinairoCellValue.One -> if (given) "one, given" else "one"
                    value == BinairoCellValue.Zero -> if (given) "zero, given" else "zero"
                    given -> "given, empty"
                    else -> "undecided"
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null, // our own feedback; no Material ripple
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (value) {
            // The one: a solid dot — the shared "decided" motif at display hue,
            // the pairing that clears contrast on both themes.
            BinairoCellValue.One -> Box(
                modifier = Modifier
                    .fillMaxSize(ONE_DOT_FRACTION)
                    .background(
                        NothingTheme.colors.text(TextRole.Display),
                        CircleShape,
                    ),
            )
            // The zero: a hollow ring — the same dot, opened. Density is the
            // distinction; hue never is (method §4, D33).
            BinairoCellValue.Zero -> Box(
                modifier = Modifier
                    .size(NothingSpacing.optical.dp * ZERO_RING_OPTICAL_MULTIPLE)
                    .border(
                        BorderStroke(
                            StrokeTokens.strokeHairline,
                            NothingTheme.colors.text(TextRole.Primary),
                        ),
                        CircleShape,
                    ),
            )
            BinairoCellValue.Undecided -> Unit
        }
    }
}

private const val ONE_DOT_FRACTION = 0.36f

/**
 * The ring's diameter, in optical units — the token's only sanctioned use at a
 * multiple, kept here so the ring scales with the dot language rather than the cell.
 */
private const val ZERO_RING_OPTICAL_MULTIPLE = 3