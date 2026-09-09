package com.example.lightapp.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.example.lightapp.designsystem.theme.LocalAccentChoice
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole

/**
 * Slide pagination — a row of Glyph dots that tells you *where* you are.
 *
 * White dot = current slide, grey dot = another is beside it. Same 2dp Glyph
 * dot as the matrix (optical2, G2 Canvas, space16 pitch G3) — vocabulary does
 * not fork, axis just changes from "how much" (progress) to "which one"
 * (index) so G1 stays honest. White is textDisplay (#FFF), grey is
 * textDisabled (#666) — the same two text levels the board uses, so the
 * indicator reads as secondary to the puzzle, not as decoration.
 *
 * Canvas-drawn, single pass, no asset (G2), pitch is a spacing token (G3),
 * consumes no pointer event and exposes no accessibility node (G6).
 */
@Composable
fun SlideDotIndicator(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    if (total <= 1) return
    val clamped = current.coerceIn(0, total - 1)
    val accentChoice = LocalAccentChoice.current
    // White for current, grey for the one beside — unless a TE accent is chosen
    // in Settings, then the active dot becomes that accent (sage/amber) while
    // still normally 0, max 1, non-colour paired like red (C8-C11).
    val activeColor = when (accentChoice) {
        "sage" -> NothingTheme.colors.accentSage
        "amber" -> NothingTheme.colors.accentAmber
        else -> NothingTheme.colors.text(TextRole.Display)
    }
    val inactiveColor = NothingTheme.colors.text(TextRole.Disabled)
    // Spec-legal: optical2 for diameter, space16 (Tight) for pitch.
    val diameter: Dp = NothingSpacing.optical.dp
    val pitch: Dp = NothingSpacing.md.dp
    // Width = last dot centre + radius, height = diameter.
    val width = pitch * (total - 1) + diameter
    val height = diameter

    // Slightly smaller than the full 2dp matrix dot (≈1.44dp) so the
    // pager indicator reads as secondary to the board, yet still Glyph.
    // Diameter token stays optical2 — size reduction is a draw-time scale,
    // not a new literal, so R2/R11 stay green. Box sits below the card,
    // centred, at sm 8dp gap (HomeScreen).
    // Slightly bigger than before (0.72→0.90) per request — still secondary to the
    // 8dp cell radius, but reads without squinting. Token stays optical2.
    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .clearAndSetSemantics { },
    ) {
        val baseRadiusPx = diameter.toPx() / 2f
        val radiusPx = baseRadiusPx * 0.90f
        val pitchPx = pitch.toPx()
        val cy = size.height / 2f
        for (i in 0 until total) {
            val cx = baseRadiusPx + i * pitchPx
            // White dot = this slide, grey dot = another is beside it.
            val color = if (i == clamped) activeColor else inactiveColor
            drawCircle(
                color = color,
                radius = radiusPx,
                center = Offset(cx, cy),
            )
        }
    }
}
