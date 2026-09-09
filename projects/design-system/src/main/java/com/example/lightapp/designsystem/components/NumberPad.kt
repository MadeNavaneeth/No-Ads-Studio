package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.sizeOf

/**
 * Number pad — spec in component-specs.md.
 *
 * Nine circular keys plus the three text actions. Every action is a **word**, not an
 * icon: `UNDO`, `ERASE`, `NOTES`.
 *
 * An exhausted digit — one whose [remaining] count has reached zero — renders at
 * [TextRole.Disabled] but stays tappable. Blocking the tap would need an error state, and a
 * dimmed digit already communicates the fact without one. Restraint over feedback.
 *
 * ## The remaining counts
 *
 * [remaining] maps each digit to how many of it are still to be placed, shown as a badge on
 * the key's upper-right at label size. This replaced a plain `exhaustedDigits: Set<Int>`,
 * which was the same information with the interesting part thrown away — "nine of these are
 * gone" is strictly less useful than "two left", and both come from one pass over the grid.
 *
 * It is also the clearest small example of demystification over minimalism
 * (`nothing-study.md` §1): the count was always computable by the player, and showing it
 * exposes the mechanism rather than making them do arithmetic. [showRemaining] exists because
 * some players consider it assistance, so it is a preference rather than a decision made for
 * them.
 *
 * ## Why the row wraps
 *
 * The spec reads "nine keys in one row if width allows, else 5 + 4", and on a phone the
 * width never allows. Nine `space48` keys with eight `space8` gaps need 496dp; a 360dp
 * handset offers 328dp after the screen inset. A `Row` given less space than its fixed
 * children need does not scroll or shrink — it overlaps and clips them, so the outer keys
 * were unreachable on every phone. Hence the measured branch rather than an assumed one.
 *
 * Keys are never shrunk to force a single row: `space48` is the 48dp touch-target floor
 * from Requirement 10 criterion 7, so wrapping is the only move that keeps them tappable.
 *
 * ## Digit haptics belong to the caller
 *
 * [DigitKey] fires no haptic of its own. Only the caller knows whether the digit it just
 * accepted created a conflict, and decision D15 wants those to feel different. A tick
 * here as well would double every pulse. The three action buttons keep their own tick,
 * since pressing them has no outcome to distinguish.
 */
@Composable
fun NumberPad(
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    notesActive: Boolean = false,
    undoAvailable: Boolean = true,
    remaining: Map<Int, Int> = emptyMap(),
    showRemaining: Boolean = true,
    selectedDigit: Int? = null,
    /** Offered only when non-null, so only the game that has a hint engine shows it. */
    onHint: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Nine keys and the eight gaps between them. Measured, not assumed: the
            // same component has to be right on a 360dp phone and an 800dp tablet.
            val oneRowWidth =
                NothingSpacing.xxl.dp * DIGITS + NothingSpacing.sm.dp * (DIGITS - 1)

            // Read out here: inside the Column below, `this` is the ColumnScope and the
            // BoxWithConstraints receiver is no longer reachable implicitly.
            val fitsOneRow = maxWidth >= oneRowWidth

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (fitsOneRow) {
                    DigitRow(1..9, remaining, showRemaining, selectedDigit, onDigit)
                } else {
                    DigitRow(1..5, remaining, showRemaining, selectedDigit, onDigit)
                    DigitRow(6..9, remaining, showRemaining, selectedDigit, onDigit)
                }
            }
        }

        Row(
            horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
        ) {
            NothingButton(
                text = "undo",
                onClick = onUndo,
                enabled = undoAvailable,
                emphasis = ButtonEmphasis.Quiet,
            )
            NothingButton(
                text = "erase",
                onClick = onErase,
                emphasis = ButtonEmphasis.Quiet,
            )
            NothingButton(
                text = "notes",
                onClick = onToggleNotes,
                emphasis = ButtonEmphasis.Quiet,
                selected = notesActive,
            )
            // A fourth word joins only when the caller has a hint to offer. Words
            // over icons (T9), and a null default keeps every other call site —
            // present and future — unchanged.
            if (onHint != null) {
                NothingButton(
                    text = "hint",
                    onClick = onHint,
                    emphasis = ButtonEmphasis.Quiet,
                )
            }
        }
    }
}

/**
 * One row of keys, centred by its parent. Rows are laid out at the key's natural size
 * rather than with `SpaceEvenly`, so a wrapped row of four sits centred under a row of
 * five instead of stretching to the full width.
 */
