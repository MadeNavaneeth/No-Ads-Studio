---
status: canonical
---

# Decision Log

Every decision that would otherwise stall a build. One prompt cannot succeed if the agent has to ask
questions, so everything here is **decided**. Override any of it deliberately, but nothing is left open.

Format: decision, why, and what it costs.

---

## D1 — Target the austere Nothing era, and say so

**Decided:** Flat monochrome, no translucency, no depth, no wallpaper tints. The Nothing OS 2.x/3.x
aesthetic, not Nothing OS 5.0.

**Why:** Nothing OS 5.0 introduced frosted translucency, depth, and colour tints (see
`design-canon/nothing-study.md` §5). A puzzle grid does not want translucency competing with it, and
flat monochrome is still instantly legible as Nothing. Choosing an era on purpose stops the canon
rotting every time Nothing ships a redesign.

**Cost:** We will look slightly dated against a Phone (3) running 5.0. Accepted.

## D2 — Zero accent by default

**Decided:** A screen with nothing wrong has **no red on it**. Red only on conflict, error, or
destructive action, one element maximum, always with a non-colour indicator alongside.

**Why:** Nothing's own UI has no accent colour at all. Rams' economy argument: you subtract to make one
thing louder. Applied to Requirement 14.

**Cost:** Normal play looks very plain. That is correct, not a bug.

## D3 — Keep "no sharp corners", and record that it is ours

**Decided:** Pills, circles, 16dp cards, 8dp cells. No rectangles on visible surfaces.

**Why:** Nothing's own web CTA is a rectangle, so this rule is stricter than their practice. But a grid
game benefits from consistent softness, and absolutism here is cheap to enforce.

**Cost:** One documented divergence from Nothing. Noted, not hidden.

## D4 — Adopt parenthetical lowercase naming

**Decided:** Games are named `sudoku(9)`, `nonogram(10)`, `minesweeper(10)`, `wordsearch(12)`. The
number is the grid dimension. App name stays "Nothing Games".

**Why:** `phone(2a)`, `ear(open)` is Nothing's verbal signature. Free authenticity, instantly readable
to anyone who knows the brand.

**Cost:** None.

## D5 — Two Gradle modules now, not four

**Decided:** `:design-system` and `:app`. Split `:app` into `:games:*` when game two arrives.

**Why:** Four modules for one game is over-engineering, and module count costs real build time on a
2-core machine. Two gets the useful separation; the rest is subdivision, which is easy later.

**Correction, verified:** the original rationale for this decision was that `implementation` rather than
`api` would keep `Color` and `Dp` off the app's compile classpath, making a hex literal an unresolved
reference. **That does not work.** `androidx.compose.ui:ui` exposes `ui-graphics` and `ui-unit` as `api`,
and `Modifier` lives in `ui`, so any module writing Compose gets all three forbidden types regardless.
See `design-canon/rationale.md` §9 for the module metadata proving it.

The enforcement is therefore a static analysis rule bound to the Gradle `check` task — a build failure,
not an unresolved reference. Value classes at the boundary remain a true compile-time gate on the design
system's own API, which is real and worth having.

**Cost:** Game code shares a module with navigation until tier 2, and the forbidden-type gate is
enforcement rather than impossibility.

## D6 — Generate tokens, do not synchronise them

**Decided:** `design-canon/tokens.json` is the only token source. A Gradle task generates
`Tokens.kt` into `build/generated/`. Generated file is never committed and never hand-edited.

**Why:** Drift between canon and code becomes structurally impossible rather than policed. Deletes most
of Requirement 2's comparison machinery.

**Cost:** One Gradle task to write. Worth it.

## D7 — DataStore, not Room

**Decided:** Persist Sudoku state via DataStore Preferences, encoding the grid as a compact string. No
Room, no KSP.

**Why:** Room requires KSP annotation processing, which is slow on a 2-core / 8 GB machine and would
tax every build for the rest of the project. One game's grid state is three strings and a long — it
does not justify a database or a code generator. This directly serves the performance constraints in
`avoid.md`.

**Cost:** Manual encoding, and a migration if a future game needs relational queries. Revisit at tier 2.

**Encoding**, keyed under `sudoku9.` in DataStore:

| Key | Type | Meaning |
|---|---|---|
| `givens` | 81 chars `0-9` | the generated puzzle; `0` is empty. Never mutated. |
| `entries` | 81 chars `0-9` | player values only; `0` is empty. Separate from givens so a given can never be edited. |
| `notes` | 81 fields of 9 bits, `,`-separated ints | pencil marks per cell. Was missing from the first draft while the UI had a NOTES toggle. |
| `elapsedMs` | long | accumulated play time |
| `difficulty` | enum name | one of `Simple`, `Easy`, `Moderate`, `Hard`, `Challenge` |
| `mistakes` | int | see D20 for what counts |

Settings persist in the same DataStore under a `settings.` prefix: `themeMode` (`SYSTEM`/`DARK`/`LIGHT`)
and `hapticsEnabled` (boolean). One store, two key prefixes, no second mechanism.

## D8 — Hand-rolled navigation, no Navigation-Compose

**Decided:** A sealed `Screen` class held in a `rememberSaveable` state, switched with `Crossfade` at
the 300ms transition token.

**Why:** Three destinations (home, game, settings) do not justify a navigation dependency, and
`avoid.md` bans heavy libraries. Crossfade is exactly the opacity-only transition the motion rules
require anyway.

**Cost:** No deep links, no back stack beyond one level. Neither is needed.

