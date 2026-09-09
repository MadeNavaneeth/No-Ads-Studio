package com.example.lightapp.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type styles, built from the generated metrics — Requirement 9, derived in rationale.md §6.
 *
 * Two details here are load-bearing and easy to get wrong:
 *
 * **Tracking is `em`, not `sp`.** An em figure is proportional to font size, so one
 * value holds across the scale. Writing `0.08.sp` instead of `0.08.em` yields eight
 * hundredths of a pixel — visually no tracking at all. This was a real bug in the
 * original `NothingTypography.kt`, where every web em value had been transcribed as sp,
 * leaving labels roughly 11× under-tracked.
 *
 * **Tabular figures on anything that counts.** Proportional digits have different
 * advance widths, so a value updating in place visibly jitters. `tnum` fixes the
 * advance. Nothing's instrument-panel character depends entirely on numbers holding
 * still. Enforced by `TokenValidationTest`.
 */
private fun TypeMetrics.toTextStyle(): TextStyle = TextStyle(
    fontFamily = fontFamilyFor(family),
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = trackingEm.em,
    fontWeight = weight?.let { FontWeight(it) } ?: FontWeight.Normal,
    fontFeatureSettings = if (tabularFigures) "tnum" else null,
)

@Immutable
class NothingTypography internal constructor() {

    // ─── Display. Doto, 36sp and above only. Requirement 9 criterion 4. ───
    /** Hero numerals. */
    val displayLarge: TextStyle = TypeTokens.displayLg.toTextStyle()

    /** The one display element on a screen. */
    val displayMedium: TextStyle = TypeTokens.displayMd.toTextStyle()

    /** Section heroes. The dot-matrix floor exactly. */
    val displaySmall: TextStyle = TypeTokens.displaySm.toTextStyle()

    // ─── Body and UI. Geist Sans. ───
    /** Section headings. Light weight so type recedes. */
    val heading: TextStyle = TypeTokens.heading.toTextStyle()

    /** Body text, game names. */
    val body: TextStyle = TypeTokens.body.toTextStyle()

    /**
     * Secondary body. A 1.14 ratio to [body], which is below the just-noticeable
     * difference for type size — so in practice these two are separated by colour,
     * not by size. Recorded rather than hidden. rationale.md §6.
     */
    val bodySmall: TextStyle = TypeTokens.bodySm.toTextStyle()

    // ─── Labels and data. Geist Mono. ───
    /**
     * Every label, button caption, and readout key. All-caps is applied here rather
     * than left to the caller, so a lowercase label is not expressible.
     *
     * All-caps removes ascender and descender cues, so word recognition falls back to
     * letter-by-letter. The tracking buys that separation back.
     */
    val label: TextStyle = TypeTokens.label.toTextStyle()

    /** Timers, counters, scores. Tabular. */
    val data: TextStyle = TypeTokens.data.toTextStyle()

    /** Grid numerals. Tabular, so all nine digits occupy identical width. */
    val cellNumeral: TextStyle = TypeTokens.cellNumeral.toTextStyle()

    /** True when the label style should be rendered upper-case by the component. */
    internal val labelIsAllCaps: Boolean = TypeTokens.label.allCaps
}

internal val LocalNothingTypography = staticCompositionLocalOf { NothingTypography() }
