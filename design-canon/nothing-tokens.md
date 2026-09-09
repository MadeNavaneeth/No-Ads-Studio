---
status: canonical
---

# Nothing Design System — Tokens

> **Prose reference, not the source.** `design-canon/tokens.json` is the source — it is machine-readable,
> it generates the theme package, and its values are validated by `:design-system:test`. This file exists
> to explain the system in words. Where the two disagree, `tokens.json` wins and this file is the bug.
>
> `AGENTS.md` and this file may only name values that appear in `tokens.json`. Both are checked by
> `conformanceCheck` (`R17-agents-token-drift`, `R17-canon-token-drift`).

## 1. TYPOGRAPHY

### Font Stack

Three roles, filled by three open-licensed faces. The project does **not** ship Nothing's own typefaces
and never will — see "Never ship" below. The px figures throughout this file are the web notation; the
Android mapping is in "Unit mapping" below.

### Shippable font stack (primary — what the theme package must declare)

| Role | Face | Licence | Applies to |
|------|------|---------|------------|
| **Display / hero numerals** | Doto | SIL OFL | 36sp and above only |
| **Body / UI** | Geist Sans | SIL OFL | headings, body, all UI text |
| **Data / Labels** | Geist Mono | SIL OFL | all-caps labels, timers, scores |

Geist is what **Nothing OS 5.0 actually ships** as its primary system typeface, so this stack is more
faithful to current Nothing than the older custom faces, not a compromise. Doto is a 6×10 dot-matrix
family with variable dot-size and dot-roundness axes — the variable axis makes dot size animatable,
which the static NDot files cannot do.

### Never ship: NDot 55/57, NType 82, NType 82 Mono

These are Nothing's proprietary brand typefaces, commissioned for their exclusive use. They are **not**
openly licensed, despite a claim to the contrary in an earlier revision of `resource-map.md` — the error
that put them in the project in the first place.

**They have been deleted from the repository.** `res/font/` now holds only the two OFL stand-ins. This is
no longer a review obligation: `conformanceCheck` rule `R9-proprietary-fonts` fails the build both on any
source reference matching `ndot5[57]` or `ntype82` and on the presence of any file in `res/font/` whose
name contains `ndot` or `ntype`. Re-adding them is a build failure, not a code-review conversation.

Nothing's own website pairs its custom faces with **LL Lettera Mono** (Lineto). Geist Mono is the open
stand-in for that role.

### Unit mapping, Android

The scale below is written in px for web. On Android, **px maps 1:1 to sp** for size and line height.
**Letter spacing is em, not sp** — Compose accepts `.em` directly, and writing an em figure as `.sp`
silently produces roughly no tracking at all.

### Type Scale

| Token | Size | Line Height | Letter Spacing | Use |
|-------|------|-------------|----------------|-----|
| `--display-xl` | 72px | 1.0 | -0.03em | Hero numbers, time displays |
| `--display-lg` | 48px | 1.05 | -0.02em | Section heroes, percentages |
| `--display-md` | 36px | 1.1 | -0.02em | Page titles |
| `--heading` | 24px | 1.2 | -0.01em | Section headings |
| `--subheading` | 18px | 1.3 | 0 | Subsections |
| `--body` | 16px | 1.5 | 0 | Body text |
| `--body-sm` | 14px | 1.5 | 0.01em | Secondary body |
| `--caption` | 12px | 1.4 | 0.04em | Timestamps, footnotes |
| `--label` | 11px | 1.2 | 0.08em | ALL CAPS monospace labels |

### Typographic Rules

Stated in terms of the three **roles**, not of whichever face currently fills them. That indirection is
the point: the stand-ins are swapped by editing `Fonts.kt` alone, and a rule written against a face name
would have to be rewritten too.

- **Display:** Doto, 36sp and above only, tight tracking, never body text. Below 36sp a dot-matrix face
  stops resolving into legible characters, which is why the floor exists and why it is the same number
  as the display-level threshold in Requirement 9 criterion 7.
- **Labels:** always the **mono** role, ALL CAPS, 0.06–0.1em tracking, 11–12sp. These are instrument-panel
  labels; upper-casing happens in the `Label` composable, not at the call site, so a lowercase label is
  unexpressible.
