package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.rememberNothingHaptics

/**
 * Bottom navigation — spec in component-specs.md.
 *
 * ## Words, not icons
 *
 * Every other bottom bar on the platform is a row of pictograms. This one is a row of
 * all-caps tracked mono words, because rule T9 says a word goes wherever a word fits, and a
 * three-item nav is exactly where one fits. It also sidesteps the problem that this project
 * has no icon set and no image assets at all — but the rule came first, and Nothing's own
 * interfaces label things in words for the same reason: a word cannot be misread.
 *
 * ## No divider, and elevation by surface only
 *
 * A top border on a bottom bar is the reflex, and rule P8 forbids it. Separation comes from
 * the bar sitting on `surface` while the screen behind it is `bg` — one perceptual step, which
 * is the only elevation this design language has (rule C1). No shadow, no line.
 *
 * ## Insets
 *
 * The bar consumes the navigation-bar inset itself, so it reaches the bottom edge of the
 * screen while its words stay above the system gesture area. That is why the screens that sit
 * above it inset only their top and sides: padding the bottom in both places would leave a
 * visible dead band. See `NothingGamesApp`.
 *
 * Selection is a text-role change animated over `durationMicro` — colour only, which is all
 * rule S13 allows. Nothing moves and nothing resizes.
 */
@Composable
fun NothingBottomNav(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NothingTheme.colors.surface(SurfaceRole.Surface))
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        items.forEach { item ->
            NavItem(
                label = item,
                selected = item == selected,
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One nav word.
 *
 * Sized to `space64` — the same height as `NothingTopBar`, so the two bars that frame a screen
 * agree — and it fills an equal share of the width, which keeps every touch target far above
 * the 48dp floor even with three or four items.
 */
@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberNothingHaptics()

    val target = if (selected) {
        NothingTheme.colors.text(TextRole.Primary)
    } else {
        NothingTheme.colors.text(TextRole.Disabled)
    }
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = NothingMotion.microSpec(),
        label = "navItem",
    )

    Box(
        modifier = modifier
            .height(NothingSpacing.xxxl.dp)
            .clickable(
                interactionSource = interaction,
                indication = null, // our own feedback; no Material ripple
                role = Role.Tab,
                onClickLabel = label,
            ) {
                haptics.perform(HapticEvent.Tick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        // Goes through Text rather than Label because the colour is animated, and Label
        // resolves its own colour from a role. Upper-casing and the label style are applied
        // here to match Label exactly.
        Text(
            text = if (NothingTheme.typography.labelIsAllCaps) label.uppercase() else label,
            style = NothingTheme.typography.label,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}
