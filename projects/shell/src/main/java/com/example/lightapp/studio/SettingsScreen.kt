package com.example.lightapp.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.ThemeMode
import com.example.lightapp.designsystem.theme.pad

/**
 * Settings — layout is exactly the Settings spec in component-specs.md.
 *
 * **No display element here.** Settings is not a hero screen, and giving it one would
 * spend the single display slot on something that does not deserve it.
 *
 * Options are pill buttons rather than switches or radio rows: a selected option carries
 * a brighter border, which is the same selection language the difficulty picker uses. No
 * accent colour is spent on a non-error state.
 */
@Composable
fun SettingsScreen(
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
    autoCleanNotes: Boolean,
    onAutoCleanNotesChange: (Boolean) -> Unit,
    mistakeLimit: Int,
    onMistakeLimitChange: (Int) -> Unit,
    accentChoice: String,
    onAccentChoiceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Top and sides only — NothingBottomNav below consumes the navigation-bar
                // inset itself, and padding it twice would leave a dead band above the bar.
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                ),
        ) {
            // No back action: this is a tab, and the bar at the bottom is how you leave it.
            NothingTopBar(title = "settings")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.xl),
                verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
            ) {
                Section(title = "appearance") {
                    Row(
                        horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            NothingButton(
                                text = mode.name,
                                onClick = { onThemeModeChange(mode) },
                                selected = mode == themeMode,
                            )
                        }
                    }
                }

                Section(title = "feedback") {
                    Toggle(value = hapticsEnabled, onChange = onHapticsChange)
                    Label("haptic ticks on cell entry and conflict", role = TextRole.Disabled)
                }

                Section(title = "remaining counts") {
                    Toggle(value = showRemaining, onChange = onShowRemainingChange)
                    Label(
                        "a badge on each number key, showing how many are left",
                        role = TextRole.Disabled,
                    )
                }

                Section(title = "timer") {
                    Toggle(value = showTimer, onChange = onShowTimerChange)
                    // Said plainly, because hiding a clock that is still running would
                    // otherwise be a small deception — and the time still lands on the
                    // stats page either way.
                    Label("the clock runs either way", role = TextRole.Disabled)
                }

                Section(title = "same-digit & peer highlight") {
                    Toggle(value = showPeers, onChange = onShowPeersChange)
                    Label(
                        "the board lights what the selection implicates — a word, same digit, empty peers",
                        role = TextRole.Disabled,
                    )
                }

                Section(title = "pencil auto-clean") {
                    Toggle(value = autoCleanNotes, onChange = onAutoCleanNotesChange)
                    Label(
                        "placing a digit sweeps it from peers' pencil marks — undo restores them",
                        role = TextRole.Disabled,
                    )
                }

                Section(title = "mistakes") {
                    Toggle(
                        value = mistakeLimit == 3,
                        onChange = { on -> onMistakeLimitChange(if (on) 3 else 0) },
                    )
                    Label(
                        if (mistakeLimit == 3) "3 mistakes ends the run — the single red still marks the last"
                        else "unlimited — mistakes count but never end the run",
                        role = TextRole.Disabled,
                    )
                }

                Section(title = "third accent — teenage engineering") {
                    Row(
                        horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        listOf("none", "sage", "amber").forEach { choice ->
                            NothingButton(
                                text = choice,
                                onClick = { onAccentChoiceChange(choice) },
                                selected = accentChoice == choice,
                            )
                        }
                    }
                    Label(
                        when (accentChoice) {
                            "sage" -> "sage #7A9E8B — muted TE green for progress/solved, still normally 0"
                            "amber" -> "amber #C2A878 — mustard TE yellow for highlight, still normally 0"
                            else -> "none — red is still the only error accent (C7 normally 0)"
                        },
                        role = TextRole.Disabled,
                    )
                }

                Section(title = "about") {
                    Label("offline · no ads · no tracking", role = TextRole.Disabled)
                    Label("gpl-3.0 · qqwing", role = TextRole.Disabled)
                    // Blank until the site exists — a button to nowhere never ships.
                    // See StudioLinks: plain ACTION_VIEW, no dependency, no permission.
                    if (StudioLinks.DONATE_URL.isNotBlank()) {
                        val context = LocalContext.current
                        NothingButton(
                            text = "support studio",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(StudioLinks.DONATE_URL),
                                        )
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.md),
        horizontalAlignment = Alignment.Start,
    ) {
        Label(title)
        content()
    }
}

/**
 * A two-pill on/off control.
 *
 * Not a Material `Switch`: a switch is a filled track whose "on" state is carried by a brand
 * colour, and this palette has none to give it. Two pills where the selected one holds a
 * brighter border is the same selection language the difficulty picker and the theme options
 * already use, so the screen has one vocabulary rather than three.
 */
@Composable
private fun Toggle(value: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = NothingArrangement.spacedBy(NothingSpacing.sm)) {
        NothingButton(text = "on", onClick = { onChange(true) }, selected = value)
        NothingButton(text = "off", onClick = { onChange(false) }, selected = !value)
    }
}


