package com.example.lightapp.studio

import androidx.compose.runtime.saveable.Saver

/**
 * Navigation state — decision D8.
 *
 * Three destinations do not justify a navigation library, and `avoid.md` bars heavy
 * dependencies. A sealed interface switched by `Crossfade` gives us exactly what the
 * motion rules already demand: an opacity-only transition. Navigation-Compose would
 * have brought a dependency, a back stack we do not need, and slide animations that
 * Requirement 13 forbids anyway.
 */
sealed interface Screen {
    data object Home : Screen
    data class Game(val gameId: String, val difficulty: String? = null) : Screen
    data class ChooseDifficulty(val gameId: String) : Screen
    data object Stats : Screen
    data object Settings : Screen

    /**
     * How-to-play — decision D26 amended 2026-09-03. The original decision refused a
     * rules page outright; a later pass reversed it, with the page kept deliberately
     * quiet and reached from Home. Unlike Stats/Settings this is not a tab: it is a
     * full-height destination with its own top bar, and back returns to Home.
     */
    data object Rules : Screen
}

/**
 * Survives process death. A sealed interface is not saveable by default, so this
 * flattens to a route string plus an optional id.
 */
internal val ScreenSaver: Saver<Screen, Any> = Saver(
    save = { screen ->
        when (screen) {
            Screen.Home -> listOf("home", "")
            Screen.Stats -> listOf("stats", "")
            Screen.Settings -> listOf("settings", "")
            Screen.Rules -> listOf("rules", "")
            is Screen.Game -> listOf("game", screen.gameId, screen.difficulty ?: "")
            is Screen.ChooseDifficulty -> listOf("choose", screen.gameId)
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val parts = saved as List<String>
        when (parts[0]) {
            "settings" -> Screen.Settings
            "stats" -> Screen.Stats
            "rules" -> Screen.Rules
            "game" -> Screen.Game(parts[1], parts.getOrNull(2)?.takeIf { it.isNotEmpty() })
            "choose" -> Screen.ChooseDifficulty(parts[1])
            // Home is the fallback on purpose: an unrecognised route restores to a real
            // destination rather than throwing. Saved state does not survive an app update,
            // so a route this build no longer knows is reachable.
            else -> Screen.Home
        }
    },
)
