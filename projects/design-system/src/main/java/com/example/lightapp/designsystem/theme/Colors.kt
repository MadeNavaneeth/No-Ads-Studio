package com.example.lightapp.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Resolved colours for the active mode.
 *
 * `@Immutable` matters for performance, not just correctness: Compose can skip
 * recomposition of anything reading this when the instance has not changed.
 * See the Stability in Compose guidance in ideas/learning-library.md §4.
 *
 * Values come from the generated tokens, which come from `design-canon/tokens.json`.
 * Nothing here is authored by hand.
 */
@Immutable
class NothingColors internal constructor(
    private val tokens: ColorTokens,
) {
    internal fun text(role: TextRole): Color = when (role) {
        TextRole.Display -> tokens.textDisplay
        TextRole.Primary -> tokens.textPrimary
        TextRole.Secondary -> tokens.textSecondary
        TextRole.Disabled -> tokens.textDisabled
    }

    internal fun surface(role: SurfaceRole): Color = when (role) {
        SurfaceRole.Background -> tokens.bg
        SurfaceRole.Surface -> tokens.surface
        SurfaceRole.SurfaceRaised -> tokens.surfaceRaised
    }

    internal fun border(role: BorderRole): Color = when (role) {
        BorderRole.Subtle -> tokens.borderSubtle
        BorderRole.Visible -> tokens.borderVisible
    }

    /**
     * Mode-invariant. Requirement 14: assigned to no type style, used only as
     * border, outline, or shape fill. Red itself never carries text — it measures
     * below 4.5:1 on every surface — but a red *fill* may carry white
     * `textDisplay`, which measures 5.19:1 on `#D71921`. Normally absent from a
     * screen entirely; the whole-fill conflict cell is the single exception.
     */
    internal val accentRed: Color get() = FixedColorTokens.accentRed
    internal val accentRedSubtle: Color get() = FixedColorTokens.accentRedSubtle
    internal val accentSage: Color get() = FixedColorTokens.accentSage
    internal val accentSageSubtle: Color get() = FixedColorTokens.accentSageSubtle
    internal val accentAmber: Color get() = FixedColorTokens.accentAmber
    internal val accentAmberSubtle: Color get() = FixedColorTokens.accentAmberSubtle
}

internal val DarkColors = NothingColors(DarkColorTokens)
internal val LightColors = NothingColors(LightColorTokens)

/**
 * `static` because the colour set changes only when the whole theme changes.
 * A non-static local would invalidate every reader on any change.
 */
internal val LocalNothingColors = staticCompositionLocalOf { DarkColors }
