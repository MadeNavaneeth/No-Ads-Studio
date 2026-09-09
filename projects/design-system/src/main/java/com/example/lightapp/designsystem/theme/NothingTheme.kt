package com.example.lightapp.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The theme wrapper. **The only `MaterialTheme` call site in the project** —
 * Requirement 8 criterion 6 makes a bare `MaterialTheme` elsewhere a build failure.
 *
 * Material3 is the substrate, not the design language. Its colour scheme is populated
 * from our tokens so that any stray Material component inherits something correct
 * rather than purple, but our own components read [LocalNothingColors] directly and
 * never consult `MaterialTheme.colorScheme`.
 *
 * Every surface is shadow-free: elevation is a surface colour shift and nothing else.
 * Requirement 8 criterion 3.
 */
/**
 * Which colour set to use.
 *
 * [System] follows the OS. When the OS preference cannot be read, dark is the fallback —
 * Requirement 8 criterion 7. Dark is the primary mode here: this design language was
 * built for OLED black, and light is the accommodation.
 */
enum class ThemeMode { System, Dark, Light }

/**
 * Resolves a [ThemeMode] to a concrete light/dark decision.
 *
 * Public because the activity needs the same answer the theme reached, to decide whether
 * the status and navigation bar icons should be drawn light or dark. Duplicating the
 * `when` at that call site is how those two drift apart and you end up with black icons
 * on a black bar.
 */
@Composable
@ReadOnlyComposable
fun ThemeMode.resolvesToDark(): Boolean = when (this) {
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
    ThemeMode.System -> isSystemInDarkTheme()
}

@Composable
fun NothingTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    NothingTheme(darkTheme = mode.resolvesToDark(), content = content)
}

@Composable
fun NothingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = NothingTypography()

    CompositionLocalProvider(
        LocalNothingColors provides colors,
        LocalNothingTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(darkTheme),
            content = content,
        )
    }
}

/**
 * Accessors for our components. Deliberately not exposing `Color` — consumer code
 * names a [TextRole], [SurfaceRole], or [BorderRole] and never touches a colour.
 * That is what keeps `Color` out of `:app` in practice, given the classpath cannot
 * enforce it. See rationale.md §9.
 */
object NothingTheme {
    val colors: NothingColors
        @Composable @ReadOnlyComposable get() = LocalNothingColors.current

    val typography: NothingTypography
        @Composable @ReadOnlyComposable get() = LocalNothingTypography.current

    val spacing: NothingSpacing get() = NothingSpacing
    val radius: NothingRadius get() = NothingRadius
    val shapes: NothingShapes get() = NothingShapes
    val motion: NothingMotion get() = NothingMotion
}

/**
 * Populates Material3 from our tokens so an unstyled Material component degrades to
 * something monochrome rather than to Material defaults.
 *
 * Note what is absent: no `primary` in the Material sense, because this design system
 * has no accent colour by default. Requirement 14 and decision D2 — a screen with
 * nothing wrong has no red on it, and no brand colour at all.
 */
private fun NothingColors.toMaterialScheme(darkTheme: Boolean) = run {
    val bg = surface(SurfaceRole.Background)
    val surf = surface(SurfaceRole.Surface)
    val raised = surface(SurfaceRole.SurfaceRaised)
    val onSurface = text(TextRole.Primary)
    val onSurfaceMuted = text(TextRole.Secondary)
    val outline = border(BorderRole.Subtle)
    val outlineStrong = border(BorderRole.Visible)

    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    base.copy(
        background = bg,
        onBackground = onSurface,
        surface = surf,
        onSurface = onSurface,
        surfaceVariant = raised,
        onSurfaceVariant = onSurfaceMuted,
        outline = outline,
        outlineVariant = outlineStrong,

        // Monochrome: "primary" is just text-on-surface, not a brand hue.
        primary = text(TextRole.Display),
        onPrimary = bg,
        secondary = onSurfaceMuted,
        onSecondary = bg,

        // Red is reserved for error, and only ever as outline or fill.
        error = accentRed,
        onError = text(TextRole.Display),
        errorContainer = accentRedSubtle,
        onErrorContainer = text(TextRole.Primary),

        // No scrim, no tonal elevation overlay — flat only.
        scrim = Color.Transparent,
        surfaceTint = Color.Transparent,
    )
}
