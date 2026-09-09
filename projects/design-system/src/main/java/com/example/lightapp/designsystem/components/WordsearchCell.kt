package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole

/**
 * One `wordsearch(12)` cell — the spec lives in `component-specs.md` (`WordsearchCell`).
 *
 * Unlike every other grid cell in the studio, the letter is **always visible** — a
 * word search hides positions, not content. So the cell has exactly two states:
 * huntable and found. Found raises the fill (`surfaceRaised`, the shared decided
 * idiom) and stays lettered: the word remains readable in the grid, which is the
 * genre's quiet satisfaction.
 *
 * The drag highlight is a third visual: the line the finger is currently tracing
 * gets the selection outline. It lives on cells because it moves through them, but
 * it is the *grid's* gesture — this component has no click handler at all.
 *
 * No red anywhere (method §6): a wrong trace costs seconds, not lives, and there is
 * no state on this board urgent enough to spend the alarm on.
 */
enum class WordCellValue {
    /** Lettered and huntable. */
    Hidden,

    /** Lettered and found — part of a matched line, inert. */
    Found,
}

@Composable
fun WordsearchCell(
    letter: Char,
    value: WordCellValue,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetFill = if (value == WordCellValue.Found) {
        NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
    } else {
        NothingTheme.colors.surface(SurfaceRole.Surface)
    }
    val animatedFill by animateColorAsState(
        targetValue = targetFill,
        animationSpec = NothingMotion.microSpec(),
        label = "cell",
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(animatedFill)
            .then(
                // The live trace, not a decision: the outline follows the finger and
                // disappears on release, so it never uses the selected-cell ink that
                // means "committed" on the other boards.
                if (selected) {
                    Modifier.border(
                        BorderStroke(
                            StrokeTokens.strokeHairline,
                            NothingTheme.colors.border(BorderRole.Visible)
                        ),
                        NothingTheme.shapes.cell,
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                val state = if (value == WordCellValue.Found) "found" else "hunting"
                contentDescription = "letter $letter, $state"
            },
        contentAlignment = Alignment.Center,
    ) {
        NothingText(
            text = letter.toString(),
            role = if (value == WordCellValue.Found) TextRole.Primary else TextRole.Secondary,
            style = NothingTheme.typography.cellNumeral,
        )
    }
}
