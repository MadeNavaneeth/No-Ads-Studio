package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * One `connect(7)` cell — the spec lives in `component-specs.md` (`ConnectCell`).
 *
 * ## Path rendering without an icon set
 *
 * The route is drawn as **channels**: the caller says which sides the route continues
 * toward ([connections]), and the cell lays a bar from its centre to each of those
 * edges. Adjacent cells on a route each bar toward the other, so the bars join across
 * the cell boundary into one continuous line — pure geometry, no Canvas, no glyph
 * assets, and it scales with the grid for free.
 *
 * Endpoints carry **numerals, never colours** (roadmap §4 — the whole monochrome
 * adaptation of Numberlink). A route's bars and its endpoints' numerals are the same
 * ink; the pair is identified by its number, and two routes running side by side stay
 * readable because channels only ever connect same-value neighbours — the caller's
 * computation, but the component's contract.
 *
 * ## Fills stay reserved
 *
 * A laid route raises the fill (`SurfaceRaised`, the same "decided" idiom as
 * `NonogramCell`), the endpoint keeps the base surface under its numeral, and red does
 * not exist on this screen at all: every move is retractable, so there is nothing to
 * be wrong about (method §6).
 *
 * Taps fire no haptic of their own — only the caller knows whether a step completed a
 * route, and decision D15 wants those to feel different.
 */
enum class ConnectCellValue {
    Empty,
    Path,
    Endpoint,
}

/** The four sides a route channel can continue toward. */
enum class ConnectSide {
    North,
    East,
    South,
    West,
}

@Composable
fun ConnectCell(
    value: ConnectCellValue,
    /** The endpoint numeral (1–8). Read only when [value] is [ConnectCellValue.Endpoint]. */
    pair: Int?,
    /** Sides the route continues toward — the caller derives them from the board. */
    connections: Set<ConnectSide>,
    /** The live walk's head. */
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetFill = if (value == ConnectCellValue.Path) {
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
    val channel = NothingTheme.colors.border(BorderRole.Visible)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(animatedFill)
            .semantics(mergeDescendants = true) {
                // Instrument readout: TalkBack hears what the cell is and which pair
                // it serves, not just "button" (C11).
                val description = when (value) {
                    ConnectCellValue.Endpoint -> "endpoint $pair"
                    ConnectCellValue.Path -> "path"
                    ConnectCellValue.Empty -> "empty"
                }
                contentDescription = if (selected) "$description, selected" else description
            }
            .then(
                if (selected) {
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
            .clickable(
                interactionSource = interaction,
                indication = null, // our own feedback; no Material ripple
                role = Role.Button,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Channels first, so the numeral sits above them.
        for (side in connections) {
            Box(
                modifier = when (side) {
                    ConnectSide.North -> Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(CHANNEL_FRACTION)
                        .fillMaxHeight()
                    ConnectSide.South -> Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(CHANNEL_FRACTION)
                        .fillMaxHeight()
                    ConnectSide.East -> Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth()
                        .fillMaxHeight(CHANNEL_FRACTION)
                    ConnectSide.West -> Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .fillMaxHeight(CHANNEL_FRACTION)
                }
                    .background(channel)
            )
        }
        if (value == ConnectCellValue.Endpoint && pair != null) {
            NothingText(
                text = pair.toString(),
                role = TextRole.Primary,
                style = NothingTheme.typography.cellNumeral,
            )
        }
    }
}

/** Channel width as a fraction of the cell — a corridor, not a wire. */
private const val CHANNEL_FRACTION = 0.34f
