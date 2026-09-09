package com.example.lightapp.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad

/**
 * Top bar — spec in component-specs.md.
 *
 * **Words, not icons.** The back affordance is the text `BACK`, not a chevron. Nothing
 * labels things, and every icon replaced with a tracked label moves toward their voice.
 * See nothing-study.md §6.
 *
 * No divider beneath it either — Requirement 11 forbids divider lines. Separation is
 * spacing, and the top bar is distinguished by the whitespace below it.
 */
@Composable
fun NothingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val backInteraction = remember { MutableInteractionSource() }
    val actionInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NothingSpacing.xxxl.dp)
            .background(NothingTheme.colors.surface(SurfaceRole.Background))
            .pad(horizontal = NothingSpacing.md),
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                    ) { onBack() },
            ) {
                Label("back")
            }
        }

        Label(
            text = title,
            modifier = Modifier.align(Alignment.Center),
        )

        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        interactionSource = actionInteraction,
                        indication = null,
                    ) { onAction() },
            ) {
                Label(actionLabel, role = TextRole.Primary)
            }
        }
    }
}
