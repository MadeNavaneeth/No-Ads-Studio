package com.example.lightapp.studio

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.lightapp.BuildConfig
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingBottomNav
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.guard.GuardedScreen
import com.example.lightapp.designsystem.theme.LocalAccentChoice
import com.example.lightapp.designsystem.theme.LocalHapticsEnabled
import com.example.lightapp.designsystem.theme.NothingMotion
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.ThemeMode
import com.example.lightapp.studio.persistence.GameStats

/**
 * The studio shell — Requirement 16 criteria 1 and 2.
 *
 * Navigation is a `Crossfade` over [Screen], which is exactly what the motion rules
 * already require: opacity only, at the screen-transition duration, with the single
 * deceleration easing. A navigation library would have supplied slide transitions that
 * Requirement 13 forbids.
 *
 * Games are passed in explicitly as [games] (the catalog) rather than resolved through a
 * game. The standalone app's build passes a single-element list; a future library build
 * passes the whole catalog.
 *
 * ## Structure
 *
 * A persistent [NothingBottomNav] over three tab destinations, with the game screen as a
 * full-height destination that has no bar. The bar sits outside the `Crossfade` on purpose —
 * inside it, the bar itself would fade out and back in on every tab change, which is both
 * wrong-looking and the opposite of what "persistent" means.
 */
