package com.example.lightapp.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.lightapp.designsystem.guard.LocalDesignGuard
import com.example.lightapp.designsystem.theme.BorderRole
import com.example.lightapp.designsystem.theme.LocalAccentChoice
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.StrokeTokens
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole

/**
 * One cell of a puzzle grid — spec in component-specs.md. Shared by all five games.
 *
 * **Size is never a literal.** The cell fills the width its parent gives it and holds
 * a 1:1 aspect ratio, so an 81-cell grid divides the available width and the 8dp
 * radius stays near its intended ~21% of the cell side.
 *
 * ## Why the conflicted cell is red
 *
 * A conflicting entry is the screen's one urgent signal, and it owns the whole cell:
 * the fill becomes `accentRed` rather than a frame around it. The numeral over the
 * red stays white [TextRole.Display] — white measures 5.19:1 against `#D71921`, above
 * the 4.5:1 WCAG text floor — so the red cell carries no red text (rationale.md §8).
 * Requirement 14 criterion 7 admits red as border, outline, **or shape fill**, and
 * criterion 2 counts a whole filled cell as one element.
 *
 * ## Selected versus the two highlight layers
 *
 * A selection lights two layers ([cross] and [sibling]), and the caller decides what each
 * means per tap: [cross] is the row/column/box of the selection and is lit on **every** tap;
 * [sibling] is the other cells holding the selected digit and is lit only when the selection
 * holds a digit.
 *
 * Two grey fills cannot carry two strengths — that is why the market leader needs blue for
 * its faint tint — and there is no fourth surface level, so the two layers split the
 * palette between them: **selected and [sibling] raise the fill** to `surfaceRaised`, while
 * the **[cross] keeps the base fill and is a soft ring** (`borderSubtle` hairline) around
 * unchanged cells. A faint frame of the row, column and box with the bright digit set
 * inside it reads the way the leader's two-tint board does, without a new token
 * (see `sudoku-market-study.md` §5A).
 *
 * So selection is carried by the **outline**, not by a fourth fill: `borderVisible` when
 * nothing is wrong, yielding to `accentRed` when something is. That ordering matters — a
 * cosmetic border must never displace the screen's one urgent signal.
 */
