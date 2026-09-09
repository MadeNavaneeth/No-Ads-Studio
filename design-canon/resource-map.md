---
status: canonical
---

# Resource Map — LightApp (Nothing Sudoku)

## Sources & Where Everything Came From

| # | Source | Repo | What we took | License |
|---|--------|------|-------------|---------|
| 1 | **Dotboard** | [amal-infosec/dotboard](https://github.com/amal-infosec/dotboard) | Nothing color palette (exact hex values), theme structure, NDot font usage pattern | MIT |
| 2 | **NDot / NType Fonts** | [xeji01/nothingfont](https://github.com/xeji01/nothingfont) | NDot55, NDot57, NType82, NType82Mono, SpaceGrotesk, SpaceMono font files (.otf) | ⚠️ **NOT OFL — see licensing warning below** |
| 3 | **LibreSudoku** | [kaajjo/LibreSudoku](https://github.com/kaajjo/LibreSudoku) | QQWing Sudoku generator/solver engine, GameType, GameDifficulty, Note model | GPL-3.0 |
| 4 | **nothing-design-skill** | [dominikmartn/nothing-design-skill](https://github.com/dominikmartn/nothing-design-skill) | Complete design token system (colors, typography scale, spacing, motion, dot-matrix), design philosophy, craft rules | MIT |

---

## File Placement Map

### 📁 Fonts → `app/src/main/res/font/`

| File | From | Used For |
|------|------|----------|
| `ndot55_regular.otf` | nothingfont | Sudoku numbers, hero display text |
| `ndot57_regular.otf` | nothingfont | Alternative dot-matrix (tighter dots) |
| `ntype82_regular.otf` | nothingfont | Body text, UI labels (Nothing's actual UI font) |
| `ntype82mono_regular.otf` | nothingfont | Data display, timers, scores |
| `space_grotesk_regular.otf` | nothingfont | Fallback body font (same foundry as NType) |
| `space_mono_regular.otf` | nothingfont | Fallback data/label font |

### 📁 Theme → `app/src/main/java/com/example/lightapp/theme/`

| File | From | What It Defines |
|------|------|----------------|
| `NothingColors.kt` | Dotboard palette + nothing-design-skill tokens | All color tokens: backgrounds, text hierarchy (4 levels), accent red, borders, status colors, light/dark mode |
| `NothingTheme.kt` | Dotboard Theme.kt pattern + nothing-design-skill | Material3 theme wrapper — dark OLED scheme + light scheme, status bar color |
| `NothingTypography.kt` | nothingfont fonts + nothing-design-skill type scale | Full type scale: NDot display, NType82 body, SpaceMono data/labels |

### 📁 Sudoku Engine → `app/src/main/java/com/example/lightapp/sudoku/core/`

| File | From | What It Does |
|------|------|-------------|
| `QQWing.kt` | LibreSudoku | Core Sudoku generator + solver (1740 lines, battle-tested) |
| `QQWingController.kt` | LibreSudoku | Multi-threaded puzzle generation controller |
| `GameType.kt` | LibreSudoku | Enum: 9x9, 12x12, 6x6, Killer variants |
| `GameDifficulty.kt` | LibreSudoku | Enum: Simple → Challenge difficulty levels |
| `Action.kt` | LibreSudoku | Generator action types |
| `Symmetry.kt` | LibreSudoku | Puzzle symmetry options |
| `PrintStyle.kt` | LibreSudoku | Output formatting |
| `Note.kt` | LibreSudoku | Pencil mark data class |

### 📁 Documentation → `docs/`

| File | From | What It Covers |
|------|------|---------------|
| `resource-map.md` | This file | Where everything came from and where it's placed |
| `nothing-tokens.md` | nothing-design-skill | Full color system, typography scale, spacing, motion, dot-matrix motif |
| `nothing-design-system.md` | nothing-design-skill | Complete design philosophy, craft rules, anti-patterns |

---

## How to Use in Your Code

### 1. Apply the Nothing theme
```kotlin
// In your Activity/Composable
NothingGameTheme(darkTheme = true) {
    // Your Sudoku game UI
}
```

### 2. Use NDot font for Sudoku numbers
```kotlin
Text(
    text = "5",
    style = MaterialTheme.typography.displayLarge, // NDot font
    color = MaterialTheme.colorScheme.primary
)
```

### 3. Use Space Mono for labels
```kotlin
Text(
    text = "DIFFICULTY",
    style = MaterialTheme.typography.labelMedium, // Space Mono, ALL CAPS
    color = MaterialTheme.colorScheme.secondary
)
```

### 4. Generate a Sudoku puzzle
```kotlin
val controller = QQWingController()
val puzzle = controller.generate(GameType.Default9x9, GameDifficulty.Moderate)
val solution = controller.solve(puzzle, GameType.Default9x9)
```

---

## Notes

- **LibreSudoku is GPL-3.0** — if you publish your app, you must open-source your code or keep it completely offline (which this project does)
- **Dotboard + nothing-design-skill are MIT** — free to use

### ⚠️ Font licensing warning — blocks Play Store release

An earlier version of this file claimed the NDot/NType fonts are SIL Open Font Licensed and free for
commercial use. **That claim is unsupported.** Two things contradict it:

- The source repo credits "NOTHING Tech. All Rights Reserved." — not an OFL grant.
- [NType 82 was designed in 2021 for the exclusive use of Nothing](https://fontsinuse.com/typefaces/233368/ntype-82).
  NDot and NType 82 Mono came from the same commission.

These are proprietary brand typefaces. Bundling them in a published app is a real risk. This is a
flag to verify, not legal advice.

**The replacement is both safer and more accurate to current Nothing:**

| Role | Use this | Licence | Why |
|---|---|---|---|
| Body / UI | **Geist Sans** | [SIL OFL](https://github.com/vercel/geist-font) | Nothing OS 5.0 uses Geist as its primary system typeface |
| Data / Labels | **Geist Mono** | SIL OFL | Same family, true monospace, tabular figures |
| Display / numerals | **Doto** | SIL OFL, on Google Fonts | 6×10 dot matrix with variable dot-size and roundness axes |

Geist is not a compromise. It is what Nothing actually ships today, so moving to it raises fidelity
while removing the legal exposure. Doto's variable dot-size axis is a bonus the static NDot files
cannot offer — dot size becomes animatable.

Note also that Nothing's own website pairs its custom faces with
[LL Lettera Mono](https://fontsinuse.com/uses/59510/nothing-phone). Space Mono was standing in for
Lettera Mono, not for NType 82 Mono.

---

## Official Nothing sources (first-party)

Everything above this line is community interpretation. These are Nothing themselves.

| Source | What it is |
|---|---|
| [Nothing-Developer-Programme](https://github.com/Nothing-Developer-Programme) | Nothing's official GitHub organisation |
| [Glyph-Developer-Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) | Official Glyph SDK, ships `glyph-matrix-sdk-2.0.aar`. Nothing devices, Android 14+ |
| [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) | Glyph Matrix kit for Phone (3) — the 25×25 rear LED grid |
| [GlyphMatrix-Example-Project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project) | Working reference implementation |
| [nothing.tech/pages/glyph-developer-kit](https://nothing.tech/pages/glyph-developer-kit) | Official landing page for the kit |
| [nothing.community](https://nothing.community) | Official forum. The `ndot57-the-nothing-typeface` thread has staff input |
| Nothing Icon Pack (`com.nothing.icon`) | Nothing's own monochrome icon set on Play — a shippable artifact to study |
| Nothing OS itself | The primary source. Screenshot Weather, Clock, Recorder, Gallery and measure |

## Icons — and why we take almost none

**We need roughly three icons, not a thousand.** The design language uses words where other apps use
icons: the top bar says `BACK`, the pad says `ERASE` / `NOTES` / `UNDO`, settings rows are labelled text.
So an icon library is the wrong shape of solution — take a dependency for three glyphs and you inherit
1,000 you never draw.

**Approach:** hand-port the two or three paths actually needed into Compose `ImageVector` declarations
inside `design-system/components/`, set to our own 1.5dp stroke. No dependency, no SVG assets, no
resource lookup. Record each ported path below.

| Source | Licence | Fit |
|---|---|---|
| [Lucide](https://github.com/lucide-icons/lucide) | ISC | 1,000+ monoline, Feather descendant. Removed all brand icons in v1 for legal reasons, so no trademark exposure. |
| [Tabler Icons](https://github.com/tabler/tabler-icons) | MIT | 6,184 icons, explicitly **24×24 grid, 2px stroke** — closest geometry to our 24dp base |
| [Phosphor](https://github.com/phosphor-icons/homepage) | MIT | ships Thin and Light weights, which suits Nothing's ultra-light instinct |

Note none of these are natively 1.5px. Stroke width is a render parameter on an `ImageVector`, so we set
ours and the source geometry is unaffected.

If a dependency ever becomes justified: [lucide-icon-kmp](https://github.com/ShermanTsang/lucide-icon-kmp)
wraps Lucide for Compose Multiplatform with a build-time generator for only the bundled icons, and
[iconsax-compose](https://github.com/RabehX/iconsax-compose) offers Linear and Outline styles. Both are
currently barred by the no-heavy-dependency rule.

## Dot-matrix authoring and reference

For `GameIcon.DotMatrix` patterns and the dot-matrix loading state.

| Source | Use |
|---|---|
| [dotmatrixtool](https://github.com/stefangordon/dotmatrixtool) | Draw on a pixel grid in the browser, export arrays. The authoring tool for our 5×5 game icons. |
| [smittytone/ASCII](https://github.com/smittytone/ASCII) | macOS tool for designing 8×8 LED-matrix glyphs. Runs natively on the Reference_Machine. |
| [dot-matrix-animations](https://github.com/icantcodefyi/dot-matrix-animations) | 28 hand-designed **5×5** dot-matrix loader animations — exactly our icon grid size, and direct reference for the `GENERATING` state instead of a spinner |
| [code4fukui/led-matrix](https://github.com/code4fukui/led-matrix/) | Text-rows pattern format worth copying for how we express boolean grids in source |
| [trip5/Matrix-Fonts](https://github.com/trip5/Matrix-Fonts) | LED matrix fonts at 6 and 8 rows, if we ever need dot type below Doto's floor |

These are references and authoring tools. Nothing from them ships as an asset — patterns become boolean
arrays in Kotlin.

## Community sources worth reading

| Source | Value |
|---|---|
| [shadcn.io/design/nothing](https://www.shadcn.io/design/nothing) | React Nothing design system, 14 components. Sharp observations — e.g. NType82 headlines run at weight 100 so type recedes and product imagery dominates |
| [rec0de/glyph-api](https://github.com/rec0de/glyph-api) | Reverse-engineered Glyph light API docs, predates the official SDK |
| [TeamNothingMuch/NothingMuch](https://github.com/TeamNothingMuch/NothingMuch) | Icon pack built on Nothing brand colours |
| [Glyph-Aquarium](https://github.com/chriskenhall-tech/Glyph-Aquarium) | Procedural animation on the 25×25 Glyph Matrix — proof of how expressive it gets |
- The QQWing engine references `R.string.*` resources — you'll need to add string resources or remove those references for your use case
