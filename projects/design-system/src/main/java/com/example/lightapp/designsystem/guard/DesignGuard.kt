package com.example.lightapp.designsystem.guard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Debug-only composition guards — the runtime half of the enforcement ladder.
 *
 * Some rules cannot be checked statically because composition is dynamic. "At most one
 * display element per screen" and "at most one red accent per screen" depend on which
 * branches actually run, so no amount of source scanning settles them.
 *
 * These guards count during composition and fail loudly in debug, no-op in release.
 * They catch the state combinations nobody thought to write a test for.
 *
 * Enabled via `NothingTheme(strict = BuildConfig.DEBUG)`.
 */
class DesignGuard internal constructor(
    private val screen: String,
    private val enabled: Boolean,
) {
    private var displayElements = 0
    private var redAccents = 0

    internal fun beginPass() {
        displayElements = 0
        redAccents = 0
    }

    /**
     * Requirement 9 criterion 7: at most one display-level element per screen.
     *
     * More than one and the hierarchy stops communicating — if two things are the most
     * important thing, neither is.
     */
    fun onDisplayElement() {
        if (!enabled) return
        displayElements++
    }

    /**
     * Requirement 14 criterion 2, and decision D2: **zero** normally, at most one when
     * something is actually wrong.
     */
    fun onRedAccent() {
        if (!enabled) return
        redAccents++
    }

    internal fun endPass() {
        if (!enabled) return

        check(displayElements <= 1) {
            "Design rule broken on '$screen': $displayElements display-level elements. " +
                "Requirement 9 criterion 7 allows at most one — if two things are the most " +
                "important thing, neither is."
        }

        check(redAccents <= 1) {
            "Design rule broken on '$screen': $redAccents red accents. " +
                "Requirement 14 allows at most one, and decision D2 expects zero unless " +
                "something is actually wrong. Red's rarity is the source of its power."
        }
    }
}

private val NoOpGuard = DesignGuard(screen = "none", enabled = false)

val LocalDesignGuard = staticCompositionLocalOf { NoOpGuard }

/**
 * Wraps one screen so its composition is checked.
 *
 * Counts reset at the start of every pass and are validated in a [SideEffect] after
 * composition settles, so recomposition does not accumulate false positives.
 */
@Composable
fun GuardedScreen(
    name: String,
    strict: Boolean,
    content: @Composable () -> Unit,
) {
    val guard = remember(name, strict) { DesignGuard(name, strict) }
    guard.beginPass()

    CompositionLocalProvider(LocalDesignGuard provides guard) {
        content()
    }

    SideEffect { guard.endPass() }
}