## D9 — Testing order: runtime guards now, Paparazzi during Sudoku

**Decided:** Debug-only composition guards ship with the theme work. Paparazzi screenshot tests get set
up once the Sudoku screen has a settled design, not before.

**Why:** Guards are cheap and catch the composition rules immediately. Golden images are worthless until
there is a design worth freezing, and premature goldens mean constant re-approval churn.

**Cost:** A window where visual regressions are uncaught. Acceptable for one screen.

## D10 — Glyph integration deferred, as an optional flavour

**Decided:** Not in tier 1. When built, it goes in a separate product flavour so the base app keeps zero
permissions and universal device support.

**Why:** Strongest possible authenticity signal, but it needs a permission carve-out and restricts to
Nothing devices on Android 14+. That cannot be the baseline.

**Cost:** The most distinctive feature waits. Correct sequencing.

## D11 — Fonts: Geist Sans, Geist Mono, Doto

**Decided:** All three SIL OFL. Nothing's proprietary NDot and NType 82 are removed before any release
build.

**Why:** NType 82 was commissioned for Nothing's exclusive use; the files in `res/font/` are not openly
licensed. Geist is what Nothing OS 5.0 actually ships, so this is more faithful *and* legally safe.
Doto's variable dot-size axis makes the dot matrix animatable.

**Cost:** None. Strict improvement.

## D12 — Dots do a job

**Decided:** The dot matrix is a readout, never texture. Tier 1 use: dot density across the background
encodes completion percentage.

**Why:** In Nothing's language dots always carry information (`nothing-study.md` §3). A decorative dot
grid is the single clearest tell of a copy, and the fastest way to earn the "gimmick" charge.

**Cost:** Slightly more work than a static grid. It is the difference between homage and costume.

## D13 — Curated readouts, not raw state

**Decided:** Expose exactly three readouts on the game screen: elapsed time, completion percentage,
and mistakes. No debug overlays, no generator internals.

**Why:** iFixit's critique is that Nothing's transparency is art-directed rather than honest
(`nothing-study.md` §11). "Expose the mechanism" means compose a deliberate reveal, not dump state.
Three chosen well beat twelve dumped.

**Cost:** Less information on screen. That is the point.

## D14 — SDK levels — ⚠️ SUPERSEDED BY D19

**Superseded.** The `minSdk 24` decided here was raised to **26** by D19, because Compose applies
`FontVariation` only from API 26 and the variable dot-size axis depends on it. Use D19.

Still current from this decision: `compileSdk 35`, `targetSdk 35`, R8 enabled for release.

## D15 — Haptics are the feedback channel

**Decided:** Light tick on cell entry, a distinct heavier pattern on conflict, a short pattern on
completion. Via `LocalHapticFeedback`, no permission, no asset.

**Why:** Colour, scale, and bounce are all banned, which leaves interactions feeling thin. Nothing
devices lean on precise haptics; it is most of the perceived premium and costs nothing.

**Cost:** None. No permission required for `HapticFeedbackType`.

---

## D17 — Undo is included, bounded, and not persisted

**Decided:** An `UNDO` text button alongside `ERASE` and `NOTES`. History holds the last 50 moves in
memory. It is **not** written to DataStore, so a process restart clears it.

**Why:** A mis-tap on a 9×9 grid is punishing without undo, and every Sudoku a player has used has it —
omitting it reads as unfinished rather than as restraint. Bounding at 50 keeps memory trivial, and not
persisting keeps the encoding small and avoids a migration when the history format changes.

**Cost:** Undo does not survive a restart. Acceptable — the grid state does, which is what matters.

## D18 — Difficulty selection replaces the grid, no new screen

**Decided:** Tapping `sudoku(9)` on home starts `Moderate` immediately. `NEW` in the top bar replaces
the grid area in place with five pill buttons — `SIMPLE`, `EASY`, `MODERATE`, `HARD`, `CHALLENGE` — from
the QQWing `GameDifficulty` enum. Choosing one generates and returns to the grid.

**Why:** One purpose per view, and no navigation destination added. A player who never wants to choose
never sees a chooser, which respects the default-difficulty definition in the roadmap. Replacing the grid
rather than overlaying it keeps the screen single-purpose.

**Cost:** Difficulty is not visible on the home screen. Fine — it is shown in the game top bar.

**Amended 2026-09-03 (decision D31):** the in-game picker now leads with `DAILY` — the day's
puzzle — ahead of the five difficulties. `DAILY` is a mode, not a difficulty: it is generated at
`MODERATE` but labelled, slotted and seeded as itself (D31). The amendment is about the picker's
entries, not the difficulty model.

## D19 — minSdk 26, keep Doto variable

**Decided:** Raise `minSdk` from 24 to **26**. Bundle the variable Doto so the dot-size axis is
animatable.

**Why:** Compose applies `FontVariation` settings only from API 26; below that they are silently ignored,
so on 24–25 the dot-size axis would do nothing. That axis is what makes the functional dot matrix of D12
possible, and D12 is the most distinctive thing in the app. Trading Android 7.x — negligible share in
2026 — for the signature feature is the right way round. It also removes a silent-degradation path, which
is worse than an honest floor.

**Cost:** Drops API 24–25. Supersedes the `minSdk 24` in D14.

## D20 — Mistake and completion semantics

**Decided:**

- A **mistake** is counted when the player commits a value into an empty cell that conflicts with an
  existing value in the same row, column, or 3×3 box. Counted once at the moment of entry. Correcting the
  cell does not decrement it. Notes never count.
