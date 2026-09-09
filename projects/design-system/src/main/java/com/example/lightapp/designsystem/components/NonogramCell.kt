package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.SurfaceRole

/**
 * One `nonogram(10)` cell — the spec lives in `component-specs.md` (`NonogramCell`).
 *
 * Three values, two flags, no red anywhere: a wrong fill is found by contradiction,
 * not announced, so there is no conflict state to render. The caller gates [peer]
 * and [selected] behind the highlight preference; this component only resolves them.
 *
 * Press feedback is the fill fading over one micro duration. Like `NumberPad` keys,
 * taps fire no haptic of their own — only the caller knows whether the entry was a
 * mistake, and decision D15 wants those to feel different.
 */
enum class NonoCellValue {
    Unknown,
    Filled,
    Marked,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NonogramCell(
    value: NonoCellValue,
    peer: Boolean,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetFill = if (value == NonoCellValue.Filled) {
        NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
    } else {
        NothingTheme.colors.surface(SurfaceRole.Surface)
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
            .then(
                when {
                    selected -> Modifier.border(
                        BorderStroke(
                            StrokeTokens.strokeHairline,
                            NothingTheme.colors.border(BorderRole.Visible)
                        ),
                        NothingTheme.shapes.cell,
                    )
                    // The peer band is a soft ring, never a fourth fill: fills stay
                    // reserved for decided cells, so the band cannot be misread as one.
                    peer -> Modifier.border(
                        BorderStroke(
                            StrokeTokens.strokeHairline,
                            NothingTheme.colors.border(BorderRole.Subtle)
                        ),
                        NothingTheme.shapes.cell,
                    )
                    else -> Modifier
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
        if (value == NonoCellValue.Marked) {
            // The empty-mark: a centred dot in the Glyph's own dot colour, sized as
            // a fraction of the cell so it is correct on any screen.
            Box(
                modifier = Modifier
                    .fillMaxSize(MARK_DOT_FRACTION)
                    .background(
                        NothingTheme.colors.border(BorderRole.Visible),
                        CircleShape,
                    )
            )
        }
    }
}

private const val MARK_DOT_FRACTION = 0.32f
