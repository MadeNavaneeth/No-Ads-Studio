---
name: nothing-design
description: This skill should be used when the user explicitly says "Nothing style", "Nothing design", "/nothing-design", or directly asks to use/apply the Nothing design system. NEVER trigger automatically for generic UI or design tasks.
version: 3.0.0
allowed-tools: [Read, Write, Edit, Glob, Grep]
status: canonical
---

# Nothing-Inspired UI/UX Design System

A senior product designer's toolkit trained in Swiss typography, industrial design (Braun, Teenage Engineering), and modern interface craft. Monochromatic, typographically driven, information-dense without clutter. Dark and light mode with equal rigor.

**This file is the craft layer: how to compose. It states no values.** Every number, colour, and face comes from `design-canon/tokens.json`, explained in prose in `nothing-tokens.md`. Where this file and the canon disagree, the canon wins and this file is the bug — that has already happened twice, so it is worth stating plainly.

**Fonts are three roles, never three names:** a dot-matrix *display* face, a *sans* for body and UI, a *mono* for labels and data. In this project those are Doto, Geist Sans, and Geist Mono, currently stood in for by Space Grotesk and Space Mono. Write specs against the role. Never assume a face is already loaded — check `Fonts.kt`.

---

## 1. DESIGN PHILOSOPHY

- **Subtract, don't add.** Every element must earn its pixel. Default to removal.
- **Structure is ornament.** Expose the grid, the data, the hierarchy itself.
- **Monochrome is the canvas.** There is one accent — red — and a screen in normal operation does not use it. There are no status colours. See Section 2.5.
- **Type does the heavy lifting.** Scale, weight, and spacing create hierarchy — not color, not icons, not borders.
- **Both modes are first-class.** Dark mode: OLED black. Light mode: warm off-white. Neither is "derived" — both get full design attention. Ask the user which mode to start with.
- **Industrial warmth.** Technical and precise, but never cold. A human hand should be felt.
- **Demystification, not minimalism.** The point is not to hide the inessential but to expose the mechanism and make it beautiful. Those produce very different interfaces. See `nothing-study.md` §1 — and §11 for the correction that keeps it from becoming a debug overlay.

---

## 2. CRAFT RULES — HOW TO COMPOSE

### 2.1 Visual Hierarchy: The Three-Layer Rule

Every screen has exactly **three layers of importance.** Not two, not five. Three.

| Layer | What | How |
|-------|------|-----|
| **Primary** | The ONE thing the user sees first. A number, a headline, a state. | Display face at display size. `--text-display`. 48–96px breathing room. |
| **Secondary** | Supporting context. Labels, descriptions, related data. | Sans at body/subheading. `--text-primary`. Grouped tight (8–16px) to the primary. |
| **Tertiary** | Metadata, navigation, system info. Visible but never competing. | Mono at caption/label. `--text-secondary` or `--text-disabled`. ALL CAPS. Pushed to edges or bottom. |

**The test:** Squint at the screen. Can you still tell what's most important? If two things compete, one needs to shrink, fade, or move.

**Common mistake:** Making everything "secondary." Evenly-sized elements with even spacing = visual flatness. Be brave — make the primary absurdly large and the tertiary absurdly small. The contrast IS the hierarchy.

### 2.2 Font Discipline

Per screen, use maximum:
- **2 font families** (sans + mono. The display face only for hero moments, and only at its size floor or above.)
- **3 font sizes** (one large, one medium, one small)
- **2 font weights** (Regular + one other — usually Light or Medium, rarely Bold)

Think of it as a budget. Every additional size/weight costs visual coherence. Before adding a new size, ask: can I create this distinction with spacing or color instead?

| Decision | Size | Weight | Color |
|----------|:---:|:---:|:---:|
| Heading vs. body | Yes | No | No |
| Label vs. value | No | No | Yes |
| Active vs. inactive nav | No | No | Yes |
| Hero number vs. unit | Yes | No | No |
| Section title vs. content | Yes | Optional | No |

**Rule of thumb:** If reaching for a new font-size, it's probably a spacing problem. Add distance instead.

### 2.3 Spacing as Meaning

Spacing is the primary tool for communicating relationships.

```
Tight (4–8px)   = "These belong together" (icon + label, number + unit)
Medium (16px)    = "Same group, different items" (list items, form fields)
Wide (32–48px)   = "New group starts here" (section breaks)
Vast (64–96px)   = "This is a new context" (hero to content, major divisions)
```

