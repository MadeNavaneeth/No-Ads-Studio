package com.example.lightapp.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole

/**
 * Dot-matrix game icon — spec in component-specs.md.
 *
 * The Glyph reference at icon scale. A boolean grid drawn with Canvas, so there is no
 * image asset anywhere in the project.
 *
 * Dot geometry is **computed from the box size**, never a literal: pitch is the side
 * divided by the grid dimension, and diameter is the side divided by `2n + 1`. That
 * relationship keeps the dots visually separated at any grid size — at 5×5 in a 24dp
 * box it yields roughly 2.2dp dots on a 4.8dp pitch.
 *
 * Patterns are authored in `dotmatrixtool` or `smittytone/ASCII` and transcribed to
 * booleans. See resource-map.md. Five rows is the practical ceiling at 24dp.
 */
@Composable
fun GameIcon(
    rows: List<List<Boolean>>,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Primary,
) {
    val color = NothingTheme.colors.text(role)
    val box = NothingSpacing.lg

    Canvas(
        modifier = modifier
            .size(box.dp)
            // Decorative in the accessibility sense: the game name beside it carries
            // the meaning, so announcing the icon would be noise.
            .clearAndSetSemantics { },
    ) {
        val n = rows.size
        if (n == 0) return@Canvas

        val side = minOf(size.width, size.height)
        val pitch = side / n
        val radius = side / (2f * n + 1f) / 2f

        rows.forEachIndexed { r, cols ->
            cols.forEachIndexed { c, on ->
                if (!on) return@forEachIndexed
                drawCircle(
                    color = color,
                    radius = radius,
                    // Half-pitch offset centres each dot in its cell.
                    center = Offset(
                        x = c * pitch + pitch / 2f,
                        y = r * pitch + pitch / 2f,
                    ),
                )
            }
        }
    }
}
