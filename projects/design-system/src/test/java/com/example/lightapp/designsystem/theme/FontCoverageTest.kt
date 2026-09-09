package com.example.lightapp.designsystem.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `fontFamilyFor` calls `error()` for a family it does not recognise, which would be a
 * crash at first render rather than a build failure.
 *
 * Rather than soften that into a silent fallback — which would hide a real mistake — this
 * proves the branch can never be reached: every family named in the token source is
 * handled. Adding a font to `tokens.json` without wiring it up now fails here, on the
 * JVM, instead of on a device.
 */
class FontCoverageTest {

    /** Kept in step with the `when` in Fonts.kt. */
    private val handled = setOf("Doto", "Geist Sans", "Geist Mono")

    private val familiesInUse: Set<String> = listOf(
        TypeTokens.displayLg,
        TypeTokens.displayMd,
        TypeTokens.displaySm,
        TypeTokens.heading,
        TypeTokens.body,
        TypeTokens.bodySm,
        TypeTokens.label,
        TypeTokens.data,
        TypeTokens.cellNumeral,
    ).map { it.family }.toSet()

    @Test
    fun `every font family used by a type style is handled by fontFamilyFor`() {
        val unhandled = familiesInUse - handled
        assertTrue(
            "These families appear in tokens.json but have no branch in fontFamilyFor(), " +
                "which would crash at first render: $unhandled",
            unhandled.isEmpty()
        )
    }

    @Test
    fun `no handled family is unused`() {
        // A stale branch is harmless but means the map has drifted from the tokens.
        val unused = handled - familiesInUse
        assertTrue("fontFamilyFor() handles families no type style uses: $unused", unused.isEmpty())
    }

    @Test
    fun `resolving every family in use does not throw`() {
        familiesInUse.forEach { family ->
            // Would raise IllegalStateException from error() on an unhandled family.
            fontFamilyFor(family)
        }
    }
}
