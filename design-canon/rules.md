---
status: canonical
---

# Rules — complete index

Every rule in the system, flat and numbered, with the document that owns it.

**This document states no values.** No hex codes, no dp figures, no durations. Values live only in
`tokens.json`, so this index cannot drift out of agreement with them. If you need a number, follow the
authority column.

---

## How to read the enforcement column

| | Meaning | What it asks of you |
|---|---|---|
| **A** | **Automated.** `./gradlew check` fails on a violation, naming the file, line, rule id and authority. | Nothing. Run the build. |
| **S** | **Structural.** The code cannot express the violation — a type, visibility boundary, or component signature makes it unavailable. | Nothing, unless you weaken the structure. |
| **R** | **Review.** Nothing will catch this. If you do not check it deliberately, it ships broken. | **All of it.** |

**The `R` rules are the entire job.** Everything marked `A` or `S` is already handled; scanning those is
wasted effort. Read the `R` list before you start and again before you finish.

> **This column was wrong until recently, and the correction matters.** Twenty-one rules were marked `A`
> while nothing enforced them — sharp corners, per-screen type-size limits, dot-grid placement, one game
> in progress, roadmap agreement, the suppression cap. An honest `R` is a rule you will remember to check.
> A false `A` is a rule everyone assumes the build is handling, which is strictly worse than no marker at
> all. If you add a rule here, marking it `A` obliges you to add the gate in the same change.

Count today: **36 automated conformance rules** (`projects/Sudoku/conformance.gradle`) plus **16 token
property tests** and **3 font coverage tests** (`:design-system:test`). Every automated rule in this index
maps to one of those and has been verified to fail when the rule is broken, rather than assumed to work.

---

## Hard prohibitions

| # | Rule | | Authority |
|---|---|---|---|
| P1 | No Android permission, in any module | A | `ideas/avoid.md` · R4 |
| P2 | No networking, advertising, analytics, or crash-reporting dependency | A | Dependency_Denylist · R4 |
| P3 | No raster image or audio file under `res/` or `assets/` | A | R4 |
| P4 | No font that is not SIL OFL or equivalent. Never ship NDot or NType 82. | A | `resource-map.md` · R9 |
| P5 | No gradient, no blur, no shadow elevation above zero | A | R8 |
| P6 | No sharp corners on a visible surface | A | R10 |
| P7 | No spring, bounce, or overshoot easing | A | R13 |
| P8 | No divider or rule line between element groups | A | R11 |
| P9 | No React Native, Flutter, or WebView wrapper | R | `ideas/avoid.md` |
| P10 | No Room, no KSP, no annotation processor | A | D7 · Dependency_Denylist |
| P11 | No navigation library | A | D8 · Dependency_Denylist |
| P12 | No debug build shipped as a release | R | `ideas/avoid.md` |
| P13 | No design value authored outside `tokens.json` | A | R2 |
| P14 | No bare `MaterialTheme` — only the theme wrapper | A | R8 |
| P15 | No colour resource entry outside the design system's single XML | R | R15 |
| P16 | No non-null assertion (`!!`) outside vendored code | A | R16 |
| P17 | No raw `Thread`, `GlobalScope`, or `runBlocking` outside vendored code | A | R16 |
| P18 | No platform haptics call — only the gated channel | A | D15 |

## Colour and surface

| # | Rule | | Authority |
|---|---|---|---|
| C1 | Elevation is expressed by surface colour alone | A | R8 |
| C2 | Exactly four text levels: display, primary, secondary, disabled | S | R9 |
| C3 | Display, primary, secondary clear 4.5:1 on every surface | A | R8 · R18 |
| C4 | Disabled clears 3:1 on every surface | A | R8 · R18 |
| C5 | The dot-texture border colour must stay *below* 2:1 — the one contrast maximum | A | R18 |
| C6 | Dark mode is the fallback when the system preference is unreadable | R | R8 |
| C7 | Red is normally **zero** per screen; a screen with nothing wrong has none | R | D2 · R14 |
| C8 | Red at most one element when something is wrong | R | R14 |
| C9 | Red only for error, destructive, or urgent states | R | R14 |
| C10 | Red is assigned to no type style — outline and fill roles only | A | R14 · R18 |
| C11 | Every error state carries a non-colour indicator, verifiable in grayscale | R | R14 |
| C12 | Priority when several urgent states collide: destructive, then error, then urgent | R | R14 |

