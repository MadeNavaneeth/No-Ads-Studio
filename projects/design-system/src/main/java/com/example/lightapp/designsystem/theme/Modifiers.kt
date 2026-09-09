package com.example.lightapp.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier

/**
 * The boundary API — Requirement 15 criteria 4 and 5.
 *
 * These are the only way consumer code applies a dimension. Each takes a [Spacing],
 * so `Modifier.pad(16.dp)` does not compile: `Dp` is not `Spacing`, and `Spacing`'s
 * wrapped value is `internal` to this module. That is a genuine compile-time gate,
 * unlike the classpath separation which Compose's artifact structure makes impossible
 * (see rationale.md §9).
 *
 * Deliberately absent: any overload accepting a raw `Dp`. Leaving the wrong thing
 * unavailable beats documenting that it is forbidden.
 */

/** Uniform padding on all four edges. */
fun Modifier.pad(all: Spacing): Modifier = padding(all.dp)

/** Asymmetric padding. Omitted axes get none. */
fun Modifier.pad(horizontal: Spacing? = null, vertical: Spacing? = null): Modifier =
    padding(
        PaddingValues(
            horizontal = horizontal?.dp ?: Spacing0.dp,
            vertical = vertical?.dp ?: Spacing0.dp,
        )
    )

/** Per-edge padding. */
fun Modifier.pad(
    start: Spacing? = null,
    top: Spacing? = null,
    end: Spacing? = null,
    bottom: Spacing? = null,
): Modifier = padding(
    start = start?.dp ?: Spacing0.dp,
    top = top?.dp ?: Spacing0.dp,
    end = end?.dp ?: Spacing0.dp,
    bottom = bottom?.dp ?: Spacing0.dp,
)

/** A square of a token size. Used for number-pad keys and icon boxes. */
fun Modifier.sizeOf(spacing: Spacing): Modifier = size(spacing.dp)

/**
 * Gaps between children of a Row or Column.
 *
 * Requirement 11 criterion 5: adjacent element *groups* are separated by
 * [NothingSpacing.xl] or larger, never by a divider line.
 */
object NothingArrangement {
    fun spacedBy(spacing: Spacing): Arrangement.HorizontalOrVertical =
        Arrangement.spacedBy(spacing.dp)
}

/** Zero, for omitted axes. Not a token — there is nothing to derive about zero. */
private val Spacing0 = Spacing(androidx.compose.ui.unit.Dp(0f))
