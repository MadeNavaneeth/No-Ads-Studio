package com.example.lightapp.designsystem.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.lightapp.designsystem.R

/**
 * ⚠️  THE ONLY PLACE FONTS ARE RESOLVED — and currently the only placeholder in the
 *     design system.
 *
 * `design-canon/tokens.json` names three families, all SIL OFL:
 *
 *   - **Doto**        display and hero numerals, 36sp and above only
 *   - **Geist Sans**  body and UI
 *   - **Geist Mono**  labels, timers, data
 *
 * None of the three font files are in `res/font/` yet, and a generated reference to
 * an absent font resource would not compile. So this maps to platform stand-ins for
 * now, and everything downstream — Typography, every component, every screen — is
 * already correct and will pick up the real faces the moment they land.
 *
 * ## To finish this, one edit
 *
 * 1. Place `geist_sans`, `geist_mono`, and variable `doto` in `res/font/`
 * 2. Replace the three `FontFamily.…` stand-ins below with `FontFamily(Font(R.font.…))`
 * 3. Delete `ndot55`, `ndot57`, `ntype82`, `ntype82mono` — those are Nothing's
 *    proprietary faces and shipping them is a licensing risk. See resource-map.md.
 *
 * Nothing else in the module needs to change.
 */
internal fun fontFamilyFor(family: String): FontFamily = when (family) {

    // Dot-matrix display face. The variable dot-size axis is why minSdk is 26 (D19).
    // Space Grotesk has no dot matrix, so display type is the one place the placeholder
    // looks clearly wrong. Everything else is close.
    "Doto" -> SpaceGrotesk

    // Body and UI. Geist is what Nothing OS 5.0 actually ships. Space Grotesk is by
    // Colophon Foundry — the same foundry behind Nothing's real typefaces — so it is a
    // genuinely close stand-in rather than an arbitrary one.
    "Geist Sans" -> SpaceGrotesk

    // Labels, timers, data. Space Mono is a true monospace, so tabular-figure layout
    // and letter-spaced all-caps labels already render at close to final metrics.
    "Geist Mono" -> SpaceMono

    // A family named in tokens.json but not handled here is a defect, not a
    // fallback case. `FontFamilyCoverageTest` catches it before runtime.
    else -> error(
        "No FontFamily mapping for '$family'. Add it to fontFamilyFor() in Fonts.kt, " +
            "or fix the family name in design-canon/tokens.json."
    )
}

/**
 * OFL stand-ins, both by Colophon Foundry. Nothing's proprietary NDot and NType 82
 * faces were deleted from the project — they are not openly licensed and shipping them
 * is a real risk. See the licensing warning in design-canon/resource-map.md.
 */
private val SpaceGrotesk = FontFamily(Font(R.font.space_grotesk_regular))
private val SpaceMono = FontFamily(Font(R.font.space_mono_regular))

/** True while stand-in fonts are in use. Surfaced so tests and tooling can assert on it. */
internal const val FONTS_ARE_PLACEHOLDERS = true
