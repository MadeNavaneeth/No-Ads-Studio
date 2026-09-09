package com.example.lightapp.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme

/**
 * Generating pulse — the Glyph at icon scale, breathing.
 * Three 2dp dots on 16dp pitch. Opacity only (150ms micro is not used here;
 * the pulse is a slow 900ms cycle, so it reads as an instrument idle, not a
 * spinner). No spring, no scale (S13). Respects reduced motion by holding
 * end state — caller should gate with LocalReducedMotion if needed.
 */
@Composable
fun DotPulse(
    modifier: Modifier = Modifier,
) {
    val dotColor = NothingTheme.colors.border(BorderRole.Visible)
    val diameter = NothingSpacing.optical.dp
    val pitch = NothingSpacing.md.dp
    val dots = 3
    val width = pitch * (dots - 1) + diameter
    val height = diameter
    val transition = rememberInfiniteTransition(label = "dotPulse")
    // Staggered alpha: 0->1->0 over 900ms, offset per dot.
    // Crazy-fast instrument idle: 600ms cycle, 120ms stagger — still opacity only (S13)
    val phases = List(dots) { i ->
        val delay = i * 120
        transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, delayMillis = delay),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot-$i",
        )
    }

    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .clearAndSetSemantics { },
    ) {
        val radiusPx = diameter.toPx() / 2f
        val pitchPx = pitch.toPx()
        val cy = size.height / 2f
        for (i in 0 until dots) {
            val cx = radiusPx + i * pitchPx
            val alpha by phases[i]
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(cx, cy),
                alpha = alpha,
            )
        }
    }
}
