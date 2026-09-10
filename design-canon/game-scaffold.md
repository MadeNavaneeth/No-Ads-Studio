---
status: canonical
---

# Game Scaffold — the code to copy

Companion to `game-design-method.md`. That file decides **what** the game is; this one is **how** to build
it without rediscovering the shape.

Answer the eight questions in `game-design-method.md` first. Then copy the five files below, find-and-replace
two strings, and fill the marked gaps.

> **Find-replace exactly two strings.** `Puzzle` → your class prefix (`Nonogram`), and `puzzle` → your game
> id (`nonogram`). Nothing else in the skeleton needs renaming.

Every file here satisfies all 36 conformance rules as written. If you delete something that looks
unnecessary, run `./gradlew check` before assuming it was.

---

## Where the files go

The app has product flavours (decision D25), so a game gets its **own** source directory — never
`src/main/java`, which is compiled into every flavour and would ship your game inside every app.

```
app/src/gamePuzzle/java/com/example/lightapp/games/puzzle/
  PuzzleDefinition.kt     metadata + the factory. ~40 lines.
  PuzzleState.kt          one immutable position. Derived values computed in the constructor.
  PuzzleViewModel.kt      generation, input, timer, persistence, the completion event.
  PuzzleScreen.kt         every UI state. Insets and scroll are not optional.
  PuzzleRules.kt          pure functions: validity, completion, win. No Android imports.
```

Then three registrations:

1. **`app/build.gradle`** — one `srcDirs` line per flavour that ships the game, plus a `productFlavors`
   entry if it gets its own store listing. See §6.
2. **`src/studio/java/…/core/GameRegistry.kt`** — add it to the library.
3. **`ideas/games-roadmap.md` §4** — enforced by `M5-roadmap-agreement`. A game in code but not in the
   roadmap has skipped the admission criteria, so the build rejects it.

---

## 1 · PuzzleRules.kt — pure logic, no Android

Write this first and test it first. It is the only part with no framework in the way, so it is where bugs are
cheapest to find. `sudoku(9)`'s equivalent has 14 tests and they all run in milliseconds.

```kotlin
package com.example.lightapp.games.puzzle

/**
 * Rules for `puzzle(N)`. Pure functions on a flat cell list — no Android, no Compose,
 * so every one of these is unit-testable on the JVM.
 *
 * Precompute anything called per-cell-per-frame at class load rather than deriving it
 * on each check. `Conflicts.PEERS` in sudoku(9) is the reference: 81 lookup tables built
 * once, instead of three comparisons per cell per pass.
 */
object PuzzleRules {

    const val SIZE = 10               // ← your grid dimension
    const val CELLS = SIZE * SIZE

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE

    /** True when [grid] is a finished, valid solution. Both halves matter. */
    fun isSolved(grid: List<Int>): Boolean {
        // TODO: your win condition.
        // Guard against the empty grid passing — "nothing is wrong yet" is not a win.
        return grid.none { it == 0 } && violations(grid).isEmpty()
    }

    /** Every cell currently breaking a rule. Empty when the board is clean. */
    fun violations(grid: List<Int>): Set<Int> {
        // TODO: your validity check.
        // Deliberately do NOT compare against a stored solution unless the game needs it.
        // sudoku(9) never holds the solved grid in memory, so there is no branch that
        // could leak it — the cost is that a wrong-but-not-yet-contradictory move goes
        // unflagged, which is the honest behaviour of a puzzle that refuses to know.
        return emptySet()
    }
}
```

---

## 2 · PuzzleState.kt — one immutable position