- **Completion** is `filledPlayerCells / initiallyEmptyCells`, expressed 0–100%. Givens are excluded from
  both terms, so a fresh puzzle reads 0% and a finished one reads 100%.

**Why:** Both were referenced by the game screen readout and by `DotMatrixReadout.progress` without ever
being defined. Conflict-based rather than solution-based mistake counting means the app never has to hold
the solution in memory, which also removes a way to cheat by reading it.

**Cost:** A player can enter a value that is wrong but not yet conflicting and it is not flagged. That is
the honest behaviour of a Sudoku that refuses to know the answer.

## D21 — Games only. Do not generalise the harness.

**Decided:** The harness stays game-shaped. `GameDefinition`, `GameRegistry`, the roadmap, and the
puzzle-specific admission criteria of Requirements 6 and 7 remain as they are. No `AppModule`
abstraction, no splitting admission criteria into shared and game-specific sets.

**Why:** One concrete case cannot teach you the right abstraction. Generalising on a guess produces the
wrong seams and you pay the rework anyway, on top of having carried the complexity meanwhile. The
genuinely reusable asset — `:design-system` — is already app-agnostic and would serve a non-game app
unchanged, so nothing is lost by waiting.

**Cost:** A future non-game app needs a rename and a criteria split. Cheaper then, with a real second
case in hand, than now on speculation.

**Revisit when:** a non-game app is actually specified, not merely considered.

## D23 — A zero-dependency Gradle conformance task, not detekt plus Konsist

**Decided:** The static enforcement is a Groovy Gradle task in `conformance.gradle`, bound to `check`
in each module. No detekt, no Konsist.

**Why:** Requirement 15 and decision D9 named detekt and Konsist. Both mean a plugin, and a *custom*
detekt rule additionally means a separate module compiled against `detekt-api` — for what is
ultimately eight source patterns. `avoid.md` bars heavy dependencies and build cost on the reference
machine is a stated constraint. The task does the same work with nothing added and runs in seconds.

**Cost, stated plainly:** it matches text, not an AST, so it cannot understand scope. Every rule in it
is one where a textual match is unambiguous, and it strips comments before matching. A rule needing
real scope analysis would justify revisiting detekt.

**Verified:** injected five violations across four rules; all five were reported with file, line, rule
id and authority, and the build failed. Restored, and `check` is green.

## D24 — Android Lint is disabled, and that is a real gap

**Decided:** All `lint*` tasks are disabled.

**Why:** AGP 8.5.2's bundled Lint cannot run on JDK 25 — it dies parsing the version string, the same
family of problem as the Kotlin 1.9 daemon in D22. Every `lintAnalyze` task fails, which takes `check`
down with it. `ignoreTestSources` was not enough; it fails on production sources too.

**Cost:** we lose Lint's accessibility checks, API-level checks, and resource validation. The
conformance checker covers our own design rules but is **no substitute** for those.

**The fix is an AGP upgrade**, deferred rather than attempted: JDK 25 has already cost two toolchain
casualties this session and a third upgrade under time pressure risked leaving the build broken. This
is the highest-value remaining piece of toolchain work.

## D22 — Kotlin 2.2.21, not a second JDK

**Decided:** Upgrade Kotlin 1.9.25 → 2.2.21 and adopt the `org.jetbrains.kotlin.plugin.compose`
plugin. Keep JDK 25 as the only JDK on the machine.

**Why:** Kotlin 1.9.25's compile daemon cannot start on JDK 25, so **every** compile was silently
falling back to non-incremental. Builds had grown to 5m 18s by step 4, and that was the toolchain, not
the code. Gradle 9.3 itself embeds Kotlin 2.2.21, which is the evidence that version runs on JDK 25.

Installing JDK 17 or 21 would also have worked, but it means managing two JDKs and consuming more
disk. Upgrading Kotlin needs no new JDK and is the right answer regardless — K2 is a faster compiler,
which directly serves the reference machine.

**Measured result:** incremental build **5m 18s → 32s**, and daemon fallbacks 4 → 0.

**Cost:** From Kotlin 2.0 the Compose compiler is a separate plugin, so
`composeOptions.kotlinCompilerExtensionVersion` is gone from both modules. AGP 8.5.2 paired with
Kotlin 2.2.21 is not an officially tested combination, though it builds clean here. Reclaimed 187 MB
by deleting the now-unused Kotlin 1.9.x artifacts from the Gradle cache.

## D16 — Validate the token set itself, not just its use

**Decided:** A JVM test suite asserts properties of the tokens: divisibility by 4, adjacent scale ratios
≥ 1.3, radius ceiling, whole frame counts, contrast minimums *and* a contrast maximum for the dot
texture, no duplicate values within a category, and a mandatory `job` plus `why` field on every token.

**Why:** Every other enforcement layer assumes the token set is correct and will faithfully propagate a
bad value everywhere. This is level 6 on the guarantee ladder in `design-canon/rationale.md` §9 and it
is the level that was missing. Computing the contrast numbers rather than asserting them already found
two real defects — see `rationale.md` §8.

**Cost:** One test file. Runs on the JVM in seconds, no emulator.

**Consequence:** You cannot add a token without writing down why it exists. A duplicate value is a
build failure.

## D25 — Product flavours, not separate projects, for per-game apps

**Decided:** One source tree, one `:app` module, one Gradle `flavorDimension`. A `studio` flavour ships
the multi-game library; each game additionally gets a standalone flavour with its own `applicationId`
and store listing. Game code lives in a `src/game<Name>/java` directory that every flavour shipping that
game adds to its `srcDirs`. `GameRegistry` is the only file the flavours disagree about.