@Composable
fun NothingGamesApp(
    games: List<GameDefinition>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    showRemaining: Boolean,
    onShowRemainingChange: (Boolean) -> Unit,
    showTimer: Boolean,
    onShowTimerChange: (Boolean) -> Unit,
    showPeers: Boolean,
    onShowPeersChange: (Boolean) -> Unit,
    mistakeLimit: Int,
    onMistakeLimitChange: (Int) -> Unit,
    accentChoice: String,
    onAccentChoiceChange: (String) -> Unit,
    autoCleanNotes: Boolean,
    onAutoCleanNotesChange: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    resume: ResumeState? = null,
    resumes: List<ResumeState> = listOfNotNull(resume),
    activeDailies: Set<String> = emptySet(),
    statsFor: (String) -> GameStats,
    onGameComplete: (GameResult) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Both flavours open on Home. What differs is what Home *is*: the studio build shows a
    // library of cards, a standalone build shows that game's start page. See decision D25.
    var screen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Home)
    }

    // System back returns to Home from anywhere else, and exits from Home.
    BackHandler {
        if (screen == Screen.Home) onExit() else screen = Screen.Home
    }

    // A standalone build passes exactly one game and opens on it; a library build
    // passes the whole catalog and opens on the library. Either way the shell decides
    // nothing about which games exist.
    val standalone = games.singleOrNull()

    // The three tab destinations, in bar order. `Screen.Game` is deliberately absent: a
    // puzzle gets the whole screen, and a bar under it would steal height from the board and
    // put a mis-tap out of a game in progress one thumb-width from the number pad.
    //
    // The first tab is named for what the build is. "GAMES" is right for a library and wrong
    // for an app that holds one game; "PLAY" is the reverse.
    val tabs = remember(standalone) {
        listOf(
            NavTab(if (standalone != null) "play" else "games", Screen.Home),
            NavTab("stats", Screen.Stats),
            NavTab("settings", Screen.Settings),
        )
    }
    val activeTab = tabs.firstOrNull { it.screen == screen }

    // The haptics preference reaches components through a composition local rather than
    // through eight layers of parameter. Decision D15 makes haptics the primary feedback
    // channel, so "off" has to actually reach every call site — it previously reached
    // none of them.
    CompositionLocalProvider(
        LocalHapticsEnabled provides hapticsEnabled,
        LocalAccentChoice provides accentChoice,
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                // Sequential fade — old out then new in, no overlap. Crossfade's
                // simultaneous alpha left a 50% double-exposure flash. Splitting the
                // now 200ms transition into 100ms out + 100ms in keeps opacity+colour
                // only (S13) and the single easing curve (S12) but never shows two
                // screens at once — crazy-fast, spontaneous.
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 100,
                                delayMillis = 100,
                                easing = NothingMotion.easing,
                            ),
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = 100,
                                easing = NothingMotion.easing,
                            ),
                        )
                    },
                    label = "screen",
                ) { current ->
                    // Every destination is wrapped, so the composition guards apply per
                    // screen rather than across the whole app. Strict only in debug builds.
                    GuardedScreen(name = current.guardName(), strict = BuildConfig.DEBUG) {
                        when (current) {
                            Screen.Home -> HomeScreen(
                                games = games,
                                onGameSelected = { screen = Screen.Game(it) },
                                onPlay = { screen = Screen.ChooseDifficulty(it) },
                                standalone = standalone,
                                resume = resume,
                                resumes = resumes.ifEmpty { listOfNotNull(resume) },
                                activeDailies = activeDailies,
                                onRules = { screen = Screen.Rules },
                            )

                            Screen.Stats -> StatsScreen(
                                games = games,
                                statsFor = statsFor,
                                onReset = onResetStats,
                            )

                            Screen.Settings -> SettingsScreen(
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                hapticsEnabled = hapticsEnabled,
                                onHapticsChange = onHapticsChange,
                                showRemaining = showRemaining,
                                onShowRemainingChange = onShowRemainingChange,
                                showTimer = showTimer,
                                onShowTimerChange = onShowTimerChange,
                                showPeers = showPeers,
                                onShowPeersChange = onShowPeersChange,
                                autoCleanNotes = autoCleanNotes,
                                onAutoCleanNotesChange = onAutoCleanNotesChange,
                                mistakeLimit = mistakeLimit,
                                onMistakeLimitChange = onMistakeLimitChange,
                                accentChoice = accentChoice,
                                onAccentChoiceChange = onAccentChoiceChange,
                            )

                            is Screen.Game -> {
                                val game = games.find { it.id == current.gameId }
                                if (game == null) {
                                    // A catalog id that no longer resolves. Fail visibly
                                    // rather than showing a blank screen.
                                    NothingBackground {
                                        Label(
                                            "unknown game: ${current.gameId}",
                                            role = TextRole.Disabled,
                                        )
                                    }
                                } else {
                                    game.CreateScreen(
                                        onBack = { screen = Screen.Home },
                                        initialDifficulty = current.difficulty,
                                        onGameComplete = { result ->
                                            logCompletion(result)
                                            onGameComplete(result)
                                        },
                                    )
                                }
                            }

                            Screen.Rules -> RulesScreen(
                                games = games,
                                standalone = standalone,
                                onBack = { screen = Screen.Home },
                            )

                            is Screen.ChooseDifficulty -> {
                                val game = games.find { it.id == current.gameId }
                                if (game == null) {
                                    NothingBackground {
                                        Label(
                                            "unknown game: ${current.gameId}",
                                            role = TextRole.Disabled,
                                        )
                                    }
                                } else {
                                    NothingBackground {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            DifficultyPicker(
                                                // The day's puzzle leads when the game has one:
                                                // it is the one option that is not a difficulty.
                                                // The component stays generic — strings in, choice out.
                                                options = listOfNotNull(game.dailyLabel) + game.difficulties,
                                                current = null,
                                                onSelect = { screen = Screen.Game(current.gameId, it) },
                                                onCancel = { screen = Screen.Home },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Only on a tab. On the game screen this branch is absent entirely, so the board
            // gets the full height rather than being laid out around an invisible bar.
            if (activeTab != null) {
                NothingBottomNav(
                    items = tabs.map { it.label },
                    selected = activeTab.label,
                    onSelect = { label ->
                        tabs.firstOrNull { it.label == label }?.let { screen = it.screen }
                    },
                )
            }
        }
    }
}

/** One bottom-nav destination: the word on the bar, and where it goes. */
private data class NavTab(val label: String, val screen: Screen)

/**
 * Debug-only record that the completion callback fired.
 *
 * `MainActivity` is what actually consumes a [GameResult] — it folds each one into the stats
 * store, which is why the stats page is the real proof the callback works. This log is the
 * cheap version of that proof while working on a device: rule M11 is a review obligation, and
 * a callback nothing observes can stop firing unnoticed, which is exactly what had happened
 * once before the stats screen existed.
 */
private fun logCompletion(result: GameResult) {
    if (BuildConfig.DEBUG) {
        Log.d(
            "NothingGames",
            "completed ${result.gameId} · ${result.difficulty} · " +
                "${result.durationMs}ms · ${result.moves} moves · won=${result.won}",
        )
    }
}

private fun Screen.guardName(): String = when (this) {
    Screen.Home -> "home"
    Screen.Stats -> "stats"
    Screen.Settings -> "settings"
    Screen.Rules -> "rules"
    is Screen.Game -> gameId
    is Screen.ChooseDifficulty -> gameId
}