## Typography

| # | Rule | | Authority |
|---|---|---|---|
| T1 | Three font roles: display, body/UI, data/labels | A | Font_Role_System |
| T2 | The dot-matrix face only at or above its floor size | A | R9 · R18 |
| T3 | Labels are mono, all-caps, tracked, at the label size | S | R9 |
| T4 | Tracking is expressed in **em**, never sp | A | R9 |
| T5 | Tabular figures on anything that counts in place | A | R9 · R18 |
| T6 | At most three type sizes per screen | **R** | R9 |
| T7 | At most two font weights per screen | **R** | R9 |
| T8 | At most one display-level element per screen | R | R9 |
| T9 | Words over icons wherever a word fits | R | `nothing-study.md` |

T3 is structural because `Label` upper-cases internally and takes no size — a lowercase or resized label
cannot be written. T8 has a debug-only composition guard, but it validates only the composables that
recomposed in the same pass as the screen wrapper, so treat it as review.

## Shape, spacing, motion

| # | Rule | | Authority |
|---|---|---|---|
| S1 | Buttons are pills — radius equals half the height | S | R10 |
| S2 | Card radius never exceeds the card token | A | R10 · R18 |
| S3 | Grid cells use the cell radius, held near 21% of the cell side | R | `rationale.md` §4 |
| S4 | Every interactive element has a 48dp minimum touch target | **R** | R10 |
| S5 | Icons are monoline, no fill, inheriting the current text colour | **R** | R10 |
| S6 | Every spacing value is a token, divisible by four | A | R11 · R18 |
| S7 | The optical token is for optical offsets and dot diameter only, never layout | **R** | R11 |
| S8 | Adjacent element groups separated by the group-separation token or larger | R | R11 |
| S9 | Dimensions that scale with available space are computed, never literal | A | R11 |
| S10 | Two motion durations only: micro and transition | A | R13 · R18 |
| S11 | Durations are whole frame counts at 60fps | A | R18 |
| S12 | One easing curve, deceleration-dominant | A | R13 · R18 |
| S13 | Animate opacity and colour only — never position, size, scale, rotation. Amended D33: a view may follow a pointer during an active drag — direct manipulation is input, not animation; after input ends, opacity/colour only | A | R13 |
| S14 | Press feedback changes border or text brightness, never scale or hue | A | R13 |
| S15 | Reduced-motion setting applies end states with no intermediate frames | **R** | R13 |
| S16 | At most eight elements animating at once | R | R13 |
| S17 | Every screen applies window insets and can scroll | A | R16 |

S1 is structural because `NothingButton` fixes its own height and shape; a caller cannot pass either.
S6 and S9 are enforced by the literal ban plus the value-class boundary — a raw `Dp` will not compile in a
consumer module, so the only reachable values are tokens. S17 is new: under `targetSdk 35` a screen without
insets sits under the system bars, and a board as tall as it is wide clips its own controls without a
scroll. Both shipped that way once.

## The dot matrix

| # | Rule | | Authority |
|---|---|---|---|
| G1 | **Dots always carry information. Never decorative.** | A · S | D12 · `nothing-study.md` §3 |
| G2 | Canvas-drawn, no image asset | A | R12 |
| G3 | Pitch must be a spacing token | A | R12 |
| G4 | Background layer only — never a border or button style | **R** | R12 |
| G5 | Dot positions cached; recomputed only when pixel bounds change | R | R12 |
| G6 | Consumes no pointer event, exposes no accessibility node | **R** | R12 |
| G7 | Never hidden, thinned, or faded for performance | R | R12 |

G1 is doubly held: `DotMatrixReadout` accepts `progress: Float` and nothing else, so unbound dots are not
expressible, and the checker additionally rejects a constant literal passed as that argument. This is the
rule most likely to be broken by accident, because a dot grid is attractive and cheap — hence two layers.

## Tokens

| # | Rule | | Authority |
|---|---|---|---|
| K1 | `tokens.json` is the only place a value is authored | A | R2 |
| K2 | Generated token file is never committed, never hand-edited | **R** | R2 |
| K3 | Generation runs before any compilation that consumes a token | S | R2 |
| K4 | Every token carries a non-empty `job` and `why` | A | R2 · R18 |
| K5 | **No token may exist without a job no other token does** | A | R18 |
| K6 | No two tokens in a category share a value | A | R18 |
| K7 | Malformed source fails the build and leaves no stale output | A | R2 |
| K8 | Value-stating canon documents name only real tokens | A | R17 |