```kotlin
package com.example.lightapp.games.puzzle

import androidx.compose.runtime.Immutable

/**
 * A playable position.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * [givens] and [entries] stay **separate** — rule M8. This is what makes a given
 * uneditable, including across a process restart: there is no code path by which a player
 * value overwrites one. Merging them into a single grid is the most common way this breaks.
 */
@Immutable
data class PuzzleState(
    val givens: List<Int>,
    val entries: List<Int>,
    val difficulty: String,
    val selected: Int? = null,
    val elapsedMs: Long = 0L,
    val mistakes: Int = 0,
    val lastEntered: Int? = null,
) {
    /** Givens and player values as one grid, for rule checks only. Never persisted. */
    val merged: List<Int> = List(PuzzleRules.CELLS) { i ->
        if (givens[i] != 0) givens[i] else entries[i]
    }

    val violations: Set<Int> = PuzzleRules.violations(merged)
    val isComplete: Boolean = PuzzleRules.isSolved(merged)

    /**
     * Progress, 0..1 — this is the quantity from `game-design-method.md` §2.
     *
     * Counts only what the player supplied over what the player was asked to supply, so a
     * fresh board reads 0% and a finished one 100%. Including givens makes a puzzle start
     * at roughly 40%, which is worse than showing no number at all.
     */
    val completion: Float = run {
        val askedFor = givens.count { it == 0 }
        if (askedFor == 0) 1f else entries.count { it != 0 }.toFloat() / askedFor
    }

    fun isGiven(index: Int): Boolean = givens[index] != 0
}

/** One reversible change. In memory only — decision D17. */
data class Move(
    val index: Int,
    val previousEntry: Int,
    val previousMistakes: Int,
    val previousLastEntered: Int?,
) {
    companion object { const val MAX_HISTORY = 50 }
}
```

---

## 3 · PuzzleViewModel.kt — the part with all the traps

Six things here are load-bearing and every one of them was a shipped bug in `sudoku(9)` before it was fixed.
They are marked `⚠` in the comments. Do not simplify them away.

