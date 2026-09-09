package com.example.lightapp.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole

/**
 * One board cell of `blockpuzzle(8)` — spec in component-specs.md.
 *
 * Bi-state by design (parity M1 at its minimum): ground or landed block. There is no
 * error state — a piece that does not fit refuses to land, so nothing here can be
 * wrong and the game spends no red anywhere (method §6).
 *
 * The landed fill is `SurfaceRaised` over ground's `Surface` — the same two-step
 * material the other games' cells use, so a landed piece reads as the same substance
 * the whole studio draws with.
 */
@Composable
fun BlockCell(
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(
                if (filled) NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
                else NothingTheme.colors.surface(SurfaceRole.Surface),
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (filled) "block" else "empty"
            },
    )
}
