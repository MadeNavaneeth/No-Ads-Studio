package com.example.lightapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.lightapp.core.GameRegistry
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.ThemeMode
import com.example.lightapp.designsystem.theme.resolvesToDark
import com.example.lightapp.studio.NothingGamesApp
import com.example.lightapp.studio.ResumeState
import com.example.lightapp.studio.persistence.GameStats
import com.example.lightapp.studio.persistence.MinesweeperSessionStore
import kotlinx.coroutines.launch

/**
 * Entry point for the standalone minesweeper(9) app.
 * Renders the theme and the shell. Nothing else.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val store = remember { MinesweeperSessionStore(applicationContext) }
            val scope = rememberCoroutineScope()

            val storedMode by store.themeMode.collectAsState(initial = null)
            val hapticsEnabled by store.hapticsEnabled.collectAsState(initial = true)
            val showRemaining by store.showRemaining.collectAsState(initial = true)
            val showTimer by store.showTimer.collectAsState(initial = true)
            val showPeers by store.showPeers.collectAsState(initial = true)
            val mistakeLimit by store.mistakeLimit.collectAsState(initial = 0)
            val accentChoice by store.accentChoice.collectAsState(initial = "none")
            val autoCleanNotes by store.autoCleanNotes.collectAsState(initial = true)

            val allSummaries by store.allSummaries.collectAsState(initial = emptyMap())
            val allStats by store.allStats.collectAsState(initial = emptyMap())
            val activeDailies by store.activeDailies.collectAsState(initial = emptySet())

            val resume = allSummaries["minesweeper"]?.let { snapshot ->
                ResumeState(
                    gameId = snapshot.gameId,
                    difficulty = snapshot.difficulty,
                    progress = snapshot.progress,
                    elapsedMs = snapshot.elapsedMs,
                    mistakes = snapshot.mistakes,
                )
            }

            val themeMode = storedMode?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.System

            val darkTheme = themeMode.resolvesToDark()
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(scrim = TRANSPARENT)
                } else {
                    SystemBarStyle.light(scrim = TRANSPARENT, darkScrim = TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            NothingTheme(mode = themeMode) {
                NothingGamesApp(
                    games = GameRegistry.games,
                    themeMode = themeMode,
                    onThemeModeChange = { mode -> scope.launch { store.setThemeMode(mode.name) } },
                    hapticsEnabled = hapticsEnabled,
                    onHapticsChange = { on -> scope.launch { store.setHapticsEnabled(on) } },
                    showRemaining = showRemaining,
                    onShowRemainingChange = { on -> scope.launch { store.setShowRemaining(on) } },
                    showTimer = showTimer,
                    onShowTimerChange = { on -> scope.launch { store.setShowTimer(on) } },
                    showPeers = showPeers,
                    onShowPeersChange = { on -> scope.launch { store.setShowPeers(on) } },
                    mistakeLimit = mistakeLimit,
                    onMistakeLimitChange = { limit -> scope.launch { store.setMistakeLimit(limit) } },
                    accentChoice = accentChoice,
                    onAccentChoiceChange = { choice -> scope.launch { store.setAccentChoice(choice) } },
                    autoCleanNotes = autoCleanNotes,
                    onAutoCleanNotesChange = { on -> scope.launch { store.setAutoCleanNotes(on) } },
                    onResetStats = { scope.launch { store.clearAllStats() } },
                    resume = resume,
                    resumes = listOfNotNull(resume),
                    activeDailies = activeDailies,
                    statsFor = { gameId -> allStats[gameId] ?: GameStats() },
                    onGameComplete = { result ->
                        scope.launch {
                            store.recordResult(
                                gameId = result.gameId,
                                difficulty = result.difficulty,
                                won = result.won,
                                durationMs = result.durationMs,
                            )
                        }
                    },
                    onExit = { finish() },
                )
            }
        }
    }
}

private const val TRANSPARENT = 0