- **Data and numerals:** always the **mono** role, with **tabular figures** on anything that counts. Units
  at `--label` size, adjacent. A timer whose digits change width as it runs is the single most visible
  craft failure available on this screen.
- **Hierarchy:** display (Doto) > heading (sans) > label (mono caps) > body (sans). Four levels max.

### Currently shipping: stand-ins, not the target

`res/font/` holds **Space Grotesk** and **Space Mono**, and `Fonts.kt` maps all three roles onto them
with `FONTS_ARE_PLACEHOLDERS = true`.

| Role | Target | Shipping today | Gap |
|------|--------|----------------|-----|
| Display | Doto | Space Grotesk | **Real.** Space Grotesk has no dot matrix, so hero numerals carry none of the departure-board reference that makes them Nothing. |
| Body / UI | Geist Sans | Space Grotesk | Minor. Same foundry lineage, similar geometric-grotesque proportions. |
| Labels / data | Geist Mono | Space Mono | Minor. A true monospace, so tabular-figure layout is already correct. |

Both stand-ins are SIL OFL. The display gap is the one that matters visually — until Doto lands, the
dot-matrix voice is absent from type and carried only by `DotMatrixReadout`.

Do not write rules or specs against "Space Grotesk" or "Space Mono". They are what is loaded, not what
is intended.

---

## 2. COLOR SYSTEM

### Primary Palette (Dark Mode)

| Token | Hex | Contrast on #000 | Role |
|-------|-----|-------------------|------|
| `--black` | `#000000` | — | Primary background (OLED) |
| `--surface` | `#141414` | 1.3:1 | Elevated surfaces, cards |
| `--surface-raised` | `#1F1F1F` | 1.5:1 | Secondary elevation |
| `--border` | `#2A2A2A` | — | Subtle dividers (decorative only) |
| `--border-visible` | `#3A3A3A` | — | Intentional borders, wireframe lines |
| `--text-disabled` | `#707070` | 3.1:1 | Disabled text, decorative elements |
| `--text-secondary` | `#999999` | 6.3:1 | Labels, captions, metadata |
| `--text-primary` | `#E8E8E8` | 16.5:1 | Body text |
| `--text-display` | `#FFFFFF` | 21:1 | Headlines, hero numbers |

### Accent

There is exactly one, and a screen in normal play does not use it.

| Token | `tokens.json` | Value | Usage |
|-------|---------------|-------|-------|
| `--accent` | `accentRed` | `#D71921` | Outline of the single most urgent element on a screen. Normally absent. Never text, never decorative. |
| `--accent-subtle` | `accentRedSubtle` | `#D71921` at 15% | Fill behind an urgent element where an outline alone is too quiet |

Red is never assigned to a type style. It measures 3.64:1 on `surface`, which fails the 4.5:1 threshold
for normal-size text but clears the 3:1 threshold for a non-text element, so it is available as border,
outline or fill and nothing else. The derivation is in `rationale.md` §8.

### Removed: status and interactive colours

Earlier revisions of this file documented `--success` (green), `--warning` (amber), `--info`, and
`--interactive` (blue), plus a "data status colours are exempt from the one-accent rule" carve-out.

**None of them exist in `tokens.json`, and all of them contradict the palette rule.** The hard rule is
black, white, grey, plus one red for error only; `ideas/avoid.md` bars bright and branded colour
outright. A green success state and a blue link are ordinary interface conventions, which is exactly
why they crept in — and adopting them would have made this look like every other app with a dark theme.

They are recorded here as removed rather than silently deleted, because the pressure to re-add them is
real and recurring. If a state needs distinguishing, the channels available are, in order of
preference:

1. **Type role** — `textDisplay` / `textPrimary` / `textSecondary` / `textDisabled`
2. **Surface role** — `bg` / `surface` / `surfaceRaised`
3. **Border role** — `borderSubtle` / `borderVisible`
4. **A word** — an all-caps mono label saying what the state is
5. **Opacity or dot pattern** — for data series that must be told apart
6. **Haptics** — see decision D15; this carries more weight here than in most systems

