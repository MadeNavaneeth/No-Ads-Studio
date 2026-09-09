package com.example.lightapp.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.TextRole

/**
 * One tray piece of `blockpuzzle(8)` — spec in component-specs.md.
 *
 * ## Why density, not colour
 *
 * Decision D33's ruling: three concurrent tray pieces must be distinguishable without
 * colour, and the language already carries information as dot pitch (G1–G3, the Doto
 * typeface itself). So a piece renders at one of three densities — **solid** fill,
 * **half-pitch** dots, or a **hollow** outline — which is a data-bearing distinction
 * (it survives grayscale by construction) and never a decoration.
 *
 * The shape grid is row-major 0/1; empty shape cells render nothing at all, so the
 * piece's silhouette is exactly its catalog geometry. Cells are circles on the shared
 * `GridCell` radius so a landed piece reads as the same material as the board.
 */
@Composable
fun BlockPiece(
    grid: List<List<Int>>,
    density: Int,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Primary,
) {
    if (grid.isEmpty() || grid.first().isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(grid.first().size.toFloat() / grid.size.toFloat()),
    ) {
        for (row in grid) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (cell in row) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell == 1) BlockCell(density = density, role = role)
                    }
                }
            }
        }
    }
}

/**
 * One occupied shape cell at [density] — 0 solid, 1 half-pitch, 2 hollow. The dot is
 * a circle at the shared cell radius; hollow is the same circle with only its border,
 * half-pitch a centred dot at half the cell side (the typeface's own half-pitch idea).
 */
@Composable
private fun BlockCell(density: Int, role: TextRole) {
    val fill = NothingTheme.colors.text(role)
    // Proportions, not pixels: each density is a fraction of the shape cell, so the
    // three read as the same object at three settings on any screen. The hollow
    // outline uses the shared hairline stroke — the studio's one border weight.
    when (density) {
        // Solid: the full circle.
        0 -> Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(fill),
        )

        // Half-pitch: a centred dot at half the cell side.
        1 -> Box(
            modifier = Modifier
                .fillMaxSize(0.41f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(fill),
        )

        // Hollow: the outline only.
        else -> Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .aspectRatio(1f)
                .border(
                    BorderStroke(StrokeTokens.strokeHairline, fill),
                    CircleShape,
                ),
        )
    }
}