```kotlin
package com.example.lightapp.games.puzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightapp.core.GameResult
import com.example.lightapp.studio.persistence.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What the screen shows. A sealed hierarchy, so "generating and complete" cannot be
 * represented at all.
 *
 * ⚠ [ChoosingDifficulty] carries where to go back to. Storing only the state it came from
 * produced a two-tap dead end: finish a puzzle → NEW → CANCEL landed on a permanent
 * "GENERATING" screen with no generator running and no button on it.
 */
sealed interface PuzzleUi {
    data object Generating : PuzzleUi
    data object GenerationFailed : PuzzleUi
    data class Playing(val state: PuzzleState) : PuzzleUi
    data class ChoosingDifficulty(
        val currentDifficulty: String?,
        val returnTo: PuzzleUi?,
    ) : PuzzleUi
    data class Complete(val state: PuzzleState) : PuzzleUi
}

class PuzzleViewModel(private val store: SessionStore) : ViewModel() {

    private val _ui = MutableStateFlow<PuzzleUi>(PuzzleUi.Generating)
    val ui: StateFlow<PuzzleUi> = _ui.asStateFlow()

    /**
     * ⚠ An event, not state. Replay 0, no buffer. Deriving "you won" from the Complete UI
     * state re-fires every time the screen returns to view, because this ViewModel is
     * scoped to the activity and outlives the screen.
     */
    private val _completions = MutableSharedFlow<GameResult>()
    val completions: SharedFlow<GameResult> = _completions.asSharedFlow()

    private val history = ArrayDeque<Move>()
    private var timerJob: Job? = null
    private var generateJob: Job? = null
    private var moveCount = 0

    /** ⚠ The clock only runs while the screen is on top. See [onScreenResumed]. */
    private var screenResumed = false

    val undoAvailable: Boolean get() = history.isNotEmpty()

    init { viewModelScope.launch { restoreOrGenerate() } }

    // ─── lifecycle ────────────────────────────────────────────────────────────

    /**
     * ⚠ This ViewModel is activity-scoped, not screen-scoped. Without these two, the timer
     * keeps counting while the player sits on Home or Settings, so TIME reports wall-clock
     * since the puzzle started rather than time spent playing.
     */
    fun onScreenResumed() {
        screenResumed = true
        if (_ui.value is PuzzleUi.Playing) startTimer()
    }

    fun onScreenPaused() {
        screenResumed = false
        timerJob?.cancel()
        persistNow()
    }

    private suspend fun restoreOrGenerate() {
        // TODO: read your saved session and validate it means something, not just that it
        // parsed. See SudokuRestore — a well-formed save can still describe a board with no
        // givens, or givens that contradict each other, and restoring one gives the player
        // a grid they can never clear and cannot tell is broken.
        generate(DEFAULT_DIFFICULTY)
    }

    fun generate(difficulty: String) {
        timerJob?.cancel()
        generateJob?.cancel()
        history.clear()
        moveCount = 0
        _ui.value = PuzzleUi.Generating

        generateJob = viewModelScope.launch {
            val puzzle = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    // ⚠ Off the main thread, and cancellable. If your generator blocks in a
                    // way coroutine cancellation cannot interrupt — a Thread.join, a tight
                    // loop — it needs an explicit abort flag it polls, or this timeout is
                    // decorative. sudoku(9)'s did nothing for exactly that reason: it would
                    // wait as long as the generator took, then discard a good puzzle because
                    // the deadline had passed.
                    runCatching { PuzzleGenerator.generate(difficulty) }.getOrNull()
                }
            }

            if (puzzle == null || puzzle.size != PuzzleRules.CELLS) {
                _ui.value = PuzzleUi.GenerationFailed
                return@launch
            }

            val state = PuzzleState(
                givens = puzzle,
                entries = List(PuzzleRules.CELLS) { 0 },
                difficulty = difficulty,
            )
            _ui.value = PuzzleUi.Playing(state)
            if (screenResumed) startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        if (!screenResumed) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val playing = _ui.value as? PuzzleUi.Playing ?: continue
                _ui.value = PuzzleUi.Playing(
                    playing.state.copy(elapsedMs = playing.state.elapsedMs + TICK_MS)
                )
            }
        }
    }

    // ─── input ────────────────────────────────────────────────────────────────

    fun select(index: Int) {
        val playing = _ui.value as? PuzzleUi.Playing ?: return
        _ui.value = PuzzleUi.Playing(playing.state.copy(selected = index))
    }

    /** @return true when the move created a violation, so the screen can vary the haptic. */
    fun enter(value: Int): Boolean {
        val playing = _ui.value as? PuzzleUi.Playing ?: return false
        val state = playing.state
        val index = state.selected ?: return false

        // ⚠ Rule M8. A given rejects input outright.
        if (state.isGiven(index)) return false

        history.addLast(
            Move(index, state.entries[index], state.mistakes, state.lastEntered)
        )
        moveCount++
        while (history.size > Move.MAX_HISTORY) history.removeFirst()

        val next = state.copy(
            entries = state.entries.toMutableList().also { it[index] = value },
            lastEntered = index,
        )
        // A mistake is counted once, at the moment of entry. Correcting it does not decrement.
        val broke = index in next.violations
        commit(if (broke) next.copy(mistakes = next.mistakes + 1) else next)
        return broke
    }

    fun undo() {
        val playing = _ui.value as? PuzzleUi.Playing ?: return
        val move = history.removeLastOrNull() ?: return
        commit(
            playing.state.copy(
                entries = playing.state.entries.toMutableList()
                    .also { it[move.index] = move.previousEntry },
                mistakes = move.previousMistakes,
                lastEntered = move.previousLastEntered,
                selected = move.index,
            )
        )
    }

    // ─── the picker ───────────────────────────────────────────────────────────

    fun showDifficultyPicker() {
        val previous = _ui.value
        if (previous is PuzzleUi.ChoosingDifficulty) return
        timerJob?.cancel()
        generateJob?.cancel()
        _ui.value = PuzzleUi.ChoosingDifficulty(
            currentDifficulty = when (previous) {
                is PuzzleUi.Playing -> previous.state.difficulty
                is PuzzleUi.Complete -> previous.state.difficulty
                else -> null
            },
            // ⚠ Generating is never a destination — the line above just cancelled it.
            returnTo = previous.takeUnless {
                it is PuzzleUi.Generating || it is PuzzleUi.ChoosingDifficulty
            },
        )
    }

    fun cancelDifficultyPicker() {
        val picker = _ui.value as? PuzzleUi.ChoosingDifficulty ?: return
        when (val destination = picker.returnTo) {
            null -> generate(picker.currentDifficulty ?: DEFAULT_DIFFICULTY)
            is PuzzleUi.Playing -> { _ui.value = destination; startTimer() }
            else -> _ui.value = destination
        }
    }

    // ─── plumbing ─────────────────────────────────────────────────────────────

    private fun commit(next: PuzzleState) {
        if (next.isComplete) {
            timerJob?.cancel()
            _ui.value = PuzzleUi.Complete(next)
            viewModelScope.launch {
                // ⚠ Rule M11. sudoku(9) shipped without this for its whole life.
                _completions.emit(
                    GameResult(
                        gameId = PuzzleDefinition.ID,
                        won = true,
                        durationMs = next.elapsedMs,
                        difficulty = next.difficulty,
                        moves = moveCount,
                    )
                )
            }
        } else {
            _ui.value = PuzzleUi.Playing(next)
            persistNow()
        }
    }

    fun persistNow() {
        // TODO: encode and save. Digit strings, not a database — decision D7.
    }

    override fun onCleared() {
        timerJob?.cancel()
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val DEFAULT_DIFFICULTY = "Moderate"
        val DIFFICULTIES = listOf("Simple", "Easy", "Moderate", "Hard", "Challenge")
        private const val TICK_MS = 1_000L
        private const val GENERATION_TIMEOUT_MS = 10_000L
    }
}
```

