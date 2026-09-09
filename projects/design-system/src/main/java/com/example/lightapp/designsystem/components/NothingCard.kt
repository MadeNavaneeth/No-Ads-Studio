package com.example.lightapp.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.pad

/**
 * Card — Requirement 8 criterion 3, spec in component-specs.md.
 *
 * **No border and no shadow.** Elevation is the surface colour shift and nothing else.
 * That is the single most-copied-wrongly part of this design language: adding a subtle
 * border to a card is the reflex, and it is wrong here.
 *
 * The 16dp radius stays below the 24dp content padding, so the corner arc never
 * intrudes on content. See rationale.md §4.
 */
@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val fill = if (pressed && onClick != null) {
        NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
    } else {
        NothingTheme.colors.surface(SurfaceRole.Surface)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NothingTheme.shapes.card)
            .background(fill)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                    ) { onClick() }
                } else {
                    Modifier
                }
            )
            .pad(NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
        content = content,
    )
}