Reaching step 6 without solving it means the screen is trying to say too much, not that the palette is
too small.

### Dark / Light Mode

| Token | Dark | Light |
|-------|------|-------|
| `--black` | `#000000` | `#F5F5F5` |
| `--surface` | `#141414` | `#FFFFFF` |
| `--surface-raised` | `#1F1F1F` | `#F0F0F0` |
| `--border` | `#2A2A2A` | `#E8E8E8` |
| `--border-visible` | `#3A3A3A` | `#CCCCCC` |
| `--text-disabled` | `#707070` | `#8A8A8A` |
| `--text-secondary` | `#999999` | `#666666` |
| `--text-primary` | `#E8E8E8` | `#1A1A1A` |
| `--text-display` | `#FFFFFF` | `#000000` |

**Identical across modes:** accent red, ALL CAPS labels, fonts, type scale, spacing, component shapes.

**Dark feel:** Instrument panel in a dark room. OLED black, white data glowing.
**Light feel:** Printed technical manual. Off-white paper (#F5F5F5), black ink. Cards = `#FFFFFF` on off-white page = subtle elevation without shadows.

---

## 3. SPACING

### Spacing Scale (8px base)

| Token | Value | Use |
|-------|-------|-----|
| `--space-2xs` | 2px | Optical adjustments only |
| `--space-xs` | 4px | Icon-to-label gaps, tight padding |
| `--space-sm` | 8px | Component internal spacing |
| `--space-md` | 16px | Standard padding, element gaps |
| `--space-lg` | 24px | Group separation |
| `--space-xl` | 32px | Section margins |
| `--space-2xl` | 48px | Major section breaks |
| `--space-3xl` | 64px | Page-level vertical rhythm |
| `--space-4xl` | 96px | Hero breathing room |

---

## 4. MOTION & INTERACTION

- **Duration:** 150–250ms micro, 300–400ms transitions
- **Easing:** `cubic-bezier(0.25, 0.1, 0.25, 1)` — subtle ease-out. No spring/bounce.
- Prefer opacity over position. Elements fade, don't slide.
- Hover: border/text brightens. No scale, no shadows.
- No parallax, scroll-jacking, gratuitous animation.

---

## 5. ICONOGRAPHY

- Monoline, 1.5px stroke, no fill. 24x24 base, 20x20 live area. Round caps/joins.
- Color inherits text color. Max 5–6 strokes.
- Preferred: Lucide (thin), Phosphor (thin). Never filled or multi-color.

---

## 6. DOT-MATRIX MOTIF

**A dot grid must encode something. There is no decorative use.**

This is the rule most likely to be broken by accident, because a dot grid is attractive and cheap. An
earlier revision of this file listed "decorative grid backgrounds" and "empty state illustrations" as
valid uses, which contradicts `nothing-study.md` §3 and the hard rule in `AGENTS.md`. Nothing's dot
matrix descends from a transit departure board — a surface whose entire purpose is conveying information
at a glance. A dot grid that means nothing is the single clearest signal that the look was copied without
the idea.

| Use | Verdict |
|-----|---------|
| Progress, completion, or remaining count | **Yes** — this is the motif's job |
| Hero numerals in the display face | **Yes** — Doto is itself a dot matrix |
| A game grid whose cells are genuinely two-state | **Yes** — e.g. `nonogram(10)`, where the solution *is* dot-matrix art |
| Wallpaper, texture, filler behind content | **No** |
| Empty-state illustration | **No** — an empty state gets a word, not a picture |
| Container border or button styling | **No** |

**Values.** Dots at `optical2` diameter on a `space16` or `space24` pitch, alpha between `dotAlphaDim`
(0.10) and `dotAlphaBright` (0.20). Below 0.10 the texture vanishes; above 0.20 it competes with the game
grid for attention. `DotMatrixReadout` takes a `progress: Float` and has no parameter for anything else —
the component is built so that drawing dots without binding them to a quantity is not expressible.

### Deriving a readout for a new game

Pick the game's single most representative quantity, normalise it to 0..1, and pass it in. The mapping
from that fraction to dot brightness is the component's business, not the caller's. See
`game-design-method.md` §2 for how to choose the quantity.