---

## 4 · PuzzleScreen.kt — insets and scroll are not optional

```kotlin
package com.example.lightapp.games.puzzle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightapp.core.GameResult
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.DotMatrixReadout
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.NothingBackground
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.theme.HapticEvent
import com.example.lightapp.designsystem.theme.NothingArrangement
import com.example.lightapp.designsystem.theme.NothingSpacing
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
import com.example.lightapp.designsystem.theme.pad
import com.example.lightapp.designsystem.theme.rememberNothingHaptics
import com.example.lightapp.studio.persistence.SessionStore

@Composable
fun PuzzleScreen(
    onBack: () -> Unit,
    onComplete: (GameResult) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    val viewModel: PuzzleViewModel = viewModel { PuzzleViewModel(store) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // Rule P18: the gated channel, so the Settings toggle actually reaches here.
    val haptics = rememberNothingHaptics()

    // The clock follows the resumed lifecycle, not composition — this also covers the app
    // being swiped away without the screen ever being disposed.
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { viewModel.onScreenPaused() }
    }

    // Rule M11. Fires once per solve.
    LaunchedEffect(viewModel) {
        viewModel.completions.collect { result ->
            haptics.perform(HapticEvent.Complete)
            onComplete(result)
        }
    }

    NothingBackground {
        // Rule G1: bound to a real quantity. A literal here fails the build, correctly.
        // Outside the insets padding on purpose — the texture reaches the screen edges,
        // only content is inset.
        DotMatrixReadout(
            progress = (ui as? PuzzleUi.Playing)?.state?.completion
                ?: (ui as? PuzzleUi.Complete)?.state?.completion
                ?: 0f,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Rule S17. targetSdk 35 draws behind the system bars whether you asked or not.
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NothingTopBar(
                title = "puzzle(10)",
                onBack = onBack,          // leaving pauses, and pausing saves
                actionLabel = "new",
                onAction = { viewModel.showDifficultyPicker() },
            )

            when (val current = ui) {
                PuzzleUi.Generating -> Centred { Label("generating") }

                PuzzleUi.GenerationFailed -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        Label("generation failed")
                        NothingButton(
                            text = "retry",
                            onClick = { viewModel.generate(PuzzleViewModel.DEFAULT_DIFFICULTY) },
                        )
                    }
                }

                is PuzzleUi.ChoosingDifficulty -> DifficultyPicker(
                    options = PuzzleViewModel.DIFFICULTIES,
                    current = current.currentDifficulty,
                    onSelect = { viewModel.generate(it) },
                    onCancel = { viewModel.cancelDifficultyPicker() },
                )

                is PuzzleUi.Playing -> Board(
                    state = current.state,
                    onCellClick = viewModel::select,
                    onValue = { value ->
                        val broke = viewModel.enter(value)
                        haptics.perform(
                            if (broke) HapticEvent.Conflict else HapticEvent.Tick
                        )
                    },
                )

                is PuzzleUi.Complete -> Centred {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.lg),
                    ) {
                        // The screen's ONE display element — rule T8.
                        NothingText(
                            text = formatTime(current.state.elapsedMs),
                            role = TextRole.Display,
                            style = NothingTheme.typography.displayMedium,
                        )
                        Label("solved · ${current.state.difficulty.lowercase()}")
                        NothingButton(
                            text = "new game",
                            onClick = { viewModel.showDifficultyPicker() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Board(
    state: PuzzleState,
    onCellClick: (Int) -> Unit,
    onValue: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Rule S17. A board as tall as it is wide overflows a short viewport, and a Row
            // or Column given less space than its fixed children need clips them rather
            // than shrinking — the controls simply become unreachable.
            .verticalScroll(rememberScrollState())
            .pad(horizontal = NothingSpacing.md, vertical = NothingSpacing.lg),
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xl),
    ) {
        Readouts(state)
        // TODO: your grid here. Reuse GridCell — weight-based, aspectRatio(1f), never a
        // fixed cell size, so it is correct on any screen.
        // TODO: your input control here.
    }
}

/** Rule M10: exactly three. A fourth `Readout("…")` call fails the build. */
@Composable
private fun Readouts(state: PuzzleState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Readout("time", formatTime(state.elapsedMs))
        Readout("done", "${(state.completion * 100).toInt()}%")
        Readout("miss", state.mistakes.toString())
    }
}

@Composable
private fun Readout(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = NothingArrangement.spacedBy(NothingSpacing.xs),
    ) {
        Label(label)
        // Tabular figures, so the value does not jitter as it counts.
        NothingText(text = value, role = TextRole.Primary, style = NothingTheme.typography.data)
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
```

