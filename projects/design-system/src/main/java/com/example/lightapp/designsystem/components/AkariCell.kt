package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole

/**
 * One `akari(10)` cell's display value. Owned here, in the design system, so the
 * component never imports game code — the screen maps the game's cell vocabulary
 * onto this at the call site, exactly as minesweeper's does.
 */
enum class AkariCellValue {
    /** White ground, unlit. */
    Dark,

    /** White ground a bulb can see. */
    Lit,

    /** A placed bulb. */
    Bulb,

    /** A blank wall. */
    Wall,

    /** A numbered wall; the demanded count travels in the cell's [clue] parameter. */
    Clue,
}

/**
 * One `akari(10)` cell — the spec lives in `component-specs.md` (`AkariCell`).
 *
 * The board's material story in one component: **walls are voids** (`Background` —
 * the only game whose voids carry data), **ground is surface**, **lit ground is
 * raised** — a two-step material where lit reads *higher*, the opposite of
 * `MinesweeperCell`, because here lighting a cell *adds* information rather than
 * removing hiding. A bulb is a centred dot, the design language's own light source,
 * and a clue numeral sits over its void like minesweeper's count over ground.
 *
 * There is no red and no error state (parity A5): a clash is two lit bulbs, an
 * over-numbered wall is a numeral over more adjacent dots than it names — both
 * visible on the board, neither announced.
 *
 * Taps fire only on white cells; the caller passes a no-op for walls, which is the
 * structural refusal — walls are not in the tap cycle at all.
 */
@Composable
fun AkariCell(
    value: AkariCellValue,
    clue: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = when (value) {
        AkariCellValue.Wall, AkariCellValue.Clue -> NothingTheme.colors.surface(SurfaceRole.Background)
        AkariCellValue.Lit, AkariCellValue.Bulb -> NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
        AkariCellValue.Dark -> NothingTheme.colors.surface(SurfaceRole.Surface)
    }
    val animatedFill by animateColorAsState(
        targetValue = fill,
        animationSpec = NothingMotion.microSpec(),
        label = "cell",
    )
    val isWall = value == AkariCellValue.Wall || value == AkariCellValue.Clue
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(animatedFill)
            .semantics(mergeDescendants = true) {
                contentDescription = when (value) {
                    AkariCellValue.Wall -> "wall"
                    AkariCellValue.Clue -> "wall needing $clue"
                    AkariCellValue.Bulb -> "bulb"
                    AkariCellValue.Lit -> "lit"
                    AkariCellValue.Dark -> "dark"
                }
            }
            .then(
                if (!isWall) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null, // our own feedback; no Material ripple
                        onClick = onTap,
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // The bulb: a centred dot — the shared "something is here" motif, and
            // this game's whole subject. Display hue over the raised fill: the
            // pairing that clears contrast on both themes.
            value == AkariCellValue.Bulb -> Box(
                modifier = Modifier
                    .fillMaxSize(BULB_DOT_FRACTION)
                    .background(
                        NothingTheme.colors.text(TextRole.Display),
                        CircleShape,
                    ),
            )
            // The clue numeral: one hue for every count (method §4) — the digit
            // already distinguishes 0–4, and over a void there is no contrast problem.
            value == AkariCellValue.Clue -> NothingText(
                text = clue.toString(),
                role = TextRole.Primary,
                style = NothingTheme.typography.cellNumeral,
            )
        }
    }
}

private const val BULB_DOT_FRACTION = 0.36f
