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
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.ThemeMode
import com.example.lightapp.designsystem.theme.resolvesToDark
import com.example.lightapp.studio.NothingGamesApp
import com.example.lightapp.studio.ResumeState
import com.example.lightapp.studio.persistence.GameStats
import com.example.lightapp.studio.persistence.SessionStore
import kotlinx.coroutines.launch

/**
 * Entry point — Requirement 16 criterion 1.
 *
 * Renders the theme and the shell. Nothing else: no game-specific composable, no
 * hardcoded puzzle, no bare `MaterialTheme`. The prototype that used to live here was
 * deleted rather than left as unreferenced code, because two competing Sudoku
 * implementations was the problem this migration exists to fix.
 *
 * Settings come from [SessionStore], so theme mode and haptics survive a cold start,
 * not just a configuration change.
 *
 * ## Edge to edge
 *
 * `targetSdk 35` means Android 15 and above draws the app behind the status and
 * navigation bars whether or not it asked to, so opting in explicitly is the only way to
 * control the result. Without it the 64dp top bar sits under the status bar and the
 * number pad's action row sits under the navigation bar.
 *
 * Insets are consumed by each screen's content rather than here, so a full-bleed
 * background — the Sudoku dot matrix in particular — still reaches the screen edges
 * while text and controls stay clear of the system bars.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Before setContent, so the very first frame is already edge to edge and the
        // layout does not visibly shift once composition settles. The bar icon colours are
        // set again below, once the stored theme preference has been read.
        enableEdgeToEdge()

        setContent {
            val store = remember { SessionStore(applicationContext) }
            val scope = rememberCoroutineScope()

            // Default to System until the stored value arrives. First composition is
            // one frame ahead of disk, which is why the launch budget is not blocked
            // on persistence — Requirement 16 criterion 5.
            val storedMode by store.themeMode.collectAsState(initial = null)
            val hapticsEnabled by store.hapticsEnabled.collectAsState(initial = true)
            val showRemaining by store.showRemaining.collectAsState(initial = true)
            val showTimer by store.showTimer.collectAsState(initial = true)
            val showPeers by store.showPeers.collectAsState(initial = true)
            val mistakeLimit by store.mistakeLimit.collectAsState(initial = 0)
            val accentChoice by store.accentChoice.collectAsState(initial = "none")
            val autoCleanNotes by store.autoCleanNotes.collectAsState(initial = true)

            // Saved games, for the resume cards on the start page. Empty until disk
            // answers — "no saved game" is the safe assumption for that one frame.
            //
            // Mapped to the shell's game-agnostic ResumeState here rather than passed raw,
            // so the shell never learns what a Sudoku session looks like. Each game's id
            // comes from the stored session map itself, so adding a second game needs no
            // change here — MainActivity never hard-codes a game id. The old single
            // `sudokuSession` is still read via `allSessions` with legacy migration inside
            // SessionStore, so an existing install keeps its resume.
            val allSessions by store.allSessions.collectAsState(initial = emptyMap())
            val activeDailies by store.activeDailies.collectAsState(initial = emptySet())
            val resumes = allSessions.map { (gameId, session) ->
                ResumeState(
                    gameId = gameId,
                    difficulty = session.difficulty,
                    progress = session.progress,
                    elapsedMs = session.elapsedMs,
                    mistakes = session.mistakes,
                )
            }
            // Back-compat single for callers that still expect it (NothingGamesApp default).
            val resume = resumes.firstOrNull()

            // One map for every game, so the stats screen needs no per-game wiring and a
            // second game appears there without touching this file.
            val allStats by store.allStats.collectAsState(initial = emptyMap())

            val themeMode = storedMode?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.System

            // The bars follow the app's own theme setting, not the system's. Someone
            // running the OS in light mode with this app forced to Dark still needs
            // light bar icons, and the platform has no way to know that.
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
                    resumes = resumes,
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

/**
 * A fully transparent system bar scrim.
 *
 * `enableEdgeToEdge` wants a packed colour int, not a Compose `Color` — a consumer module
 * may not so much as import that type (Requirement 15 criterion 2). Zero is transparent
 * in every packed ARGB encoding, so the bars stay unpainted and the app's own background
 * shows through, which is the whole point of going edge to edge.
 */
private const val TRANSPARENT = 0