**If a divider line is needed, the spacing is probably wrong.** Dividers are a symptom of insufficient spacing contrast. Use them only in data-dense lists where items are structurally identical.

### 2.4 Container Strategy (prefer top)

1. **Spacing alone** (proximity groups items)
2. A single divider line
3. A subtle border outline
4. A surface card with background change

Each step down adds visual weight. Use the lightest tool that works. Never box the most important element — let it float on the background.

### 2.5 Color as Hierarchy

In a monochrome system, the gray scale IS the hierarchy. Max 4 levels per screen:

```
--text-display (100%) → Hero numbers. One per screen.
--text-primary (90%)  → Body text, primary content.
--text-secondary (60%) → Labels, captions, metadata.
--text-disabled (40%) → Disabled, timestamps, hints.
```

**Red is not part of the hierarchy.** It's an interrupt — "look HERE, NOW." If nothing is urgent, no red on the screen. Not "at most one red" — *zero*, by default. Its rarity is the entire source of its power, and Nothing's own interfaces have no accent colour at all (`nothing-study.md` §4).

Red is also never text. As a type colour it fails the 4.5:1 threshold for normal-size text on every surface, so it is available as border, outline, or shape fill and nothing else — and when a shape *fills* with it, the text over the fill stays white `textDisplay` (5.19:1 on `#D71921`), never red. The Sudoku conflict cell is exactly that: the whole cell fills red, numeral white.

**There are no status colours.** No success green, no warning amber, no interactive blue, and no "data values are exempt from the one-accent rule" carve-out. Earlier revisions of this file and of `nothing-tokens.md` granted exactly that exemption; the colours it referred to were never in `tokens.json`, and `ideas/avoid.md` bars bright and branded colour outright.

To distinguish a state without colour, in order of preference: **type role** → **surface role** → **border role** → **a word** → **opacity or dot pattern** → **haptics**. If none of those separate it, the screen is saying too much — that is a composition problem, not a palette problem.

### 2.6 Consistency vs. Variance

**Be consistent in:** Font roles, label treatment (always mono, ALL CAPS), spacing rhythm, color roles, component shapes, alignment.

**Break the pattern in exactly ONE place per screen:** An oversized number, a circular widget among rectangles, a red accent among grays, a Doto headline, a vast gap where everything else is tight.

This single break IS the design. Without it: sterile grid. With more than one: visual chaos.

### 2.7 Compositional Balance

**Asymmetry > symmetry.** Centered layouts feel generic. Favor deliberately unbalanced composition:
- **Large left, small right:** Hero metric + metadata stack.
- **Top-heavy:** Big headline near top, sparse content below.
- **Edge-anchored:** Important elements pinned to screen edges, negative space in center.

Balance heavy elements with more empty space, not with more heavy elements.

### 2.8 The Nothing Vibe

1. **Confidence through emptiness.** Large uninterrupted background areas. Resist filling space.
2. **Precision in the small things.** Letter-spacing, exact gray values, 4px gaps. Micro-decisions compound into craft.
3. **Data as beauty.** `36GB/s` in mono at 48px IS the visual. No illustrations needed.
4. **Mechanical honesty.** Controls look like controls. A toggle = physical switch. A gauge = instrument.
5. **One moment of surprise.** A dot-matrix headline. A circular widget. A red dot. Restraint makes the one expressive moment powerful.
6. **Percussive, not fluid.** Imagine UI sounds: click not swoosh, tick not chime. Design transitions that feel mechanical and precise.

### 2.9 Visual Variety in Data-Dense Screens

When 3+ data sections appear on one screen, vary the visual form:

| Form | Best for | Weight |
|------|----------|--------|
| Hero number (display or mono, large) | Single key metric | Heavy — use once |
| Segmented progress bar | Progress toward goal | Medium |
| Concentric rings / arcs | Multiple related percentages | Medium |
| Inline compact bar | Secondary metrics in rows | Light |
| Number-only, label above | Values without proportion | Lightest |
| Sparkline | Trends over time | Medium |
| Stat row (label + value) | Simple data points | Light |

Lead section → heaviest treatment. Secondary → different form. Tertiary → lightest. The FORM varies, the VOICE stays the same.

### 2.10 Dots Must Mean Something