@Composable
private fun DigitRow(
    digits: IntRange,
    remaining: Map<Int, Int>,
    showRemaining: Boolean,
    selectedDigit: Int?,
    onDigit: (Int) -> Unit,
) {
    Row(horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm)) {
        for (digit in digits) {
            DigitKey(
                digit = digit,
                remaining = remaining[digit],
                showRemaining = showRemaining,
                isSelectedDigit = digit == selectedDigit,
                onClick = { onDigit(digit) },
            )
        }
    }
}

/**
 * One key, with its remaining count as a badge on the upper-right.
 *
 * ## Why the badge lands where it does, with no offset
 *
 * The key is a circle inscribed in a `space48` box, so that box's upper-right *corner* is
 * outside the circle. A `space16` badge aligned to `TopEnd` therefore sits centred on the
 * circle's upper-right arc — the conventional badge position — without needing a nudge, and
 * without extending past the key's own bounds. That last part matters: keys sit `space8`
 * apart, so a badge that overhung horizontally would eat most of the gap to its neighbour on
 * a 360dp phone, where the pad is already the tightest thing on the screen.
 *
 * It also does not collide with the numeral. The digit is centred at `cellNumeral` size and
 * its glyph stops short of x=32dp; the badge starts there.
 *
 * ## Why the badge is `bg` and not a lighter surface
 *
 * `bg` is the screen behind the key, so the badge reads as an aperture punched through the
 * key rather than a sticker on top of it — which is the more Nothing of the two readings, and
 * it is the only choice that stays legible against *both* key states. `surfaceRaised` would
 * have merged into the pressed fill at exactly the moment the player is looking at it.
 *
 * Zero reads `0` at [TextRole.Disabled] rather than disappearing. A blank would be ambiguous
 * with "counts are switched off", and the dimmed digit already says "done with this one" — the
 * two agree instead of one carrying the whole message.
 */
@Composable
private fun DigitKey(
    digit: Int,
    remaining: Int?,
    showRemaining: Boolean,
    isSelectedDigit: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val exhausted = remaining == 0
    val accentChoice = com.example.lightapp.designsystem.theme.LocalAccentChoice.current

    val targetFill = if (pressed) {
        NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
    } else {
        NothingTheme.colors.surface(SurfaceRole.Surface)
    }
    val fill by animateColorAsState(
        targetValue = targetFill,
        animationSpec = NothingMotion.microSpec(),
        label = "padKeyFill",
    )
    // When the board's selected digit is 1, key 1's remaining badge and border
    // pick up the chosen TE accent so the count reads as part of the highlight
    // system — "number of 1's" in green, not just a separate badge.
    val isAccentDigit = isSelectedDigit && accentChoice != "none" && !exhausted

    val accentBorder = when (accentChoice) {
        "sage" -> NothingTheme.colors.accentSage
        "amber" -> NothingTheme.colors.accentAmber
        else -> null
    }
    // The outer box is deliberately unclipped: clipping it to the circle would cut the
    // badge off at exactly the arc it is supposed to sit on.
    Box(modifier = Modifier.sizeOf(NothingSpacing.xxl)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(fill)
                .then(
                    if (isAccentDigit && accentBorder != null) {
                        Modifier.border(
                            androidx.compose.foundation.BorderStroke(
                                com.example.lightapp.designsystem.theme.StrokeTokens.strokeHairline,
                                accentBorder,
                            ),
                            CircleShape,
                        )
                    } else Modifier
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    // Screen readers otherwise announce a bare numeral with no indication
                    // that it is actionable. Android Lint would normally catch this; it is
                    // disabled on this toolchain (D24), so it is stated explicitly.
                    onClickLabel = remaining
                        ?.let { "enter $digit, $it remaining" }
                        ?: "enter $digit",
                ) {
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            NothingText(
                text = digit.toString(),
                role = if (exhausted) TextRole.Disabled else TextRole.Primary,
                style = NothingTheme.typography.cellNumeral,
            )
        }

        if (showRemaining && remaining != null) {
            val badgeBg = if (isAccentDigit && accentBorder != null) accentBorder else NothingTheme.colors.surface(SurfaceRole.Background)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .sizeOf(NothingSpacing.md)
                    .clip(CircleShape)
                    .background(badgeBg)
                    // The count is already in the key's onClickLabel, so announcing it
                    // again here would read the number twice.
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Label(
                    text = remaining.toString(),
                    role = when {
                        isAccentDigit -> TextRole.Display
                        exhausted -> TextRole.Disabled
                        else -> TextRole.Secondary
                    },
                )
            }
        }
    }
}

/** Nine, and it is nine because the grid is 9×9. Not a spacing value. */
private const val DIGITS = 9
