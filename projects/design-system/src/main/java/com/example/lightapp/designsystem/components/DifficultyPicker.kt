package com.example.lightapp.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.pad

/**
 * Difficulty selection — decision D18.
 *
 * **Not a screen and not an overlay.** This replaces the grid area in place, so the
 * view keeps a single purpose and the navigation graph gains no destination. A player
 * who never wants to choose a difficulty never sees a chooser.
 *
 * Options are generic strings rather than an enum, so the design system stays ignorant
 * of any particular game's difficulty model — Sudoku passes QQWing's five levels,
 * another game could pass three.
 */
@Composable
fun DifficultyPicker(
    options: List<String>,
    current: String?,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pad(NothingSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
    ) {
        Label("difficulty")

        options.forEach { option ->
            NothingButton(
                text = option,
                onClick = { onSelect(option) },
                // The active difficulty carries a brighter border rather than a fill
                // or a tick — no accent colour is spent on a non-error state.
                selected = option == current,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Group separation, not a divider line — Requirement 11.
        Column(
            modifier = Modifier.pad(top = NothingSpacing.xl),
        ) {
            NothingButton(
                text = "cancel",
                onClick = onCancel,
                emphasis = ButtonEmphasis.Quiet,
            )
        }
    }
}
