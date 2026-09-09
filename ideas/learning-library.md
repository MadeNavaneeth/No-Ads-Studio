---
status: research
---

# Learning Library

Books, docs, tools, and Nothing's own material. Ordered by how much it will actually change your code.

Values and rules live in `design-canon/`. This is reading, not authority.

---

## 1. Nothing's first-party developer surfaces

These are Nothing themselves, not community interpretation.

| Resource | What it is |
|---|---|
| **[Nothing Playground](https://playground.nothing.tech/)** | Nothing's own app-building platform — "build apps and browse community creations." First-party, and directly relevant if you build beyond games. |
| [Glyph Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) | Official SDK, ships `glyph-matrix-sdk-2.0.aar` |
| [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) | The 25×25 rear LED grid on Phone (3) |
| [GlyphMatrix Example Project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project) | Working reference implementation |
| [Nothing-Developer-Programme](https://github.com/Nothing-Developer-Programme) | The whole org — watch it for new kits |
| [nothing.community](https://nothing.community) | Official forum. Staff participate; design intent surfaces here before press. |

Worth knowing: Nothing has been pushing an AI app-builder for their OS, and The Verge's assessment was
that [vibe coding Nothing's apps is fun until you try to make them useful](https://www.theverge.com/tech/876229/nothing-essential-ai-app-builder).
Useful calibration — the aesthetic is easy to generate, the substance is not. Which is the whole argument
for the harness you've built.

---

## 2. Nothing's own apps — the primary source

Install these and study them. This teaches more about fidelity than any token table, and it is the fix
for the accuracy ceiling noted in `nothing-study.md` §8.

| App | Package | Study it for |
|---|---|---|
| Essential Space | `com.nothing.ntessentialspace` | typography-first layout, whitespace-heavy composition |
| Nothing Gallery | `com.nothing.gallery` | grid density, empty states, restraint at scale |
| Nothing Icon Pack | `com.nothing.icon` | monoline stroke weight, grid discipline |
| Nothing Launcher | — | widget typography, dot-matrix clock treatment |
| Nothing Widgets | — | how much information fits before it stops being calm |
| Weather · Clock · Recorder | — | instrument-panel readouts. The closest analogue to our game screen. |

Full list: [NOTHING TECHNOLOGY LIMITED on Play](https://play.google.com/store/apps/developer?id=NOTHING%20TECHNOLOGY%20LIMITED).

**If you are not on a Nothing device:** [nitanmarcel/NothingCore](https://github.com/nitanmarcel/NothingCore)
is a Magisk/Xposed module that installs Nothing OS apps on any device, covering everything except Camera.
It needs root — that is a real risk to your daily driver, so treat it as a spare-device exercise rather
than a casual one.

---

## 3. Open-source code in the Nothing aesthetic

Reading someone else's Nothing-style Android code is faster than deriving it.

| Repo | Value |
|---|---|
| **[Roqak/NothingLauncher](https://github.com/Roqak/NothingLauncher)** | A pixel-accurate monochrome Nothing OS home screen replacement for Android 8+. The single most useful code reference here — a real Android app in this exact language. |
| [amal-infosec/dotboard](https://github.com/amal-infosec/dotboard) | Where your colour palette and NDot usage pattern originally came from (MIT) |
| [kaajjo/LibreSudoku](https://github.com/kaajjo/LibreSudoku) | Source of the QQWing engine. Also a working Compose Sudoku — read it for grid input handling, then do the UI differently. |
| [xCaptaiN09/glyph-sddm](https://github.com/xCaptaiN09/glyph-sddm) | Nothing-inspired login theme; dot-matrix typography and adaptive monochrome |
| [Glyph-Aquarium](https://github.com/chriskenhall-tech/Glyph-Aquarium) | Procedural animation on the 25×25 matrix — proof of how expressive functional dots get |

---

## 4. Official Android docs — free, authoritative, and the highest-leverage reading

Read these before any book. They are maintained, current, and written by the people who built it.

**Directly load-bearing for our constraints:**

| Doc | Why it matters here |
|---|---|
| [Stability in Compose](https://developer.android.com/develop/ui/compose/performance/stability) | The `avoid.md` rule against redundant recomposition is unenforceable until you understand stable versus unstable types. Read this first. |
| [Compose phases and performance](https://developer.android.com/develop/ui/compose/performance/phases) | Lambda-based modifiers and deferred state reads. This is how the 81-cell grid stays at 60fps on your hardware. |
| [Follow best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices) | The canonical don't-recompute-in-composition guidance |
| [Lifecycle of composables](https://developer.android.com/develop/ui/compose/lifecycle) | Keys, and why a 9×9 grid needs them |
| [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state) | State hoisting — the shape of `SudokuViewModel` |
| [Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model) | The mental model. Skip at your peril. |
| [Performance codelab](https://developer.android.com/codelabs/jetpack-compose-performance) | Hands-on: measure, trace, fix. Do this one. |
| [Compare Compose and View metrics](https://developer.android.com/develop/ui/compose/migrate/compare-metrics) | APK size and startup cost — relevant to the 1-second launch budget |

If you read only two things ever: **Stability in Compose** and **Compose phases**. Together they are most
of what separates a Compose app that feels like Nothing from one that stutters.

---

## 5. Books — with a warning

**Be careful here.** Searching "Jetpack Compose 2026" surfaces a flood of self-published titles with
generic names, no identifiable author reputation, and suspiciously recent dates. Much of it is
low-quality or machine-generated. Two credible lines:

| Book | Why it is credible |
|---|---|
| **Jetpack Compose Essentials** — Neil Smyth, [2026 edition](https://play.google.com/store/books/details/Neil_Smyth_Jetpack_Compose_Essentials_2026_Edition?id=A0f7EQAAQBAJ) | Revised annually against current Compose versions. Comprehensive, reference-shaped rather than narrative. Good for lookup. |
| **Android UI Development with Jetpack Compose** — Thomas Kunneth | Kunneth is a Google Developer Expert and a working senior Android developer. Real authority rather than repackaged docs. |

Honest assessment: for this project the official docs plus the LibreSudoku and NothingLauncher source
will teach you more than either book. Buy a book for structured grounding if you want it, not because the
project needs it.

---

## 6. Tooling that fits our constraints

All JVM-side or build-time, so nothing lands in the release artifact.

| Tool | Use |
|---|---|
| **[Vkompose](https://www.forasoft.com/blog/article/vkompose-jetpack-compose-performance)** | VK's open-source toolkit: Gradle plugin, IntelliJ plugin, and **detekt rules that block recomposition issues at compile time**. We already need detekt for the R15 forbidden-type gate, so this makes the `avoid.md` recomposition rule mechanically enforced instead of aspirational. Strongest addition available. |
| Paparazzi | JVM screenshot tests, no emulator. Decision D9. |
| Konsist | The R15 architecture test — module boundaries as a unit test |
| detekt | Hosts the forbidden-type rule bound to `check` |
| Android Studio Layout Inspector | Recomposition counts per composable. Use it on the grid. |

Add Vkompose's detekt rules to the denylist's permitted list when you wire it up.

---

## 7. How to actually study their apps

A method, not a reading list. Half a day, and it raises fidelity more than a week of guessing.

1. **Build a reference board.** Screenshot Nothing Weather, Clock, Recorder, Gallery. Put each beside the
   equivalent screen of yours at matched scale in one document.
2. **Measure, do not eyeball.** Take the screenshots into any editor and measure label tracking, gap
   rhythm, and stroke weights in pixels. Convert to dp at the device's density.
3. **Count.** How many type sizes per screen? How many weights? How much of the screen is empty? Compare
   against our limits of three sizes and two weights — they may be stricter than us.
4. **Find the readouts.** Every number they display, and how it is set. This is the instrument-panel
   vocabulary we are borrowing.
5. **Look for the accent.** Note how rarely any colour appears. This is the evidence behind decision D2.
6. **Then run our four review tests** on your own screens: squint, grayscale, gimmick, detail.

---

## 8. If you build non-game apps — a scope note

You mentioned possibly building apps rather than only games. Worth being clear about what that costs,
because the harness is currently **game-shaped**:

- `GameDefinition`, `GameRegistry`, `GameResult`, and the `games-roadmap` all assume a game
- Requirement 5 fails a `Game_Module` that the roadmap does not list
- Requirements 6 and 7 gate admission and tiering on puzzle-specific criteria — generation time, puzzle
  validity, deduction-solvability

The design system itself is not game-shaped at all. `:design-system` — tokens, components, theme — would
serve any Nothing-style app unchanged, and that is the genuinely reusable asset.

**What would need to change:** rename the contract to something like `AppModule` with `GameDefinition` as
one implementation, generalise the roadmap's admission criteria into shared and game-specific sets, and
relax Requirements 6 and 7 accordingly.

**Resolved: games only.** See decision D21. The harness stays game-shaped until a non-game app is
actually specified rather than merely considered. `:design-system` is already app-agnostic, so nothing is
lost by waiting.
