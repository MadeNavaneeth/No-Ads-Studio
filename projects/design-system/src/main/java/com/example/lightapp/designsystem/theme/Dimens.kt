package com.example.lightapp.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

/**
 * The spacing scale — Requirement 11, derived in rationale.md §2.
 *
 * A doubling spine (4→8→16→32→64) with 1.5× half-steps (24, 48, 96). Smallest
 * adjacent ratio is 1.33, which is the floor at which a difference still reads as
 * intentional rather than as a mistake.
 *
 * Every step has a job no other step does. That is the admission test for a new one.
 */
object NothingSpacing {
    /** Gaps between cells inside a 3×3 box. */
    val xs = Spacing(SpacingTokens.space4)

    /** Gaps between the boxes themselves, number-pad key gaps. */
    val sm = Spacing(SpacingTokens.space8)

    /** Component internal padding, screen edge inset, dot-matrix pitch. */
    val md = Spacing(SpacingTokens.space16)

    /** Card padding, button horizontal padding, wide dot-matrix pitch. */
    val lg = Spacing(SpacingTokens.space24)

    /** Group separation — the minimum that replaces a divider line. */
    val xl = Spacing(SpacingTokens.space32)

    /** Touch target height, number-pad key size, major section separation. */
    val xxl = Spacing(SpacingTokens.space48)

    /** Top bar height. */
    val xxxl = Spacing(SpacingTokens.space64)

    /** Breathing room above and below a display element. */
    val hero = Spacing(SpacingTokens.space96)

    /**
     * The only sub-grid value. Optical alignment offsets and dot diameters only —
     * never padding, gap, or margin. Requirement 11 criterion 2.
     */
    val optical = Spacing(SpacingTokens.optical2)
}

/**
 * Corner radii — Requirement 10, derived in rationale.md §4.
 *
 * Radius is perceptual and must read as a proportion of the element, never as an
 * absolute. See [pillShape] for why the pill value is what it is.
 */
object NothingRadius {
    /**
     * Large enough that the arc is always a true semicircle, so the shape reads as
     * one continuous form rather than a rectangle with rounded corners.
     */
    val pill = Radius(RadiusTokens.radiusPill)

    /** Cards. Stays below the 24dp content padding so the arc never eats content. */
    val card = Radius(RadiusTokens.radiusCard)

    /** Grid cells. ~21% of a typical cell side — mid-band in "rounded square". */
    val cell = Radius(RadiusTokens.radiusCell)
}

/** Shapes, resolved from the radius tokens. No sharp corners exist here. */
object NothingShapes {
    val pill: Shape = RoundedCornerShape(NothingRadius.pill.dp)
    val card: Shape = RoundedCornerShape(NothingRadius.card.dp)
    val cell: Shape = RoundedCornerShape(NothingRadius.cell.dp)
}
