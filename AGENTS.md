# Nothing Games Studio — Agent Guide

Offline-first Android puzzle game studio. Native Kotlin + Jetpack Compose. Zero ads, zero tracking,
zero network, zero ongoing cost. Every game shares one design system: the Nothing OS / Glyph
visual language.

This file is the entry point for any coding agent. It routes to detail rather than restating it.

---

## Router — read this table, then read only what it names

There are ~3,000 lines of canon here. **Do not read it all.** Find your task, open the two or three
documents named, and start. Everything else is reference you can look up when a specific question arises.

| Your task | Read, in this order | Then |
|---|---|---|
| **Add a new game** | `design-canon/game-design-method.md` → `design-canon/game-scaffold.md` | Copy the scaffold, fill the eight answers |
| **Work on a shipped game's mechanics** | `projects/<game>-app/intent.md` → `design-canon/archetypes.md` | Stay inside the recorded contract (D38) |
| **Ship a game as its own app** | `ideas/decisions.md` D36/D37 → a `projects/<game>-app` sibling | Copy a game app, point its registry at the new game |
| **Build a screen for an existing game** | `design-canon/component-specs.md` (the screen's own section) | Implement literally |
| **Add or change a component** | `design-canon/component-specs.md` → `design-canon/nothing-design-system.md` §2 | Add the spec heading *and* the function, or `R5` fails |
| **Change a design value** | `design-canon/rationale.md` (the section for that value) → `design-canon/tokens.json` | Never edit generated code |
| **Fix a bug** | The failing file. Nothing else. | Add a test that fails before your fix |
| **Understand *why* a rule exists** | `design-canon/nothing-study.md` | — |
| **Check your work** | `design-canon/rules.md` — the review list | `./gradlew check` |
| **Orient as a new agent (or model)** | `ideas/agent-playbook.md` — direction, recipes, and hard-won scars | Then this file's router |

**The single most useful thing to know:** run `./gradlew check` from any `projects/<name>/` directory.
It runs that project's tests plus the repo-root conformance gate (`:conformance:check`, composite-wired
into every project's `check` — D37), and reports the file, line, rule id
and authority for every
violation. You do not need to memorise what it covers — it will tell you. Spend your attention on the rules
marked **`R`** in `rules.md`, because those are the only ones it cannot see.

**One game, one app — check the neighbours anyway.** Each game ships from its own project
(`projects/<game>-app`), consuming `projects/design-system` and `projects/shell` as composite builds
(D36). A change to a shared library is not finished until every consuming app is still current:
`for p in projects/*-app; do (cd "$p" && ./gradlew check); done`. When a change is verified on the
emulator, reinstall every affected app (`scripts/run-on-emu.sh <game>` per game). A build that looks
right is not done if a sibling app still runs the previous build — they share one source tree.

**The checker writes the fix, not just the complaint.** Every conformance failure prints a `FIX:` line with
the exact edit — literal-to-token, the missing modifier, the file to move. Do what it says and re-run; you
rarely need to open the authority document. For a fast inner loop that skips the build,
`./gradlew :conformance:conformanceCheck -Pconformance.fast=true` (from any project) runs the rules in a
second or
two.

Two things run automatically so you cannot skip them: an always-on steering file (`.kiro/steering/`) keeps
the non-negotiables in context every turn, and a `Stop` hook (`.kiro/hooks/verify-before-done.json`) reminds
you to run the gate before finishing a code change. Neither replaces running `check` — they make forgetting
harder.

**A local device is available for real checks — decision D28.** `scripts/` (repo root) manages one
lightweight AVD (`nas-fast`) so verifying a change on-device never means manually building and installing
an APK:

```bash
scripts/emu-start.sh              # boot it once per session (~45-75s cold, seconds after)
scripts/run-on-emu.sh sudoku      # build + install + launch + screenshot, one command
scripts/emu-screenshot.sh         # screenshot on demand
scripts/emu-stop.sh               # stop it
```

Two non-obvious host-specific fixes are baked into `emu-start.sh` — `-gpu swiftshader_indirect` and no
`-no-window` — because this host's real config silently returned a black frame from every screenshot
otherwise, on an image that was in every other respect working correctly. See D28 before changing either
flag or switching system images. Release signing is not configured in the composite tree yet; add a throwaway `local-verify.jks`
(gitignored, never the production key) only when a real `bundleRelease` needs exercising.

### The five mistakes to not make

Ranked by how often they actually happened here, not by severity:

1. **Reading the canon as prose and inferring values.** Every number lives in `tokens.json`. If you are
   about to type a `dp`, `sp`, or colour literal in `:app` or `components/`, the build will reject it —
   correctly. Find the token.
2. **Trusting a document over the build.** Docs drift; `tokens.json` and `check` do not. Two canon files
   had accumulated colours that never existed.
3. **Drawing a dot grid because it looks right.** Name the quantity it encodes first. If there is no
   answer, it is wallpaper, and wallpaper is how a copy announces itself.
4. **Reaching for red because a screen looks plain.** Plain is correct. In normal play the count is zero.
5. **Assuming a rule marked `A` in an older revision is enforced.** Twenty-one were not. The column is
   accurate now; keep it that way by adding the gate in the same change as the rule.

---

## Hard rules

These are non-negotiable. They exist because the product promise depends on them, and a
conformance checker enforces most of them mechanically.

**Never add:**

- Any Android permission. The manifest declares zero `uses-permission` elements.
- Any font that is not SIL OFL or equivalent. Nothing's NDot and NType 82 are proprietary.
- Networking, advertising, analytics, or crash-reporting dependencies.
- Raster images (`.png`, `.jpg`, `.webp`, …) or audio files. Use Compose shapes and Canvas.
- Gradients, blurs, or shadow elevation above 0dp. Elevation is expressed through surface color only.
- Sharp corners. Everything is a pill, a circle, or a 16dp/8dp rounded rect.
- Bright or branded colors. The palette is black, white, gray, plus one red for errors only.
- Spring, bounce, or overshoot animation. Motion is 150ms/300ms opacity and color fades.
- React Native, Flutter, or WebView wrappers.
- Hardcoded color, `dp`, or `sp` literals outside the theme package.

**Performance context:** the reference machine is a 2-core / 8 GB MacBook Air. Avoid repeated Gradle
syncs, heavy dependencies, and emulator-plus-IDE-plus-browser at once. Prefer Compose Preview.

Full list with rationale: `ideas/avoid.md`

---

## Repo map

```
AGENTS.md                     this file — instruction entry point for every agent
README.md                     human orientation
CLAUDE.md                  →  AGENTS.md
.github/copilot-instructions.md → AGENTS.md
.claude/skills/nothing-design/SKILL.md → design-canon/nothing-design-system.md

ideas/                        thinking: ideation, research, constraints. Not a source of values.
  decisions.md            ★ every decision resolved + the build order. READ FIRST.
  games-roadmap.md            catalog: admission criteria, tier order, rejected candidates
  learning-library.md         Nothing's own apps + docs, books, tooling, how to study them
  tooling-research.md         vetted OSS tooling (licenses checked) — adopt in tier order (D34)
  design-spec.md              original ideology + Nothing OS research (defers to the canon)
  avoid.md                    the anti-requirements list, with reasons

design-canon/                 authoritative design language
  tokens.json             ★ THE token source. Every design value is authored here, nowhere else.
  architecture.md         ★ where every file goes + the ownership ladder for new files
  component-specs.md      ★ exact specs for every component + every screen. BUILD FROM THIS.
  rules.md                ★ every rule, flat and numbered, with its authority
  rationale.md            ★ why every value is what it is + the honest guarantee ladder
  game-design-method.md   ★ how to translate a NEW game into this language. Start here for game 2+.
  game-scaffold.md        ★ the code to copy for a new game. Companion to the method.
  archetypes.md           ★ the mechanic families games inherit from; blueprint registry (D38).
  nothing-study.md            why Nothing works, do/don't, the four review tests
  sudoku-market-study.md      the most-played sudoku app — mechanisms, refusals, and the
                              two-layer highlight §5A ADOPTED (D30 amended 2026-09-03)
  nothing-tokens.md           prose reference for the token system
  nothing-design-system.md    craft rules. Symlinked as the nothing-design skill.
  resource-map.md             font, icon, and resource provenance + licences
  dependency-denylist.md      barred coordinates, and what is permitted despite looking heavy

projects/                     one Gradle build per project (D36/D37): every game app, plus two
                              shared libraries. Each consumes design-system + shell as
                              includeBuilds and binds the repo-root conformance gate into its
                              own `check`.
  sudoku-app/                 sudoku(9) + the vendored GPL-3.0 QQWing engine.
  nonogram-app/               nonogram(10) — no vendored code, GPL-clean.
  minesweeper-app/            minesweeper(10) — the studio's one live red (a detonation).
  connect-app/                connect(7) — numberlink, construction-built boards.
  wordsearch-app/             wordsearch(12) — the studio's only bundled data (a word pool).
  blockpuzzle-app/            blockpuzzle(8) — endless placement, score-not-win.
  akari-app/                  akari(10) — light-up, uniqueness-proving generator.
  binairo-app/                binairo(10) — Takuzu, deduction-carved generator (D37).
  shell/                      game-agnostic chrome: home/stats/settings/rules, nav, persistence.
  design-system/              tokens generated from design-canon/tokens.json, theme, components.

conformance/                  the repo-root gate build (D37): 33 rules over every project,
                              wired into each project's `check`.

specs/                        feature specs, plain markdown
```

Two rules about this layout. Design values live only in `design-canon/`; `ideas/` explains reasoning
and defers to it. Nothing vendor-specific holds content — `.kiro/specs/` and the two adapter files
above are symbolic links, so there is one copy of everything.

---

## Current state, honestly

Everything below is working code. `sudoku(9)` has been played on a device.

| Item | State | Note |
|---|---|---|
| `ideas/` | implemented | populated, status markers enforced by `X5-status-markers` |
| `design-canon/` | implemented | `tokens.json` is the machine-readable source; canon drift is a build gate |
| `projects/` | implemented | composite layout (D36/D37): eight game apps + shared shell + design-system |
| `specs/` | implemented | harness spec lives here |
| `:design-system` | implemented | token generator, theme, value-class boundary, 19 tests |
| `:design-system/theme` | implemented | colours, type, spacing, shape, motion, gated haptics. `Fonts.kt` is the one placeholder. |
| `app/studio/persistence` | implemented | DataStore with a corruption handler, D7 encoding, 11 codec tests |
| `:design-system/components` | implemented | all 19 documented in `component-specs.md`; agreement is enforced (R5) |
| `app/core/` | implemented | GameDefinition contract + registry |
| `app/games/sudoku/` | implemented | full game, edge-to-edge, lifecycle-aware timer, notes with optional auto-clean, peer highlight, mistake limit, per-digit remaining counts, deduction hints (naked single, never a revealed answer), daily mode (D31), emits its result |
| `app/games/nonogram/` | implemented | 10×10 picture logic: tap/long-press/drag-paint, clue strips, line-solved dimming, daily mode (D32), no red anywhere |
| `app/games/minesweeper/` | implemented | 9×9 field: first-tap-safe layouts placed at first reveal, flood fill, flags, the studio's one live red (a detonation), daily mode (D32, seed = day + first tap) |
| `app/games/connect/` | implemented | 7×7 numberlink: every pair joined and every cell used, drag-to-walk with retractable paths, construction-built Hamiltonian boards (no search), daily mode (D32), no red anywhere |
| `app/games/wordsearch/` | implemented | 12×12 hunt: eight-direction placement with scored overlaps, drag-a-line selection that locks only real words, the studio's first bundled data (a curated word pool), daily mode (D32) |
| `app/games/blockpuzzle/` | implemented | 8×8 endless placement: drag-from-tray with the D33 motion ruling (piece follows the finger, lands by opacity), dot-density piece identity, placeable-deal contract so a dead deal never ends a run, score-not-win, daily mode (D32) |
| `app/games/akari/` | implemented | 10×10 light-up: tap cycles ground→bulb→ground, walls that carry data (the studio's only informative voids), lit ground raised over dark, generator proves uniqueness with a bounded solution counter (the sudoku bar, hand-rolled), daily mode (D32), no red anywhere |
| `app/MainActivity.kt` | implemented | `NothingTheme` + the shell, edge-to-edge, bar icons follow the app's own theme |
| `app/studio/` | implemented | four destinations: home/start page, game, stats, settings. All seven settings (theme, haptics, remaining counts, timer, peer highlight, mistakes, third accent) persist and are honoured. |
| `app/studio/persistence` stats | implemented | per game and difficulty: played, won, best time. Consumes `GameResult` (D26). |

**Build order position: step 9 complete, plus the composite split (D36/D37).** Eight games are
implemented pre-launch; none has been device-playtested in its standalone app yet. The enforcement layer is
the repo-root gate, wired into every project's `check`. Remaining: Paparazzi goldens (step 10),
on-device playtesting per app, and an AGP upgrade to get Android Lint back.

**Known gaps, stated plainly.**

- **`applicationId`s are `com.noadsstudio.<game>`** — decision
  D27, "No Ads Studio". Still change them to a domain you
  own before the first publish if you have one; they are permanent after that. The code `namespace` is still
  `com.example.lightapp` — a compile-time-only id Play never sees, whose alignment is an optional refactor.
- **Android Lint is off** (D24), so there is no automated accessibility or API-level checking.
- **Every test is JVM.** No instrumented tests, no screenshot tests. Rotation and font-scale behaviour is
  verified by hand or not at all.
- ~~`SessionStore` has per-game session methods~~ **resolved by the composite split (D36)**: persistence
  moved into the game-agnostic `shell`; each game app owns its session codec (`SudokuSessionStore`, …),
  one key per game — the shape this gap asked for.
- **Generation time on real hardware is unmeasured.** `Simple` and `Challenge` are the slowest to land.
  Attempts are bounded (8 tries plus a wall-clock timeout, with a silent `MODERATE` fallback), but wall
  time on the reference phone is still unknown.

Each project's `./gradlew check` runs its own unit tests **and** the repo gate (`:conformance:check`),
which enforces **33 rules**:

- **14 source-pattern rules** — literals, forbidden Compose types, bare `MaterialTheme`, forbidden motion,
  dividers, elevation, proprietary fonts, ungated haptics, raw concurrency, `!!`, unbound dot grids,
  screens missing insets or scroll, and a fourth readout.
- **19 structural rules** — harness directories, root markdown, agent symlinks, zero permissions in every
  source-set manifest *and* in every variant's merged manifest, `allowBackup="false"` in every manifest,
  no `package` attribute, no binary assets, the dependency denylist, canon/components agreement, token
  drift in AGENTS.md and in the canon, document status markers, registry/roadmap agreement, a referenced
  ProGuard file, a DataStore corruption handler, the suppression cap, GPL-licensed
  vendored code staying out of the shared source set, no game code in the shell, and project admission
  agreement (one `GameRegistry` per app, admitted in the roadmap).

It fails the build on a violation, reporting the location, rule id and authority. **Every rule has been
verified by injecting the violation it targets and confirming it fails** — not assumed. Ten of them exist
because the defect they catch actually shipped in `sudoku(9)` and had to be fixed by hand. The four
harness gates added in D32 (`allowBackup`, src/main purity, per-flavour admission agreement, standalone
identity) were verified the same way on the day they landed.

Two known toolchain gaps, both caused by JDK 25:
- **Android Lint is disabled** (decision D24). AGP 8.5.2's Lint cannot run on JDK 25. We lose its
  accessibility and API-level checks. An AGP upgrade is the fix.
- Kotlin was upgraded to 2.2.21 for the same reason (D22). If you see "Daemon compilation failed",
  the toolchain has regressed and every build is silently non-incremental.

**Toolchain:** Kotlin 2.2.21 (see decision D22). Incremental builds are ~32s. If you see
"Daemon compilation failed", the toolchain has regressed — that meant every compile was
non-incremental and cost 5+ minutes.

**Fonts:** Nothing's proprietary NDot and NType faces have been **deleted** from the project for
licensing reasons. `res/font/` now holds Space Grotesk and Space Mono as OFL stand-ins, both by the
same foundry as Nothing's real typefaces. Swap in Geist and Doto via `Fonts.kt` when available.

Two things to know before working here:

- **`Fonts.kt` maps to platform stand-ins.** Geist and Doto are not in `res/font/` yet. Everything
  downstream is already correct and picks up the real faces via one edit to that file. The gap that
  actually shows is display type: Space Grotesk has no dot matrix, so hero numerals currently carry none
  of the departure-board reference that makes them Nothing.
- **`rules.md`'s enforcement column is now accurate, and was not before.** Twenty-one rules were marked
  `A` while nothing enforced them. It now distinguishes `A` (the build fails), `S` (the code cannot
  express the violation), and `R` (nothing will catch this — it is your job). Read the consolidated `R`
  list at the end of that file; it is the part automation cannot help you with.

---

## Design language, in brief

Read `design-canon/nothing-tokens.md` for values and
`specs/nothing-games-studio-harness/requirements.md` for the enforceable rules.

- **Surfaces (dark):** background `#000000`, elevated `#141414`, raised `#1F1F1F`, borders `#2A2A2A` / `#3A3A3A`
- **Text, four levels:** display `#FFFFFF`, primary `#E8E8E8`, secondary `#999999`, disabled `#707070`
- **Fonts, three roles (all SIL OFL):** Doto for display and hero numerals (36sp+ only), Geist Sans
  for body and UI, Geist Mono for labels, timers, and data (all-caps, 11–12sp, 0.06–0.1em tracking).
  Tracking is **em, not sp**. Tabular figures on anything that counts.
- **Never ship NDot or NType 82.** They are Nothing's proprietary faces; the `.otf` files in
  `res/font/` are not openly licensed. See the warning in `design-canon/resource-map.md`.
- **Shape:** pill buttons (radius = height/2, 48dp min touch target), 16dp cards, 8dp grid cells
- **Spacing:** 4, 8, 16, 24, 32, 48, 64, 96dp. Separate groups with 32dp+, never a divider line.
- **Texture:** Canvas-drawn dot matrix, 2dp dots on a 16dp or 24dp pitch, 0.10–0.20 alpha
- **Motion:** 150ms micro, 300ms transitions, `cubic-bezier(0.25, 0.1, 0.25, 1)`, opacity and color only
- **Red `#D71921`:** normally **zero** per screen. Nothing's own UI has no accent colour at all. Red
  appears only when something is actually wrong, never more than one element, always paired with a
  non-colour indicator. A screen in normal play has no red on it.

Guiding idea, and it is not minimalism: Nothing's actual principle is **demystification** — expose the
mechanism and make it beautiful. Minimalism hides the inessential; Nothing shows you the engineering.
So show generator state, show why a cell conflicts, treat the timer as an instrument readout. And
**dots must always mean something** — a decorative dot grid is the clearest sign of a copy. Read
`design-canon/nothing-study.md` before making design decisions.

---

## Commands

Run from any `projects/<name>/` — each project is its own Gradle build (D36):

```bash
./gradlew check                     # ← the one to run. That project's tests + the 33-rule repo gate.
./gradlew :conformance:conformanceCheck         # the gate alone (-Pconformance.fast=true for the fast loop)
./gradlew :app:assembleDebug        # that project's debug APK
./gradlew :app:bundleRelease        # unsigned .aab (add signing config before real use)
```

**One game, one project.** Each app under `projects/<game>-app` ships its own `applicationId`
(`com.noadsstudio.<game>`) and store listing. Output lands in
`projects/<game>-app/app/build/outputs/apk/debug/`. Changed a shared library? Check every consumer:
`for p in projects/*-app; do (cd "$p" && ./gradlew check); done`.

**Order matters, and only in one place.** `R4-merged-permissions` reads the *merged* manifest to prove the
installed APK grants nothing — the realistic way the offline promise breaks is a dependency merging a
permission in, which a source scan cannot see. On a clean tree that file does not exist yet, so
`./gradlew clean check` fails with "no merged manifest found — run assembleDebug first". That is the check
refusing to vouch for something it cannot see, which is correct. Put `assembleDebug` first and it passes.

`./gradlew lint` is disabled on this toolchain — AGP 8.5.2's Lint cannot run on JDK 25 (decision D24). It is
not a passing check, it is an absent one. Losing it costs the accessibility and API-level checks, so those
are review obligations until AGP is upgraded.Every project runs its own suite: each game app carries its game's rules/generator/hint/codec tests
plus the shell's shared persistence tests, and `shell` + `design-system` carry their own (token
properties, font coverage). Add tests with logic; a
pure-function file with no test is the cheapest thing in this repo to get wrong.

---

## Building the app

Everything needed is already decided and specified. Do not ask scoping questions — read these three
and build:

1. **`ideas/decisions.md`** — every resolved decision (D1–D37) plus the build order. Nothing is open.
2. **`design-canon/architecture.md`** — the exact file tree for both modules, what belongs in each
   file, the moves required from the current state, and the ownership ladder for anything new.
3. **`design-canon/component-specs.md`** — exact token names, every component, every screen layout
   with spacing tokens. Implement literally.
4. **`design-canon/rules.md`** — every rule in one numbered index. Use as the pre-merge checklist.
5. **`design-canon/rationale.md`** — the derivation behind every value, the token validation tests to
   write, and the honest limits of each enforcement layer. Read before changing any token.
6. **`design-canon/nothing-study.md`** — why the rules exist, and the four review tests.
7. **`ideas/agent-playbook.md`** — the field guide written by the agents who built the games: what
   the project is, where it is going, the distilled recipe for adding a game, and the bugs already
   paid for so you do not pay again.

Adding a **new game** rather than a new screen? Start at **`design-canon/game-design-method.md`**. It
covers the eight translation decisions — the name, the one quantity, the three readouts, what replaces
colour, what the dots encode, where red is allowed, which input verb to reuse, and the paper review —
that all have to be answered before `component-specs.md` has anything to say about your screen.

Key decisions you would otherwise waste a prompt asking about: one Gradle build per game app + two
shared libraries (`projects/design-system`, `projects/shell`) — D36, tokens generated from
`design-canon/tokens.json`, DataStore not Room, hand-rolled navigation
not Navigation-Compose, Geist + Doto fonts, haptics as the feedback channel, games named `sudoku(9)`.

## Conventions

- Kotlin + Compose only. Material3 as the substrate, but always wrapped in the project theme.
- Never call `setContent { MaterialTheme { … } }`. Use the project theme wrapper.
- Adding a game is: a `GameDefinition`, a ViewModel, a Screen, a `GameRegistry`, and an admitted project
  under `projects/` (D36) — see `design-canon/game-scaffold.md`.
- Design values go in `theme/`. If a screen needs a value that has no token, add the token.
- The vendored QQWing engine is GPL-3.0. Keep its license obligation in mind before publishing.
