# Working here — the short version

Offline Android puzzle studio, Kotlin + Compose, Nothing OS look. This is the always-on core.
`AGENTS.md` is the full map; `design-canon/rules.md` is the complete rule index. Read those when a
task needs them — this file is what to keep in mind on every task.

## Two commands (from any `projects/<name>/`)

- `./gradlew check` — that project's tests + the repo-root conformance gate (33 rules, wired into every
  project's `check`, D37). **Run this before saying a code task is done.** Every failure prints a `FIX:`
  line with the exact edit to make.
- `./gradlew :conformance:conformanceCheck -Pconformance.fast=true` — the rules only, in seconds, no build needed.

Changed a shared library (`design-system`, `shell`)? Check every consumer:
`for p in projects/*-app; do (cd "$p" && ./gradlew check); done`.

## Never (the build rejects most of these — do not fight it, do what the FIX line says)

- **No `dp`/`sp`/`Color(...)` literal** in an app or the shell. Use a token: `16.dp` → `NothingSpacing.md`
  via `Modifier.pad`/`sizeOf`; a colour → a `TextRole`/`SurfaceRole`/`BorderRole`. Steps: 4 xs · 8 sm · 16 md ·
  24 lg · 32 xl · 48 xxl · 64 xxxl · 96 hero.
- **No new design value outside `design-canon/tokens.json`.** Need a value with no token? Add the token first.
- **No `MaterialTheme {}`** — wrap in `NothingTheme {}`. **No shadow, blur, or gradient** — elevation is a
  surface-colour step. **No divider lines** — separate groups with `NothingSpacing.xl`+.
- **No sharp corners.** Pills, circles, or the card/cell radius only.
- **No spring/bounce/scale/position animation.** Opacity and colour fades only.
- **No permission, no network, no analytics, no image or audio file, no NDot/NType fonts.**
- **No `!!`, no raw `Thread`/`GlobalScope`/`runBlocking`** in our code. **No `LocalHapticFeedback`** — call
  `rememberNothingHaptics().perform(HapticEvent.…)`.
- **Every `*Screen.kt`** applies `windowInsetsPadding(WindowInsets.safeDrawing)` and `verticalScroll(...)`.

## Two ideas that carry the whole look

- **Dots must mean something.** A dot matrix is bound to a real 0..1 quantity (`state.completion`), never a
  constant. A decorative dot grid is the clearest sign of a copy.
- **Red is for error only, and normally there is none.** A screen in normal play has zero red. Never reach
  for it to make a plain screen livelier — plain is correct.

## Adding a game

Start at `design-canon/game-design-method.md` (the eight decisions), then copy `design-canon/game-scaffold.md`
(the files). A game ships from its own project under `projects/<game>-app` — one `GameRegistry`, its own
manifest and entry point, admitted in `ideas/games-roadmap.md` (D36/D37). Game code never lives in
`shell` or `design-system`.

## Before you finish

Run `./gradlew check` from the project you touched. If anything is red, the output names the file, line,
and the fix — apply it and re-run. Do not report a code task complete while a rule fails or a test is red.
The gates cannot judge composition, so also glance at the `R` list at the end of `rules.md`.