@Composable
fun GridCell(
    value: String?,
    modifier: Modifier = Modifier,
    state: CellState = CellState.Empty,
    selected: Boolean = false,
    cross: Boolean = false,
    sibling: Boolean = false,
    notes: String? = null,
    onClick: (() -> Unit)? = null,
) {
    // Debug-only: counts red accents so a grid full of conflicts cannot end up with
    // a screen full of red. Requirement 14 criterion 2, decision D2.
    if (state == CellState.ConflictAccented) {
        LocalDesignGuard.current.onRedAccent()
    }

    val cellInteraction = remember { MutableInteractionSource() }

    // Selection and the sibling layer share the raised fill — the two *bright* cells.
    // The cross deliberately keeps the base fill, because a fourth surface level does
    // not exist: two grey fills cannot carry two strengths (that is why the market
    // leader needs blue). So the cross's strength is its ring, not its fill. See the
    // class doc.
    val accentChoice = LocalAccentChoice.current
    val bright = selected || sibling
    val baseFill = if (bright) {
        NothingTheme.colors.surface(SurfaceRole.SurfaceRaised)
    } else {
        NothingTheme.colors.surface(SurfaceRole.Surface)
    }
    // Conflict owns the whole cell: the fill turns accentRed, not a border around it.
    // The numeral above stays white textDisplay — 5.19:1 on #D71921, above the 4.5:1
    // text floor — so the red cell still carries no red text. Conflict outranks
    // selection and both layers, so the red ignores the highlighted fill entirely.
    // When a TE accent is chosen, the *generated* givens carry a whisper of it —
    // 8% sage/amber tint so the 9×9 reads as "generated with sage" without
    // spending an accent as a solid. Still normally 0 (none = no tint), max 1,
    // below text threshold. Lerp keeps the token path visible (R2) and no literal.
    val fill = when {
        state == CellState.ConflictAccented -> NothingTheme.colors.accentRed
        accentChoice == "sage" -> if (state == CellState.Given) {
            androidx.compose.ui.graphics.lerp(baseFill, NothingTheme.colors.accentSage, 0.08f)
        } else baseFill
        accentChoice == "amber" -> if (state == CellState.Given) {
            androidx.compose.ui.graphics.lerp(baseFill, NothingTheme.colors.accentAmber, 0.08f)
        } else baseFill
        else -> baseFill
    }
    // Mass highlight (the 20-cell cross, or a digit set) would otherwise animate many
    // fills at once, violating S16 (≤8 animating) and flashing. Cross and sibling cells
    // snap; only the single selected cell micro-fades. Both still only animate colour (S13).
    val isMassHighlight = (cross || sibling) && !selected
    val animatedFill by animateColorAsState(
        targetValue = fill,
        animationSpec = if (isMassHighlight) {
            androidx.compose.animation.core.snap()
        } else {
            NothingMotion.microSpec()
        },
        label = "cellFill",
    )

    // A conflicted cell is already the whole red signal, so it takes no outline — even
    // when it is also selected or in a layer, the red fill outranks the cosmetics.
    // Below that the strengths are: Visible for the selection, Subtle framed on the
    // sibling digit set, and the cross is a **soft ring only** — its base fill does not
    // change, so a row, column and box read as a faint frame around the bright digit
    // set. A cosmetic outline never displaces the screen's one urgent signal. When a TE
    // accent is chosen, selected and sibling outlines take that accent (C8-C11) so the
    // digit set carries the leader's one hint of colour, on the user's say-so; the cross
    // stays a neutral ring and red stays error-only.
    val outline = when {
        // Red fill is the alarm; nothing frames it.
        state == CellState.ConflictAccented -> null
        state == CellState.ConflictMuted -> NothingTheme.colors.border(BorderRole.Visible)
        selected -> when (accentChoice) {
            "sage" -> NothingTheme.colors.accentSage
            "amber" -> NothingTheme.colors.accentAmber
            else -> NothingTheme.colors.border(BorderRole.Visible)
        }
        sibling -> when (accentChoice) {
            "sage" -> NothingTheme.colors.accentSage
            "amber" -> NothingTheme.colors.accentAmber
            else -> NothingTheme.colors.border(BorderRole.Subtle)
        }
        // The cross layer: base fill unchanged, soft ring only — strength is the ring.
        cross -> NothingTheme.colors.border(BorderRole.Subtle)
        else -> null
    }
    // Outline now animates (colour only, micro) so the frame doesn't pop. Mass layers snap.
    val animatedOutline by animateColorAsState(
        targetValue = outline ?: androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = if (isMassHighlight) {
            androidx.compose.animation.core.snap()
        } else {
            NothingMotion.microSpec()
        },
        label = "cellOutline",
    )
    val hasOutline = outline != null

    val textRole = when (state) {
        // A given is dimmer than a player entry: the player's own marks should read
        // as the active layer.
        CellState.Given -> TextRole.Primary
        CellState.Entered -> TextRole.Display
        CellState.ConflictAccented -> TextRole.Display
        CellState.ConflictMuted -> TextRole.Secondary
        CellState.Empty -> TextRole.Disabled
    }

    // Instrument readout: TalkBack hears value/state, not just "button".
    // Selected/cross/sibling/conflict are encoded in the description so the board is
    // navigable without colour (C11). Value null → empty, notes announced as "notes 1 3".
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(NothingTheme.shapes.cell)
            .background(animatedFill)
            .semantics(mergeDescendants = true) {
                val stateLabel = when (state) {
                    CellState.Given -> "given"
                    CellState.Entered -> "entered"
                    CellState.ConflictAccented -> "conflict"
                    CellState.ConflictMuted -> "conflict muted"
                    CellState.Empty -> if (notes != null) "notes $notes" else "empty"
                }
                val sel = when {
                    selected -> "selected"
                    sibling -> "same digit"
                    cross -> "related"
                    else -> null
                }
                val parts = listOfNotNull(
                    value?.let { "value $it" },
                    if (value == null && notes != null) null else null,
                    stateLabel,
                    sel,
                )
                contentDescription = parts.joinToString(", ")
            }
            .then(
                if (hasOutline) {
                    Modifier.border(
                        BorderStroke(StrokeTokens.strokeHairline, animatedOutline),
                        NothingTheme.shapes.cell,
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    // Every cell is selectable, including a given.
                    //
                    // This used to exclude CellState.Given, on the reasoning that
                    // Requirement 16 criterion 10 makes a given reject input. It does — but
                    // that is about *input*, not selection, and blocking the tap outright
                    // also blocked selecting a given to see where its siblings are. Givens
                    // are most of the filled cells on a fresh board, so that removed the
                    // main use of same-digit highlighting.
                    //
                    // Input is still refused, in the view model where the grids actually
                    // live: both `enter` and `erase` return early on a given, so a tap here
                    // can select but never write.
                    Modifier.clickable(
                        interactionSource = cellInteraction,
                        indication = null,
                    ) { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            value != null -> NothingText(
                text = value,
                role = textRole,
                style = NothingTheme.typography.cellNumeral,
            )

            notes != null -> {
                // Pencil marks — 3×3 positions like paper, not a linear string.
                // Each digit sits where it would in the completed cell, so "1"
                // reads top-left, "5" centre. Up to 9 marks, 0.7 scale via label's
                // small size, Disabled role, no new type size (spec pencil marks
                // excluded from per-screen count).
                val digits = notes.mapNotNull { ch -> ch.digitToIntOrNull()?.takeIf { it in 1..9 } }.toSet()
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (r in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            for (c in 0..2) {
                                val d = r * 3 + c + 1
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (d in digits) {
                                        NothingText(
                                            text = d.toString(),
                                            role = TextRole.Disabled,
                                            style = NothingTheme.typography.label,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cell states. Distinguishing [Given] from [Entered] is load-bearing: a given must be
 * visually distinct *and* reject input, so a restart can never let a player erase one.
 */
enum class CellState {
    /** No value and no notes. */
    Empty,

    /** Part of the generated puzzle. Not editable, ever. */
    Given,

    /** Entered by the player. */
    Entered,

    /**
     * In conflict and carrying the screen's single red accent. At most one cell holds
     * this — Requirement 14 criterion 10 gives it to the most recently entered
     * conflicting cell.
     */
    ConflictAccented,

    /** In conflict but not the accented one. Muted, with a non-colour outline. */
    ConflictMuted,
}