**Why:** Two independent reasons, and the second is the stronger one.

*Discoverability.* Store search runs on specific terms. A "sudoku" listing can rank for sudoku; a
"puzzle studio" listing cannot rank for anything specific. Installs are also smaller, and one game's
reviews cannot drag the others down.

*Licensing.* The vendored QQWing generator is GPL-3.0, and that obligation attaches to whatever artifact
ships it. In a single combined app a future `nonogram(10)` — containing no GPL code — would inherit the
obligation for nothing. Scoping the engine to a source set that only Sudoku-bearing flavours compile
keeps the obligation on the artifacts that actually earn it.

**Rejected alternative: one project per game.** It gets the same store benefits and costs far more. A
token change becomes N releases across N repositories, and `:design-system` has to be published as a
versioned artifact or copied into each one. Copying it is precisely what `design-canon/` exists to
prevent — the whole harness is built on there being one place a value is authored.

**Cost:** Unit tests now run once per variant, so the suite executes four times rather than once. Adding
a game means one `srcDirs` line per flavour that ships it, and the absence of that line is what keeps a
flavour clean. Two `GameRegistry` files must be kept in step, which `M5-roadmap-agreement` checks.

**Consequence:** `GameRegistry.standalone` tells the shell which build it is in. Both flavours open on
Home; what differs is what Home *is* — a library of cards, or that game's start page with CONTINUE/PLAY.
`R4-gpl-isolation` fails the build if vendored GPL code reappears in the shared source set.

**Amended once.** The first version had a standalone build launch straight into the puzzle, reasoning that
a one-item library is a weak front door. It is — but skipping the front door entirely costs more than it
saves: there is nowhere to distinguish CONTINUE from PLAY, and no way to reach stats or settings without
first navigating somewhere you did not ask to go. Home as a start page gets both.

**Resolved by D27:** the `applicationId` was `com.example.lightapp`, which Play rejects. It is now
`com.noadsstudio` (studio) / `com.noadsstudio.sudoku` (standalone). Still permanent once published — match
a domain you own first if you have one.

## D26 — What "complete" means, and where it stops

**Decided:** `sudoku(9)` ships with a start page, per-digit remaining counts on the pad, a statistics
page, and four settings (theme, haptics, remaining counts, timer). **No tutorial, no onboarding, no
how-to-play page, no achievements, no daily challenge.**

**Why those four systems:** each closes a loop that was already open rather than adding a new surface.

- *Start page* — the app had no front door. There was nowhere to say CONTINUE rather than PLAY, which is
  the single clearest signal that a game remembers you.
- *Remaining counts* — the pad already computed `exhaustedDigits`, which is the same information with the
  useful part discarded. "Two left" beats "not yet gone", from the same pass over the grid.
- *Statistics* — `GameDefinition.onGameComplete` had been a documented sink since the contract was
  written. A callback nothing reads can stop firing unnoticed, and it had. Stats give it a consumer, so
  the callback and the record now prove each other.
- *Settings* — remaining counts and the timer are the two things players genuinely disagree about.

**Why no tutorial:** anyone opening a Sudoku app knows Sudoku. A rules page would be a screen almost
nobody reads, and the design language has no good answer for a wall of instructional prose — three type
sizes and no illustrations is a bad fit for teaching. The interface teaches instead: a given rejects
input, a conflict outlines itself, the remaining count shows what is left.

**Rejected, deliberately:**

| Candidate | Why not |
|---|---|
| Achievements / badges | Needs iconography and celebratory art. `avoid.md` bars the assets, and a trophy case is the opposite of an instrument panel. |
| Daily challenge | Needs a shared seed schedule, which is a network feature wearing a disguise. |
| Hints / auto-solve | The app deliberately never holds the solution (see `Conflicts`). A hint would require it, and with it a code path that could leak the answer. |
| Highlight peers of the selected cell | Genuinely good, and the only one likely to be added. It needs a new `CellState` and a component-spec change, so it is a considered addition rather than a free one. |
| Streaks | Needs a calendar and a definition of "day" that survives time-zone changes. Cost is all in the edge cases. |

**Cost:** One new screen, one new destination, a stats codec, and two settings. Stats add ~20 tests.

**Consequence:** The stats page is generic over `GameRegistry`, so a second game appears on it with no
change. Its per-value helper is named `StatLine`, **not** `Readout` — `D13-three-readouts` counts
`Readout("` per file to hold a *game* screen to three, and a record page legitimately has more numbers
than that.

**Amended 2026-09-03 — the "no how-to-play" half is reversed.** The completeness pass recorded in
`sudoku-market-study.md` reframed the refusal: a *tutorial* nobody reads and a *reference* page that
points at signals the board already speaks are different things. The page ships as a quiet full-height
destination (`Screen.Rules`) reached from a words-only `HOW TO PLAY` link on Home — under the fixed
PLAY button when one exists, otherwise as the last item of the list. It honours the original
objections: six groups of one-line sentences (no prose, no wall of text), no display element, no red,
no new component — nothing but `Label` captions and body lines. It says nothing the player cannot
verify in one tap. Spec: component-specs §How to play.

The rest of D26 stands: no onboarding tour, no achievements, and the DAILY row below is still refused
in its **shared-schedule** form — that refusal is about the network feature, and nothing here changes
it. The deterministic *local* variant of that candidate (date-seeded, no network, no streaks, no
time-zone calendar) was implemented on 2026-09-03 as **decision D31**.

