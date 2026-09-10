---
status: canonical
---

# Architecture & File Placement

Where every file goes and what belongs in it. This is the definitive placement map — if a file is not
listed here, decide its home by the ownership rules in §5 and add it.

Two Gradle modules, per decision D5. Token values come only from `design-canon/tokens.json`, per D6.

---

## 1. Workspace

```
Games/
├── AGENTS.md                     agent entry point (CLAUDE.md, copilot-instructions symlink here)
├── README.md                     human orientation
├── ideas/                        thinking. Never a source of values.
├── design-canon/                 design authority. tokens.json is the only value source.
├── projects/                     buildable projects
└── specs/                        feature specs (.kiro/specs symlinks here)
```

Root markdown is limited to `README.md`, `AGENTS.md`, `CLAUDE.md`. Nothing else.

---

## 2. `projects/` — the Gradle builds

One Gradle build per project (D36), composed into a tree (D37):

```
projects/
├── <game>-app/ × 8          one self-contained app per game; each consumes the two
│                            libraries below as includeBuilds and carries its own
│                            manifest, GameRegistry, and entry point
├── shell/                   ← the game-agnostic chrome (nav, home, stats, settings)
└── design-system/           ← the Design_System_Module

Every one of these builds also includeBuilds the repo-root conformance/ gate,
which binds its 33 rules to each project's `check` (D37).

Each build:
├── settings.gradle          includes ':app' (or ':design-system') + the composite includeBuilds
├── build.gradle             root: plugin versions only, no dependencies
├── gradle.properties
├── gradlew · gradlew.bat · gradle/wrapper/
└── local.properties         gitignored — sdk.dir only
                             (.gitignore lives at the workspace root, covering all of this)
```

The token generator lives in `design-system/build.gradle` (`generateTokens`, reading
`../../design-canon/tokens.json`); the conformance checker lives in `conformance/build.gradle`
— there is no `build-logic/` in the composite layout.

---

## 3. `design-system/` — owns every design value

Only module allowed to reference `Color`, `Dp`, `TextUnit`, or a `dp`/`sp` literal.

```
design-system/
├── build.gradle                  Compose via `implementation`, never `api`
└── src/
    ├── main/
    │   ├── AndroidManifest.xml           no permissions, ever
    │   ├── kotlin/com/example/lightapp/designsystem/
    │   │   ├── theme/
    │   │   │   ├── Types.kt              Spacing, Radius, Duration value classes ← the boundary
    │   │   │   ├── NothingTheme.kt       theme wrapper. The ONLY MaterialTheme call site.
    │   │   │   ├── Colors.kt             maps generated tokens → ColorScheme, both modes
    │   │   │   ├── Typography.kt         maps generated tokens → TextStyle, tabular figures here
    │   │   │   ├── Shapes.kt             pill, card, cell
    │   │   │   └── Motion.kt             durations + the single easing curve
    │   │   ├── components/               one file per component, named as in component-specs.md
    │   │   │   ├── Label.kt
    │   │   │   ├── NothingButton.kt
    │   │   │   ├── NothingCard.kt
    │   │   │   ├── NothingTopBar.kt
    │   │   │   ├── DotMatrixReadout.kt   Canvas only. Takes progress: Float. Never decorative.
    │   │   │   ├── GridCell.kt
    │   │   │   └── NumberPad.kt
    │   │   ├── guard/
    │   │   │   └── DesignGuard.kt        debug-only composition guards. No-op in release.
    │   │   └── Suppression.kt            @DesignSuppression(rule, reason). Max 10 uses.
    │   └── res/
    │       ├── font/                     geist_sans, geist_mono, doto — SIL OFL only
    │       └── values/colors.xml         the ONE colour XML in the whole project
    └── test/kotlin/com/example/lightapp/designsystem/
        ├── TokenValidationTest.kt        Requirement 18. All 12 assertions.
        ├── ContrastTest.kt               computes WCAG ratios, asserts min and the one max
        └── ComponentSpecSheetTest.kt     Paparazzi golden of every component × every state
```

**Do not put here:** navigation, game logic, persistence, anything game-specific.

---

## 4. `app/` — the Consumer_Module

Consumes the design system. No design values authored here.

```
app/
├── build.gradle                  depends on :design-system. No compose-ui declared directly.
└── src/
    ├── main/
    │   ├── AndroidManifest.xml           zero permissions, `namespace` only, no `package` attr
    │   ├── kotlin/com/example/lightapp/
    │   │   ├── MainActivity.kt           setContent { NothingTheme { NothingGamesApp() } }
    │   │   │                             Nothing else. No game code, no hardcoded puzzle.
    │   │   ├── core/
    │   │   │   ├── GameDefinition.kt     the contract
    │   │   │   ├── GameRegistry.kt       the one list
    │   │   │   └── GameResult.kt
    │   │   ├── studio/
    │   │   │   ├── NothingGamesApp.kt    Crossfade over Screen at durationTransition
    │   │   │   ├── Screen.kt             sealed class, rememberSaveable. No Navigation-Compose.
    │   │   │   ├── HomeScreen.kt         renders GameRegistry
    │   │   │   ├── SettingsScreen.kt
    │   │   │   └── persistence/
    │   │   │       ├── SessionStore.kt   DataStore Preferences. No Room, no KSP.
    │   │   │       └── SudokuCodec.kt    the D7 encoding: givens/entries/elapsed/difficulty
    │   │   └── games/sudoku/
    │   │       ├── SudokuDefinition.kt   id "sudoku", name "sudoku(9)"
    │   │       ├── SudokuViewModel.kt    generation off the main thread, survives config change
    │   │       ├── SudokuScreen.kt       layout exactly as component-specs.md §Screen specs
    │   │       ├── SudokuState.kt        givens vs entries kept separate — a given is never editable
    │   │       ├── Conflicts.kt          row/col/box conflict detection
    │   │       └── engine/               vendored QQWing, GPL-3.0, unmodified
    │   └── res/values/strings.xml        no colours here
    └── test/kotlin/com/example/lightapp/
        ├── ArchitectureTest.kt           Requirement 15: forbidden imports, dependency direction
        ├── SudokuCodecTest.kt            round-trip, and a given survives restore
        └── ConflictsTest.kt
```