K3 is structural: the generate task is a Gradle input to compilation, so the ordering is not something a
build can get wrong. K8 is new — `nothing-tokens.md` had accumulated a success green, a warning amber and
an interactive blue, none of which existed in `tokens.json` and all of which contradicted the palette rule.

## Structure

| # | Rule | | Authority |
|---|---|---|---|
| X1 | Four harness directories: `ideas`, `design-canon`, `projects`, `specs` | A | R1 |
| X2 | Root markdown limited to `README.md`, `AGENTS.md`, `CLAUDE.md` | A | R1 |
| X3 | Agent adapters are symlinks, never copies | A | R17 |
| X4 | Spec documents are plain markdown at a vendor-neutral path | **R** | R17 |
| X5 | Every canon and ideas document carries a status marker | A | R5 |
| X6 | Design system owns every design value; consumers own none | A | R15 |
| X7 | Dependency direction is one-way, consumer to design system | S | R15 |
| X8 | Values cross the module boundary only as value classes | S | R15 |
| X9 | Forbidden Compose types fail the build via the `check` gate | A | R15 |
| X10 | `namespace` only — no `package` attribute in the manifest | A | R4 |
| X11 | Suppressions are single-declaration, reasoned, and capped at ten | A | R15 |
| X12 | New file placement follows the ownership ladder | R | `architecture.md` §5 |
| X13 | A referenced ProGuard file exists, so the release path can run | A | R16 |
| X14 | Persistence is created with a corruption handler | A | R16 |
| X15 | Vendored GPL code stays out of the shared source set | A | R4 · D25 |
| X16 | No game-specific code in `src/main/java` | A | D25 |
| X17 | Every manifest pins `android:allowBackup="false"` | A | R4 |

X15 is the licensing half of the flavour split: GPL-3.0 attaches to whatever artifact ships the code, so an
engine in `src/main` is linked into every flavour — including future games that contain none and would
inherit the obligation for nothing. X16 is the general form and is **automated** (`G2-pure-main`): any
`games/` or engine package in the shared source set fails the build, because a game there ships inside
every app whether it belongs there or not. X17 closes the one manifest-level path by which player data
leaves the device: `allowBackup` defaults to **true** when absent, and device transfer can lift the
app's private DataStore — saved sessions and stats — off the player's device.

X7 and X8 are structural: Gradle holds the module direction, and `Spacing`'s wrapped `Dp` is `internal` to
the design system, so `Modifier.pad(16.dp)` does not compile in a consumer. X11's cap was printed and never
applied until recently — a cap that does not fail the build is how ten escape hatches become fifty. X14
exists because an unreadable preferences file was a launch crash: the store is read during composition and
in a ViewModel `init`, neither of which can catch.

## Games

| # | Rule | | Authority |
|---|---|---|---|
| M1 | A game must pass every admission criterion to enter the catalog | **R** | R6 |
| M2 | Rejected candidates recorded with the criterion they failed | **R** | R6 |
| M3 | One game `in_progress` at a time, across all tiers | A | R7 |
| M4 | A tier opens only when the previous tier is fully implemented and clean | **R** | R7 |
| M5 | Every game module must appear in the roadmap | A | R5 |
| M6 | Games named parenthetical lowercase — `sudoku(9)` | R | D4 |
| M7 | Puzzles solvable by deduction alone, at every difficulty | **R** | R6 |
| M8 | Givens and player entries stored separately; a given is never editable | **R** | R16 · D7 |
| M9 | Generation runs off the main thread | A | R16 |
| M10 | At most three readouts per game screen — curated, not raw state | A | D13 |
| M11 | A completed game emits its result exactly once | **R** | R16 |
| M12 | Game dir, flavour srcDirs line, and registry registration agree per flavour | A | R5 · D25 |
| M13 | Every shipped flavour owns its own application id | A | R5 · D27 |

M8 is the invariant that makes a given uneditable across a restart, and it is **review** — nothing checks
that a new game keeps the two grids apart. `sudoku(9)` does it correctly and additionally validates it on
restore; copy that shape. M9 is now automated via the raw-concurrency ban plus the requirement that
generation be cancellable. M11 is review because `onComplete` being wired is not textually checkable — and
it was unwired for the entire life of `sudoku(9)` before anyone noticed. M12 is the flavour-split
expression of M5: a game directory no flavour attaches compiles nowhere, a srcDirs line without a
registration ships a game that skipped admission, and a registration without its directory cannot
compile — all three drifted before the gate existed. M13 keeps each shipped app installable alongside
the others: a flavour without its own `applicationIdSuffix` silently installs over the studio app.