The dot matrix is the most recognisable thing in this language and the easiest to get wrong. It descends from a transit departure board — a surface that exists purely to convey information at a glance. Nothing's Glyph has never been decorative either: flash, timer, progress, notification, charging state. Every use does a job.

So the rule is absolute: **before drawing a dot grid, name the quantity it encodes.** If there is no answer, it is wallpaper, and wallpaper is how a copy announces itself.

Valid: progress, completion, remaining count, a genuinely two-state game grid, hero numerals in the dot-matrix display face. Invalid: texture behind content, empty-state illustration, container border, button styling.

This is the same conclusion the Glyph criticism arrives at from the other direction — a distinctive feature that does no work gets called a gimmick, fast. Give the dots a job and the criticism has nowhere to land.

### 2.11 Sound Is Absent, So Touch Carries It

Teenage Engineering products are percussive: click, not swoosh. This project ships no audio at all, and colour and motion are both heavily constrained — which leaves haptics carrying nearly all of the felt response.

That makes haptics load-bearing rather than a garnish, and it has a consequence worth stating: a "haptics off" setting is a real accessibility and preference control, not a checkbox. If it is offered, every feedback path must actually honour it. Route haptics through one gated channel rather than letting components call the platform API directly, or "off" will silently mean "mostly on".

---

## 3. ANTI-PATTERNS — WHAT TO NEVER DO

- No gradients in UI chrome
- No shadows. No blur. Flat surfaces, border separation.
- No skeleton loading screens. Use `[LOADING...]` text or segmented spinner.
- No toast popups. Use inline status text: `[SAVED]`, `[ERROR: ...]`
- No sad-face illustrations, cute mascots, or multi-paragraph empty states
- No zebra striping in tables
- No filled icons, multi-color icons, or emoji as UI
- No parallax, scroll-jacking, or gratuitous animation
- No spring/bounce easing. Use subtle ease-out only.
- No sharp corners. Every form is a pill, a circle, or a rounded rect at the card or cell radius. Buttons are always pill — radius at or above half the height, so the arc is a true semicircle and the shape reads as one continuous form rather than a rectangle with its corners taken off.
- Data visualization: differentiate with **opacity** (100%/60%/30%) or **pattern** (solid/striped/dotted). There is no colour to fall back on — that constraint is the design, not a limitation to work around.
- No decorative dot grid. A dot grid must encode a quantity. This is the clearest possible tell of a copy — see Section 2.10.

---

## 4. WORKFLOW

1. **Confirm the font roles** — check what `Fonts.kt` actually maps before assuming a face is available
2. **Ask mode** — dark or light? Neither is default.
3. **Sketch hierarchy** — identify the 3 layers before writing any code
4. **Compose** — apply craft rules (Sections 2.1–2.11)
5. **Check values** — `tokens.json` for the value, `nothing-tokens.md` for what it is for
6. **Build components** — `component-specs.md` for the exact spec of anything already specified
7. **Review** — run the four tests in `nothing-study.md` §12 before calling it done

For a *new game* rather than a new screen, start at `game-design-method.md` — it covers the translation
work (what replaces colour, what the dots encode, where red is allowed) that has to happen before any of
the above is meaningful.

---

## 5. REFERENCE FILES

This file is the craft layer and states no values. Everything else lives in `design-canon/`:

- **`tokens.json`** — **the** source. Every design value, machine-readable, generates the theme package
- **`nothing-tokens.md`** — the same system in prose: what each token is for and why
- **`rationale.md`** — the derivation behind every value, and the honest limits of each enforcement layer
- **`component-specs.md`** — exact specs for every component and every screen
- **`rules.md`** — every rule, flat and numbered, with its authority. The pre-merge checklist
- **`nothing-study.md`** — why the rules exist, the research behind them, and the four review tests
- **`game-design-method.md`** — how to translate a new game into this language
- **`architecture.md`** — where every file goes, and the ownership ladder for new ones
- **`resource-map.md`** — font and resource provenance, and the licensing warnings
- **`dependency-denylist.md`** — barred coordinates, and what is permitted despite looking heavy

Three files named here in an earlier revision — `references/tokens.md`, `references/components.md`, `references/platform-mapping.md` — never existed in this repository. Any agent following those pointers found nothing and fell back on its own defaults, which is one way the status colours and the decorative dot grid got in.