**Moves required from the current state:**

| From | To |
|---|---|
| `sudoku/core/*` (QQWing) | `games/sudoku/engine/` |
| `theme/*` | `design-system/.../theme/` |
| `NothingSudokuScreen` in `MainActivity.kt` | **deleted** |
| `docs/` | already moved to `design-canon/` |

---

## 5. Ownership rules — how to decide where a new file goes

Apply in order. The first match wins.

1. **Does it state a design value?** → `design-canon/tokens.json`. Nowhere else. Ever.
2. **Does it explain *why* a value is what it is?** → `design-canon/rationale.md`
3. **Does it reference `Color`, `Dp`, `TextUnit`, or a `dp`/`sp` literal?** → `design-system/`
4. **Is it reusable across games?** → `design-system/components/` if visual, `app/studio/` if structural
5. **Is it specific to one game?** → `app/games/<id>/`
6. **Is it third-party vendored code?** → `app/games/<id>/engine/`, unmodified, licence recorded in `design-canon/resource-map.md`
7. **Is it a decision that would otherwise be re-litigated?** → `ideas/decisions.md`
8. **Is it research or reasoning?** → `ideas/`
9. **Is it a testable requirement?** → `specs/<feature>/requirements.md`

---

## 6. The game module pattern

Adding a game is four files and one line.

```kotlin
// app/games/minesweeper/MinesweeperDefinition.kt
class MinesweeperDefinition : GameDefinition {
    override val id = "minesweeper"
    override val name = "minesweeper(10)"        // parenthetical lowercase, D4
    override val description = "Find every mine"
    override val icon = GameIcon.DotMatrix(rows = /* … */)

    @Composable
    override fun CreateScreen(onBack: () -> Unit, onGameComplete: (GameResult) -> Unit) =
        MinesweeperScreen(onBack, onGameComplete)
}
```

Then add it to `GameRegistry` and the home screen picks it up automatically. The roadmap must record it —
an unlisted game fails `M5-roadmap-agreement`.

### Where a game's files actually go — decision D25

The app has product flavours, so "add a game" is four files, **two** registry lines, and a `srcDirs` line
per flavour that ships it:

```
app/src/gameMinesweeper/java/com/example/lightapp/games/minesweeper/
    MinesweeperDefinition.kt   MinesweeperState.kt
    MinesweeperViewModel.kt    MinesweeperScreen.kt   MinesweeperRules.kt
```

```groovy
// app/build.gradle — the game is shared between flavours, which flavour source sets
// cannot do on their own, so each flavour that ships it opts in explicitly.
sourceSets {
    studio      { java.srcDirs += ['src/gameMinesweeper/java'] }
    minesweeper { java.srcDirs += ['src/gameMinesweeper/java'] }
}
productFlavors {
    minesweeper { dimension 'distribution'; applicationIdSuffix '.minesweeper'
                  resValue 'string', 'app_name', 'minesweeper(10)' }
}
```

Add it to `src/studio/java/…/core/GameRegistry.kt` (the library) and, if it gets a standalone listing, to
`src/<flavour>/java/…/core/GameRegistry.kt` with `standalone = games.first()`.

**Nothing game-specific goes in `src/main/java`.** That directory is compiled into every flavour, so a game
placed there ships inside every app whether it belongs or not. For vendored GPL code this is a licensing
problem rather than a size one, and `R4-gpl-isolation` fails the build on it.

Per-game **Gradle modules** (`:games:<id>`) remain the eventual destination once several games exist and
compilation time justifies the boundary. Flavours were the cheaper first step: they deliver the separate
listings and the licence isolation without a module graph to maintain.

---

## 7. Build and check

```bash
cd projects/<name>
./gradlew check                                  # unit tests + the 33-rule gate (composite-wired)
./gradlew :conformance:conformanceCheck -Pconformance.fast=true   # the gate alone, seconds
./gradlew :app:assembleDebug
./gradlew :app:bundleRelease                     # unsigned .aab (add signing before real use)

# design-system only:
./gradlew generateTokens       # tokens.json → generated Kotlin
```

`generateTokens` must run before any compilation that consumes a token. `check` must be a build gate,
not an optional step — the forbidden-type rule is enforcement, not impossibility, so it only works if
it runs.

---

## 8. Cost model

| Item | Cost |
|---|---|
| Play Store registration | $25 one-time |
| Servers, CDN, analytics, ads | $0 — none exist |
| Updates | free via Play |

**Ongoing: $0/month.** This is a design constraint, not just a happy result — it is why there is no
network permission and no backend.

---

## 9. Future: iOS

Game logic and view models are plain Kotlin and move to Kotlin Multiplatform unchanged. `tokens.json`
is platform-neutral, so the same generator emits Swift constants. Compose UI is rewritten in SwiftUI;
the design language translates directly because it is expressed as tokens rather than as Compose code.
The fonts are SIL OFL and work on both platforms.