## Craft — not automatable, still binding

| # | Rule | | Authority |
|---|---|---|---|
| Q1 | Expose a *composed* view of the mechanism, never a raw state dump | R | `nothing-study.md` §11 |
| Q2 | Instrument panel, not app | R | `nothing-study.md` §2 |
| Q3 | Loading and empty states get the care of a main screen | R | `nothing-study.md` §7 |
| Q4 | Haptics are the feedback channel | R | D15 |
| Q5 | One purpose per screen | R | `design-spec.md` |
| Q6 | Whitespace is a design element, not absence of content | R | `design-spec.md` |
| Q7 | Target the austere Nothing era deliberately | R | D1 |

Every rule in this section is `R` by nature. They are the ones that decide whether the result is good, and
none of them is mechanically checkable — which is the honest reason a conformance checker is a floor and not
a standard.

---

## The review list, in one place

Everything the build will not catch. If you check nothing else, check these — grouped by when they bite.

**Before you write code**

- M1 · the game passes every admission criterion in `games-roadmap.md` §3
- M2 · if it fails one, record the rejection with the measurement
- M4 · the previous tier is fully implemented and clean
- M6 · the name is parenthetical lowercase, the number is the defining dimension
- M7 · puzzles are solvable by deduction alone, at every difficulty
- X12 · every new file placed by the ownership ladder in `architecture.md` §5

**While you write it**

- M8 · givens and player entries in **separate** grids; a given is never editable, including after a restart
- M11 · `onComplete` is actually called, exactly once per finish
- S4 · every interactive element has a 48dp minimum touch target, even when drawn smaller
- S7 · the optical token is never used for layout
- T6 · at most three type sizes on the screen
- T7 · at most two font weights
- T8 · at most one display-level element
- T9 · a word wherever a word fits, never an icon
- G4 · the dot matrix is a background layer, never a border or a button
- G6 · it consumes no pointer event and exposes no accessibility node
- C6 · dark is the fallback when the system preference cannot be read
- K2 · the generated token file is never hand-edited
- P15 · no colour resource outside the design system's single XML

**Before you call it done**

- C7 · **count the red elements. In normal play the answer is zero.**
- C8 · at most one when something is genuinely wrong
- C11 · every error state carries a non-colour indicator — verify by desaturating
- C12 · collision priority: destructive, then error, then urgent
- S8 · groups separated by the group-separation token or larger, never a line
- S15 · reduced motion applies end states with no intermediate frames
- S16 · at most eight elements animating at once
- G5 · dot positions cached, recomputed only on a bounds change
- G7 · the dot matrix is never thinned or faded for performance
- P9 · P12 · no cross-platform wrapper, no debug build shipped as release
- X4 · spec documents at a vendor-neutral path
- Q1–Q7 · the craft rules below
- **The four review tests.**

---

## The four review tests

Run before calling any screen done. `nothing-study.md` §12.

1. **Squint** — blur until detail is gone. Is the composition still identifiably ours?
2. **Grayscale** — desaturate. Is every error state still readable?
3. **Gimmick** — point at each distinctive element and name its job. No answer means delete it.
4. **Detail** — zoom to 400%. Tracking correct, figures tabular, baseline honoured?

Test 1 is the one that gets skipped and the one that matters most. If the blurred screenshot collapses into
"generic dark grid app", the layout has failed regardless of how many rules above it satisfied.

---

## What none of this guarantees

Every automated rule can pass on a screen that is badly composed and unlike Nothing. **36 green rules and a
bad screen is an entirely reachable state.** Automation buys consistency and correctness; it cannot buy
composition, hierarchy, or restraint.

That is not a reason to distrust the gates — it is the reason the `R` list above exists and is not shorter.
Automation exists to make the mechanical failures impossible so your attention is free for the ones that
need judgement. Spending review effort re-checking dp literals is the actual waste.

See `rationale.md` §9 for the honest limits of each enforcement layer — including why the module boundary is
enforcement rather than impossibility, and why the composition guard is weaker than it looks.