## D27 — The product is "No Ads Studio"

**Decided:** The studio app is named **No Ads Studio**. The studio flavour's launcher label is
`No Ads Studio`; each standalone flavour keeps the game's own name (`sudoku(9)`). The base
`applicationId` is `com.noadsstudio`, so the studio ships as `com.noadsstudio` and the standalone Sudoku
as `com.noadsstudio.sudoku`.

**Why the name:** the no-ads, no-tracking, no-network promise is the product's whole distinction, so the
name states it. It also reads honestly next to the About line already on the settings screen —
`offline · no ads · no tracking`.

**Scope, deliberately narrow.** Only two things are user-facing and both changed: the launcher label and
the home-screen header (`NO ADS` / `STUDIO`). Everything named "Nothing" internally — `NothingTheme`,
`NothingGamesApp`, the design canon — is untouched. That is the *design language*, correctly named after
the brand that inspired it; it is not the product name and renaming it would be churn with no user benefit.

**applicationId vs namespace — they are different things.** `applicationId` is the store identity Play
enforces and it is now real. The `namespace` (the `com.example.lightapp` code package) is a compile-time
concern Play never sees; it is left as-is because renaming it moves every package directory and the
hardcoded paths in the token generator and conformance checker — a real refactor with no release value.
A released app with `applicationId` ≠ `namespace` is normal and common.

**Permanent once published.** The `applicationId` is fully reversible until the first Play upload and never
after — Play keys an app to that string for life. If a specific domain is owned, match it before
publishing; `com.noadsstudio` assumes only the studio name.

**Consequence:** the `com.example.lightapp` release blocker recorded in D25 and in `AGENTS.md` is resolved
for store identity. The namespace alignment is now the only cosmetic id left, and it is optional.

## D28 — A local emulator for device checks, not repeated manual APK installs

**Decided:** `scripts/emu-*.sh` in `projects/Sudoku/` manage a single lightweight AVD named
`nas-fast` for on-machine device verification, replacing "build an APK, install it somewhere,
look at it" with one command.

```bash
projects/Sudoku/scripts/emu-start.sh              # boot it (~45-75s cold, seconds after)
projects/Sudoku/scripts/run-on-emu.sh sudoku      # build, install, launch, screenshot
projects/Sudoku/scripts/emu-screenshot.sh          # screenshot on demand
projects/Sudoku/scripts/emu-stop.sh                # stop it (saves a snapshot)
```

**Image: `google_apis` API 34 x86_64, not `aosp_atd`.** The `android-35;aosp_atd;x86_64` image
was tried first — Google's own minimal build for automated test execution, and it is
genuinely lighter and boots faster. It was rejected because `screencap` returned a
byte-identical black PNG for every capture on this host, regardless of which app or even
whether any app was installed at all. The app itself was never broken — HWUI logged
"Displayed", nothing crashed — the capture path specifically was dead on that image. Two
host-specific fixes were needed on top of switching images, both found by elimination:

1. **`-gpu swiftshader_indirect`, not `host`.** Host-GPU passthrough on this machine's Intel
   UHD 617 composited correctly but the framebuffer `screencap` reads from came back empty.
   Software rendering is slower per frame; the capture is trustworthy, which is the property
   that matters for a device meant for visual checks.
2. **No `-no-window`.** Headless mode never drove real content into the capturable surface on
   this host at all, confirmed against the bare system UI before host-GPU was even a
   suspect. A window is required for a real framebuffer to exist, independent of whether
   anything displays it.

Package verification is disabled on boot (`package_verifier_enable=0`) because this device
has no real route to Google's verification service, and `adb install` otherwise times out
with `INSTALL_FAILED_VERIFICATION_FAILURE` rather than failing fast.

**Local-only release signing** (`app/keystore/local-verify.jks`, wired in `app/build.gradle`
behind a file-existence check) exists so `assembleRelease`/`bundleRelease` — which fail
outright with no signing config — can be exercised and installed for a real device check
before a production Play key exists. The certificate's own DN says "Local Verification
Only". It is gitignored (`*.jks` was already excluded) and must never be the key an app is
actually published with — Play permanently binds a listing to its first signing key.

**Cost:** ~500MB for the system image, disk-cheap. First boot after any AVD config change is
a full cold boot; every boot after that resumes from a snapshot in seconds.

**Consequence:** a device is now available for every future game without repeating this
diagnosis. `game-scaffold.md`'s "before you say it works" step gains a device check that
does not require a physical phone.

## D29 — Bottom-tab navigation, and state shown rather than implied

**Decided:** Three changes, all of them replacing an implication with a statement.

1. **The remaining count is a badge on the number key's upper-right**, not a numeral beneath it.
2. **A persistent `NothingBottomNav`** over three tabs — `PLAY`/`GAMES`, `STATS`, `SETTINGS` — replacing
   the pair of buttons that used to sit at the bottom of Home. Absent on a game screen.
3. **A resume card** replacing the lone `CONTINUE`/`PLAY` button, showing difficulty, progress and elapsed
   time for the saved game.

**Why the badge.** Beneath the key, the count read as a caption — a second row of numbers competing with
the digits above them, and 20% more vertical space on the tightest part of the screen. On the upper-right it
reads as an annotation *of* the key. It also needs no offset to get there: the circle is inscribed in its
`space48` box, so a `space16` badge in the corner already lands on the arc while staying inside the key's
bounds. Overhanging would have eaten the `space8` gap to the next key.

