package com.example.lightapp.designsystem.theme

import androidx.compose.ui.unit.Dp

/**
 * The module boundary — Requirement 15 criterion 4.
 *
 * Design values cross into consumer code only as these value classes. Passing a raw
 * `Dp` where a [Spacing] is expected does not compile, which is a genuine compile-time
 * gate rather than a lint rule.
 *
 * These are the one place a raw Compose dimension type is unwrapped, and they are
 * `@JvmInline` so there is no allocation cost for the safety.
 */

@JvmInline
value class Spacing internal constructor(internal val dp: Dp)

@JvmInline
value class Radius internal constructor(internal val dp: Dp)

@JvmInline
value class Duration internal constructor(internal val millis: Int)

/**
 * The four-level text hierarchy — Requirement 9.
 *
 * Consumer code names a *role*, never a colour. This is why `:app` never needs to
 * reference `Color`: it says [Secondary], and the design system resolves it against
 * the active mode.
 *
 * Ordered display → primary → secondary → disabled. Contrast ratios for every
 * level against every surface are verified in `TokenValidationTest`.
 */
enum class TextRole {
    /** The single hero element per screen. At most one. Requirement 9 criterion 7. */
    Display,

    /** Body text, button labels, given grid numerals. */
    Primary,

    /** Labels, supporting text, de-accented conflict cells. */
    Secondary,

    /** Inactive text, exhausted number-pad digits, pencil marks. */
    Disabled,
}

/**
 * Surface elevation, expressed as colour rather than shadow — Requirement 8 criterion 3.
 *
 * There is no shadow anywhere in this design system. Elevation is a surface colour
 * shift and nothing else.
 */
enum class SurfaceRole {
    /** The base plane every screen sits on. */
    Background,

    /** Cards, grid cells, number pad keys. */
    Surface,

    /** Selected and pressed states of a surface element. */
    SurfaceRaised,
}

/**
 * Border weight. Two levels only.
 *
 * [Visible] is deliberately low-contrast — it measures below 2:1 against the
 * background so the dot-matrix texture cannot rise to reading as content. That
 * maximum is asserted in `TokenValidationTest`. See rationale.md §7.
 */
enum class BorderRole {
    /** Button outlines, 3×3 box grouping. Visible close up, invisible at a glance. */
    Subtle,

    /** Pressed borders, active toggles, dot-matrix texture. */
    Visible,
}