---

## 5 · PuzzleDefinition.kt — metadata and the factory

```kotlin
package com.example.lightapp.games.puzzle

import androidx.compose.runtime.Composable
import com.example.lightapp.core.GameDefinition
import com.example.lightapp.core.GameIcon
import com.example.lightapp.core.GameResult

class PuzzleDefinition : GameDefinition {
    override val id = ID

    // Parenthetical lowercase — rule M6, decision D4. The number is the defining
    // dimension, not a version.
    override val name = "puzzle(10)"
    override val description = "one lowercase line"

    // Rule G1 applies here too: this dot matrix should say something about the game.
    // A checkerboard is decoration. Prefer a shape that reads as this puzzle specifically.
    override val icon = GameIcon.DotMatrix(
        rows = listOf(
            listOf(true, true, false, false, false),
            listOf(true, true, false, false, false),
            listOf(false, false, true, false, false),
            listOf(false, false, false, true, true),
            listOf(false, false, false, true, true),
        )
    )

    @Composable
    override fun CreateScreen(onBack: () -> Unit, onGameComplete: (GameResult) -> Unit) {
        PuzzleScreen(onBack = onBack, onComplete = onGameComplete)
    }

    companion object {
        /** Also the `gameId` on every GameResult. A constant, so a typo cannot compile. */
        const val ID = "puzzle"
    }
}
```

---

## 6 · Registering it

```groovy
// app/build.gradle — the game directory is shared between flavours, so each flavour that
// ships it opts in. The ABSENCE of a line is what keeps a flavour clean.
sourceSets {
    studio { java.srcDirs += ['src/gamePuzzle/java'] }
    puzzle { java.srcDirs += ['src/gamePuzzle/java'] }   // only if it gets its own listing
}

productFlavors {
    puzzle {                                            // only if it gets its own listing
        dimension 'distribution'
        applicationIdSuffix '.puzzle'
        resValue 'string', 'app_name', 'puzzle(10)'
    }
}
```

```kotlin
// src/studio/java/.../core/GameRegistry.kt — the library build
val games = listOf(
    SudokuDefinition(),
    PuzzleDefinition(),     // ← add
)
```

```kotlin
// src/puzzle/java/.../core/GameRegistry.kt — only for a standalone listing.
// Exactly one game, and `standalone` set so the app opens on it rather than on a
// one-item library.
object GameRegistry {
    val games: List<GameDefinition> = listOf(PuzzleDefinition())
    val standalone: GameDefinition? = games.first()
    fun getById(id: String): GameDefinition? = games.find { it.id == id }
}
```

```markdown
<!-- ideas/games-roadmap.md §4, under the right tier. ENFORCED by rule M5. -->
**`puzzle(10)`** — status `planned`

| | |
|---|---|
| Generator | how puzzles are produced |
| Grid | dimensions and per-cell state |
| Input | the verb, and which existing game you took it from |
| Win | the condition |
| Target | minutes at the default difficulty |
| Data | none, generated — or the size if bundled |
```

Only one game may be `in_progress` at a time (`M3`, enforced). Move the previous one to `implemented` first.

---

## Before you say it works

```bash
cd projects/<game>-app
./gradlew check                  # this project's tests + the 33-rule repo gate
```

Then the part the build cannot do — the `R` list in `rules.md`, and these four:

1. **Squint** at a screenshot. Still identifiably ours, or a generic dark grid?
2. **Grayscale** it. Is every state still readable?
3. **Point at each distinctive element** and name its job. No answer means delete it.
4. **Count the red.** In normal play the answer is zero. Three of the five planned games have no red at all.

And two device checks no gate covers: rotate the screen, and set the system font scale to its maximum. Fixed
sizes that fit at default reflow at 200%, and that is where a layout stops being reachable.