**Why bottom tabs.** Stats and Settings were reachable only through two buttons at the end of Home's scroll,
which made them feel like footnotes to the library rather than places. Three destinations is exactly the band
where a tab bar is right — below that it is overkill, above it needs grouping. The bar is words, not icons
(rule T9), which also suits a project with no icon set.

It is **hidden on the game screen**: a bar there would steal height from a board that is already as tall as
it is wide, and would put "leave the game" one thumb-width from the number pad.

**Why a resume card.** `CONTINUE` versus `PLAY` told you a save existed and nothing else — not which puzzle,
not how far in, not whether it was the quick one nearly finished or the hard one abandoned. Those decide
whether you tap it, and every one of them was already in the save. Three metrics, matching decision D13's
reasoning: a curated view, not a state dump.

**Consequence for layering:** the shell needed a game-agnostic `ResumeState`, mapped in `MainActivity` from
the Sudoku session. That exposed a real constraint — `MainActivity` is in the shared source set and
genuinely cannot see `SudokuDefinition.ID`, which lives in a flavour-scoped one — so the game id comes from
`SessionStore.SUDOKU_GAME_ID`. That constant is the visible edge of the per-game-session wart already
documented on that class, and it goes away with the same refactor.

**Consequence for insets:** the bar consumes `navigationBars` itself, so the three tab screens inset top and
sides only. Padding the bottom in both places leaves a dead band. A game screen keeps full `safeDrawing`,
since no bar sits under it.

**Also folded in:** `formatDuration` moved to `studio/TimeFormat.kt`. There had been three private copies
and the resume card would have been a fourth — and two of them silently wrapped past 59:59, so a long
session displayed a wrong-but-plausible time.

## D30 — Selection highlighting, and the brand mark in both builds

**Decided:** Two changes.

1. **The brand mark states the full product name in both flavours.** A standalone build's header reads
   `NO ADS STUDIO` over the game's own name; the library build still spells it across the two lines,
   `NO ADS` over the display word `STUDIO`. Previously the standalone build said only `NO ADS` and never
   named the studio at all.
2. **Selecting a cell highlights the cells it implicates**, and what that means adapts to the tap.

**Why highlighting adapts.** The two standard Sudoku behaviours answer different questions, and each is
useless in the other's case:

| Tapped | Highlights | Asks |
|---|---|---|
| a filled cell | every other cell with the same digit | *where else is this digit?* |
| an empty cell | its twenty row/column/box peers | *what can go here?* |

Same-digit highlighting alone does nothing on an empty cell — the common case while solving, and precisely
when a player wants help. Peer highlighting alone wastes the more useful answer when a digit is under the
finger. Choosing per tap means one rule always says something.

This is the feature named in decision D26 as "the only one likely to be added", and it is the cheapest
possible form of exposing the mechanism (`nothing-study.md` §1): the constraint was always in the rules, and
this only draws it. No hint, no solver, no knowledge of the answer.

**Cost, and the constraint it hit.** Related and selected cells share the `surfaceRaised` fill, because this
palette has exactly three surface levels and there is no step above `surfaceRaised` to give selection a fill
of its own. Adding one would have meant a new colour token, and a fourth surface level would dilute what
elevation means everywhere else. So selection moved to the **outline** — `borderVisible`, yielding to
`accentRed` when the cell is in conflict. That ordering is the rule that matters: a cosmetic border must
never displace the screen's one urgent signal.

**Consequence:** `GridCell` gains a `related` flag. Seven tests cover the branch logic and its boundaries —
nothing selected, a digit with no partners, and givens matching player entries (they do; the player is
looking for fives, not for who placed them).

**Amended 2026-09-03 — compose, don't split** (market study §5A). The per-tap split answered one question
per tap. The market leader answers both: every tap lights the selection's row/column/box (the **cross**,
the soft layer: `surfaceRaised` fill only) and a filled tap additionally lights the sibling digit set
(the **framed** layer: fill plus its hairline or TE accent). `GridCell`'s single `related` flag became
`cross` and `sibling`; the cross is lit on empty and filled taps alike, so one rule covers every tap.
The board never picks which question to answer — it answers the one that applies and adds the other
when there is a digit to add.

## D31 — The daily puzzle: one deterministic board per UTC day, in its own slot

**Decided:** The day's puzzle ships as the sixth picker entry — `DAILY`, listed first in the
in-game `NEW` picker ahead of the five difficulties (D18 as amended). One board per **UTC day**:
the board is generated from a QQWing run seeded with the UTC date, so the same puzzle appears on
every device on the same day, with no network anywhere in the path.

- **It is a mode, not a difficulty.** Generated at `MODERATE` (the engine level that lands
  first-try, keeping the deterministic run fast) but carried under its own label, `Daily`. The
  finish panel reads `SOLVED · DAILY`, and `STATS` keeps its own `DAILY` row — the five real
  difficulties' records are never diluted by day puzzles.
- **It has its own session slot.** The daily session persists under `daily.sudoku9.*` keys —
  deliberately outside the `session.` prefix the Home resume cards scan, because a daily is
  re-entered through `DAILY`, never through a resume card. Starting, playing or finishing it
  never touches an in-progress regular session, and finishing a regular game never touches it.
- **It resumes, within its day.** Picking `DAILY` when today's puzzle is already in progress
  resumes it where it was left. A stored session from an earlier day is leftovers, not
  progress — it is discarded and the seeded generator rebuilds that earlier day's board
  identically if it is ever asked again.
