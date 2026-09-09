package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics

/**
 * Pill button — Requirement 10, spec in component-specs.md.
 *
 * The radius is half the height by construction, so the arc is a true semicircle and
 * the shape reads as one continuous form rather than a rectangle with rounded corners.
 *
 * Press feedback is border and text brightness only, over one micro duration. No
 * scale, no hue shift — Requirement 13 criterion 6. Since colour and motion are both
 * this constrained, a haptic tick carries most of the felt response (decision D15).
 *
 * The label is a [Label], so the caption is always all-caps tracked mono. A button
 * cannot be given sentence-case text.
 */
@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: ButtonEmphasis = ButtonEmphasis.Normal,
    selected: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = rememberNothingHaptics()

    // Border brightens on press, or stays bright while selected (used by NOTES, and
    // by the current difficulty in DifficultyPicker).
    val targetBorder = when {
        !enabled -> NothingTheme.colors.border(BorderRole.Subtle)
        pressed || selected -> NothingTheme.colors.border(BorderRole.Visible)
        else -> NothingTheme.colors.border(BorderRole.Subtle)
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = NothingMotion.microSpec(),
        label = "buttonBorder",
    )

    val textRole = when {
        !enabled -> TextRole.Disabled
        pressed -> TextRole.Display
        else -> TextRole.Primary
    }

    val fill = when (emphasis) {
        ButtonEmphasis.Normal -> NothingTheme.colors.surface(SurfaceRole.Surface)
        ButtonEmphasis.Quiet -> NothingTheme.colors.surface(SurfaceRole.Background)
    }

    Box(
        modifier = modifier
            // 48dp minimum touch target — Requirement 10 criterion 7.
            .defaultMinSize(minWidth = NothingSpacing.xxl.dp)
            .height(NothingSpacing.xxl.dp)
            .clip(NothingTheme.shapes.pill)
            .background(fill)
            .border(BorderStroke(StrokeTokens.strokeHairline, borderColor), NothingTheme.shapes.pill)
            .clickable(
                interactionSource = interaction,
                indication = null, // our own feedback; no Material ripple
                enabled = enabled,
                role = Role.Button,
            ) {
                haptics.perform(HapticEvent.Tick)
                onClick()
            }
            .pad(horizontal = NothingSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Label(text = text, role = textRole)
    }
}

/**
 * Two levels, not a spectrum. Nothing's buttons differ by surface, not by colour,
 * so there is no "primary" in the Material sense — there is no accent to be primary in.
 */
enum class ButtonEmphasis {
    /** Sits on the background, reads as an action. */
    Normal,

    /** Recedes into the background. Used for CANCEL and secondary actions. */
    Quiet,
}
