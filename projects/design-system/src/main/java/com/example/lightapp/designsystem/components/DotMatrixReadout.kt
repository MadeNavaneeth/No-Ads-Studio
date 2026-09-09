package com.example.lightapp.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.LocalAccentChoice
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.OpacityTokens
import com.example.lightapp.designsystem.theme.Spacing

/**
 * The Glyph reference — Requirement 12, decision D12.
 *
 * **This is a readout, never a texture.** In Nothing's language dots always carry
 * information: the Glyph Interface has only ever been used for flash, timer, progress,
 * and notification state. A decorative dot grid is the single clearest tell that
 * someone copied the look without understanding it, and the fastest way to earn the
 * "gimmick" charge that Nothing's own Glyph Matrix attracted.
 *
 * So [progress] is required, not optional. Dots at or below the threshold render
 * bright; dots above it render dim. In Sudoku it is bound to completion percentage.
 *
 * Deliberately below the legibility threshold: the dot colour measures under 2:1
 * against the background, which `TokenValidationTest` asserts as a contrast *maximum*.
 * If the texture were legible it would compete with the game grid.
 *
 * @param progress 0f–1f. Fraction of the grid that renders bright.
 */
@Composable
fun DotMatrixReadout(
    progress: Float,
    modifier: Modifier = Modifier,
    pitch: DotPitch = DotPitch.Tight,
) {
    val accentChoice = LocalAccentChoice.current
    val dotColor = when (accentChoice) {
        "sage" -> NothingTheme.colors.accentSage
        "amber" -> NothingTheme.colors.accentAmber
        else -> NothingTheme.colors.border(BorderRole.Visible)
    }
    val pitchDp = when (pitch) {
        DotPitch.Tight -> NothingSpacing.md
        DotPitch.Wide -> NothingSpacing.lg
    }
    val diameter = NothingSpacing.optical

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // Requirement 12 criterion 10: passes every pointer event through and
            // exposes no accessibility node. A texture must not be a tab stop.
            .clearAndSetSemantics { },
    ) {
        // DrawScope is a Density, so a token converts here without ever being
        // unwrapped outside this module.
        val pitchPx = pitchDp.dp.toPx()
        val radiusPx = diameter.dp.toPx() / 2f
        if (pitchPx <= 0f) return@Canvas

        val cols = (size.width / pitchPx).toInt() + 1
        val rows = (size.height / pitchPx).toInt() + 1

        // Requirement 12 criterion 4: at most 5,000 dots in a single pass.
        val total = cols * rows
        val stride = if (total > MAX_DOTS) (total / MAX_DOTS) + 1 else 1

        // Fill order is bottom-up, so progress reads as a level rising rather than as
        // an arbitrary reveal.
        val threshold = (1f - progress.coerceIn(0f, 1f)) * size.height

        var i = 0
        for (row in 0 until rows) {
            val y = row * pitchPx
            for (col in 0 until cols) {
                if (i++ % stride != 0) continue
                val x = col * pitchPx
                val alpha = if (y >= threshold) OpacityTokens.dotAlphaBright else OpacityTokens.dotAlphaDim
                drawCircle(
                    color = dotColor,
                    radius = radiusPx,
                    center = Offset(x, y),
                    alpha = alpha,
                )
            }
        }
    }
}

/** Pitch options, both drawn from the spacing scale. No off-scale value is expressible. */
enum class DotPitch {
    /** 16dp. Denser texture, for full-screen backgrounds. */
    Tight,

    /** 24dp. Sparser, for smaller regions where tight would read as a pattern. */
    Wide,
}

/** Requirement 12 criterion 4. Beyond this the pass is thinned by stride, never hidden. */
private const val MAX_DOTS = 5_000