- **The day is UTC, deliberately.** D26's local-daily amendment says no time-zone calendar: a
  device-local midnight is the same instant on different days in two time zones, and DST moves
  when the day flips. UTC has neither problem — the boundary never moves.
- **Determinism is an engine change, tested.** QQWing's random source moved out of the shared
  companion statics onto each board instance (`setRandom`), and a seeded controller run uses a
  single worker so the one stream is never interleaved by other threads. `QQWingSeedTest` locks
  same-seed → same puzzle, different-seed → different.

**Why:** This is the market leader's daily puzzle with every part that needed a server or a
calendar stripped out — no shared seed schedule, no streaks, no leaderboard, no push. It closes
the one loop ("come back tomorrow") that a pure offline puzzle cannot close on its own, at the
cost of one deterministic generator path.

**Cost:** A seeded single-worker generation path (parallelism given up; `MODERATE` lands on the
first try, so nothing measurable). A second persistence slot and its keys. The pre-game
difficulty chooser on the shell start page does not list `DAILY` — that chooser predates D18's
in-game picker and does not thread any selection; it is a separate wart, not part of this
feature.

## D32 — The daily puzzle, generalised: every game gets the day's board

**Decided:** D31's daily puzzle extends from sudoku to **all five games**. Every game's `NEW`
picker leads with `DAILY` (the shell's `GameDefinition.dailyLabel` is now non-null for every
game), every generator runs seeded with the UTC day number, and every game stores its daily in
`daily.<gameId>.*` keys — outside the `session.` prefix the resume cards scan, exactly as D31
laid down for sudoku.

- **One UTC day, one board, no network.** The four new generators were designed with a
  `seed: Long?` parameter from the start, so generalisation was wiring, not new engines. The
  seed is the epoch-day count; the same board appears on every device until midnight UTC.
- **Minesweeper's twist: the seed is [day, first tap].** First-tap safety (that game's defining
  rule) means mines cannot be laid before the first reveal, so no board can be deterministic
  from the day alone. The honest limit of determinism for that game: the same day *and the same
  opening* always meet the same field. The derivation `day * CELLS + firstTap` is asserted in
  `MinesweeperDailyTest` so it cannot drift silently.
- **State carries `dailyDay`.** Each game's state gained a `dailyDay: Long?` that rides on the
  state but is never persisted inside the session — persist and finish route to the daily slot
  exactly when it is non-null, so a daily and an in-progress regular puzzle coexist untouched.
- **Restore accepts the `Daily` label.** Each game's restore step validates difficulty against
  a `KNOWN_LABELS` list that now includes `Daily`; an unknown label is still rejected. Sudoku's
  D31 pattern held: the daily is a mode carried under its own label, and `STATS` records it
  under `DAILY` without diluting the real difficulties.
- **Picker parity in-game and on the shell.** The shell's pre-game chooser renders
  `listOfNotNull(dailyLabel) + difficulties` for every game, and each game's own in-game
  `NEW` picker now matches it.

**Why:** D31's argument — "come back tomorrow" is the one loop an offline puzzle cannot close
on its own — applies to every game in the studio equally. The cost was low because the
architecture had already absorbed the decisions once; the four games needed slots, labels, and
routing, not ideas.

**Cost:** Four daily persistence slots (each mirroring its game's regular slot, sharing one
`dailyKey` helper). One nullable `dailyDay` per state. Restore validation widened by one label
each. No engine changes anywhere — the seeded paths existed and were already tested.

## D33 — The Block Puzzle ruling: S13 amended for direct manipulation, pieces by dot density

**Decided:** The roadmap's open question (§2, "The Block Puzzle question") is ruled on. Block
Puzzle — the fastest-growing genre in the market data, +176.1% — is **admitted as Tier 3,
status `planned`**, under three rulings:

- **Direct manipulation is input, not animation.** S13 ("never animate position") is amended
  with one clarification, not an exception: a view's position may **follow a pointer during an
  active drag** — the finger is the animation, and nothing the app tweens. What S13 still
  forbids is exactly what it always forbade: the *app* moving anything after input ends. The
  release resolves by opacity only — the piece appears at its landing cells by a fade, never
  by travelling there, no settle bounce, no slide-to-slot. Nonogram's shipped drag-to-paint
  already followed this shape; D33 writes down what made it legal.
- **Piece distinction without colour: dot density.** The language already renders information
  as dot pitch (G1–G3, the Doto typeface itself). Queued pieces are distinguished by fill
  density — solid, half-pitch dots, hollow outline — which is a data-bearing distinction, not
  decoration, and survives grayscale by construction. No three-colour trick is available or
  wanted.
- **"Puzzle validity" reinterpreted for an endless game.** Block Puzzle is a placement game
  with a score, not a generated puzzle with a solution — the admission criterion's literal
  reading ("solvable by deduction from the start state") does not apply. The honest analogue,
  and the generator's contract: **every dealt piece set admits at least one legal placement at
  deal time**, so a run can only end by accumulated placement, never by an instantly-dead
  deal. Recorded here so the generator tests assert the analogue, not the original.

**Why:** The market signal is the strongest in the data, the aesthetic fit is genuine (a grid,
fill states, no assets), and the one motion question deferred since the roadmap was written now
has a shipped precedent to rule from. Refusing it on a technicality of S13's wording while
running drag-to-paint in production would make the rule dishonest; amending it with the actual
distinction — input echo versus app-driven motion — keeps the rule true.

**Cost:** One clarification sentence in `rules.md` S13. A generator with a placeable-deal
contract and its tests. The game itself is unbuilt — Tier 3, behind nothing; it may be the next
work item or wait. The admission criteria's other rows (offline, zero permissions, no assets,
2–10 min sessions) pass by inspection the same way Nonogram's did.

## Build order

Strictly sequential. Each step leaves the project compiling.

0. **Prerequisite, manual:** place `geist_sans`, `geist_mono`, and variable `doto` in
   `design-system/src/main/res/font/`. Remove `ndot55`, `ndot57`, `ntype82`, `ntype82mono`. Nothing in
   steps 2 onward can compile without this. Set `minSdk 26` per D19.
1. `tokens.json` generator task + `TokenValidationTest` (D16) + `:design-system` module skeleton.
   Validation must exist before any token is consumed.
2. Theme: `Spacing`/`Radius`/`Duration` value classes, colours, type (Geist/Doto, em tracking, tabular
   figures), shape, motion
3. Components: `Label`, `NothingButton`, `NothingCard`, `NothingTopBar`, `GameIcon`, `DotMatrixReadout`,
   `GridCell`, `NumberPad`, `DifficultyPicker`
4. Studio shell: hand-rolled nav, home screen from `GameRegistry`, settings screen
5. Persistence: DataStore, the D7 encoding, `settings.` prefix keys
6. `sudoku(9)`: ViewModel, QQWing off the main thread, grid UI, conflict logic (D20), undo (D17),
   difficulty picker (D18), haptics
7. Delete `NothingSudokuScreen` from `MainActivity.kt`
8. Debug composition guards, detekt forbidden-type rule, Konsist architecture test — all bound to `check`
9. `conformanceCheck` Gradle task: harness structure, denylist, anti-requirements, doc/code agreement
10. Paparazzi component spec sheet + goldens, then the four review tests in `nothing-study.md` §12

## D32 — The harness hardens itself: four gates the flavour split left open

**2026-09-06.** Building a second game made the goal explicit — many small, offline, monochrome games,
each individually installable — and auditing the enforcement layer against that goal found four gaps a
single-game project could never expose. All four are now gates in `conformance.gradle` (36 automated
rules, up from 32), each verified the house way: inject the violation, watch the gate fail, restore.

1. **Manifests are scanned everywhere, not just `src/main`** (`R4-zero-permissions` extended). A
   flavour-scoped manifest — the documented way to declare flavour components — was exactly the file the
   permission scan never read. A per-flavour `INTERNET` would have shipped while the gate stayed green.
2. **`allowBackup="false"` is enforced** (`R4-allow-backup`, rules.md X17). The attribute was declared
   and never checked; it defaults to **true** when absent, and device transfer can lift the private
   DataStore — saved sessions and stats — off the player's device. With "no tracker, nothing leaves the
   device" as a product promise, this closes the one manifest-level export path.
3. **The admission chain is checked per flavour** (`M5-flavour-agreement`, rules.md M12), and the
   src/main purity rule X16 is automated (`G2-pure-main`) instead of review. For each
   `app/src/game<Name>`: some flavour must attach it via `srcDirs`, and that flavour's registry must
   register `<Name>Definition()`. A game dir no flavour attaches is unreachable licence weight; a
   srcDirs line without a registration ships a game that skipped the roadmap admission criteria. This is
   the gate that makes the "separate games" goal safe to grow into: a new game cannot reach a build
   except through the chain.
4. **A shipped flavour owns its id** (`M5-standalone-identity`, rules.md M13). Any flavour source set
   holding a GameRegistry is a flavour that means to ship, and every shipped flavour needs its own
   `applicationIdSuffix` or it installs over the studio app on the device.

Two Groovy traps are recorded in the checker's comments because the second one cost a debug cycle: a
GString never equals a String in `Set.contains`, and `$` without `(?m)` anchors at the end of the whole
text, not each line — both silently produced false passes before the fix.

## D34 — Undo in an endless game, and a tooling tier list

**2026-09-07.** Two small rulings made while closing blockpuzzle's parity gap.

**Undo (parity G4) in blockpuzzle.** The genre's aid is the same as sudoku's, with one rule this
genre must not bend: *a terminal position is never recorded*. Sudoku's undo can never cross a win
because nothing follows a win; blockpuzzle's could have crossed its game-over, since `over` is just
a flag on the next state. The record-before-commit shape made the bug tempting: record, then finish.
Resurrecting a finished run would farm stats the run already emitted. So [BlockHistory] refuses
terminal snapshots by contract, is bounded at 50 (D17, shared with sudoku), and clears on a new run
— undo never reaches across runs. Snapshots, not deltas: a placement commits land + clear + score
+ refill atomically, and reverting piecemeal would re-derive the clear logic for nothing.

**The grep ceiling, and what replaces it.** The repo's discipline is grep-shaped — `conformance.gradle`
reads source text. That is the right first line, but three gates cannot be expressed as greps:
recomposition behaviour, rendered output, and launch time. The vetted replacement set lives in
`ideas/tooling-research.md` — Vkompose (Apache-2.0) compiles against skippability rules, Roborazzi
(Apache-2.0) screenshots the rendered screen on the JVM, Macrobenchmark (AndroidX) times the launch
against the 1-second budget. All build/test-time only; nothing lands in the artifact, so the
zero-permission product promise is untouched. Adoption is tiered and deferred until a gate actually
fails for the first time — a tool adopted before it has a failure to catch is decoration.
