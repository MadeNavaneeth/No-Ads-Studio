---
status: canonical
---

# Rationale — why every value is what it is

A design system you cannot derive is a design system you cannot defend. This document gives every
token a derivation, so any value can be regenerated from first principles and any proposed change can
be argued against something.

Rule that follows from this document: **no token may exist without a stated job that no other token
does.** Enforced mechanically — see §8.

---

## 1. Why a 4dp base grid

Not aesthetic. It is the smallest step that lands on a whole pixel at every Android density bucket.

| Bucket | Scale | 4dp | 5dp | 2dp |
|---|---|---|---|---|
| ldpi | 0.75× | 3px | 3.75px | 1.5px |
| mdpi | 1× | 4px | 5px | 2px |
| hdpi | 1.5× | 6px | 7.5px | 3px |
| xhdpi | 2× | 8px | 10px | 4px |
| xxhdpi | 3× | 12px | 15px | 6px |
| xxxhdpi | 4× | 16px | 20px | 8px |

4dp is integer everywhere. 5dp produces fractional pixels at ldpi and hdpi, which the renderer
resolves by antialiasing — a soft edge where a hard one was specified. On a monochrome interface with
1dp borders, that softness is visible.

2dp is integer at every bucket except ldpi. That is why `optical2` exists but is restricted to optical
offsets and dot diameters, never to layout.

**Consequence:** any spacing value not divisible by 4 is a defect, not a preference.

---

## 2. Why these eight spacing steps

`4, 8, 16, 24, 32, 48, 64, 96`

Structure is a doubling spine with 1.5× half-steps:

- **Spine:** 4 → 8 → 16 → 32 → 64 (each ×2)
- **Half-steps:** 24 (16×1.5), 48 (32×1.5), 96 (64×1.5)

Adjacent ratios: 2.0, 2.0, 1.5, 1.33, 1.5, 1.33, 1.5. The smallest is 1.33.

That floor matters. Two spacings must differ enough that the difference reads as *intentional* rather
than as a mistake. Below roughly 1.3× the eye cannot reliably tell whether you meant a change, so the
distinction stops communicating and just adds inconsistency.

**Why each step exists.** This is the test any new token must pass — name a job no existing token does.

| Token | Job nothing else does |
|---|---|
| `space4` | gaps between cells inside a 3×3 box |
| `space8` | gaps between the boxes themselves |
| `space16` | component internal padding, screen edge inset |
| `space24` | card padding, comfortable element separation |
| `space32` | group separation — the minimum that replaces a divider line |
| `space48` | touch target height, major section separation |
| `space64` | top bar height |
| `space96` | hero breathing room above a display element |

**Why not 12dp.** Honestly: it is not mathematically excluded, its ratios would be fine. It is excluded
because 8 and 16 already serve that range, so adding 12 increases the number of choices without adding
expressive range. Every extra token is a decision an implementer has to make correctly. Eight steps
across a 24× range is enough.

This is also why the dot-matrix pitch had to be 16 or 24, not 12 — an earlier draft specified a 12dp
pitch that no token could supply.

---

## 3. Why 48dp touch targets

1dp = 1/160 inch = 0.159mm. So 48dp = 0.3 inch = **7.62mm**.

An adult fingerpad contact patch is roughly 8–10mm. 7.62mm is therefore at the lower edge of reliable
targeting, which is why it is a floor and not a target. Anything smaller and miss rates rise sharply,
particularly one-handed on a Sudoku grid where nine keys sit adjacent.

**Consequence:** a drawn element may be smaller than 48dp, but its touch target may not. That is why
`NumberPad` keys are `space48` and why smaller controls extend their target beyond their bounds.

---

## 4. Why these radii

Radius is perceptual and must be read as a **proportion of the element**, never as an absolute.

**Pill = height / 2.** This is the only radius where the corner arc is a true semicircle, so the shape
reads as one continuous form. At any smaller radius the eye detects straight segments joining arcs, and
the shape reads as "rectangle with rounded corners" — a different, weaker idea.

**Card = 16dp.** Constrained by content, not taste: the corner arc must not intrude on content, so
radius must stay below the content padding. Card padding is `space24`, and 16 < 24. Against a card
several hundred dp wide, 16dp reads as "softened", which is the intent — a card should recede, not
announce itself.

**Cell = 8dp.** A Sudoku cell is roughly 38dp on a typical phone, so 8/38 ≈ **21%** of the side. The
perceptual bands:

| Radius as % of side | Reads as |
|---|---|
| under 10% | square with a chamfer |
| 15–25% | rounded square |
| 25–40% | squircle |
| over 40% | approaching a circle |

21% sits mid-band in "rounded square", which is what a grid cell must be — soft enough to match the
shape language, square enough that nine of them still read as a grid.

**Consequence:** if cell size changes materially, cell radius must change with it to hold ~21%. A fixed
8dp on a 60dp cell would read as nearly square.

---

## 5. Why 150ms and 300ms

Two independent constraints agree on these numbers.

**Frame alignment.** At 60fps one frame is 16.67ms. 150ms = 9 frames exactly. 300ms = 18 frames exactly.
Durations that do not land on frame boundaries get truncated unevenly, which shows up as a stutter at
the start or end of a short fade.

**Perception thresholds.**

| Duration | Perceived as |
|---|---|
| under 100ms | instantaneous — no motion is seen, the change simply happened |
| 100–200ms | immediate response — motion is seen but feels causal |
| 200–400ms | a transition — the user tracks it |
| over 400ms | waiting (the Doherty threshold) |

150ms sits inside "immediate response": fast enough to feel caused by the touch, slow enough that the
change is legible rather than a jump. 300ms is a tracked transition that stays clear of the wait
threshold.

**Why exactly 2:1.** The two durations must read as different *classes*, not as slightly different
timings. A 150/200 pair would just look inconsistent. 150/300 reads as deliberate.

**Why `cubic-bezier(0.25, 0.1, 0.25, 1)`.** This is deceleration-dominant: it starts moderately and
eases to a stop. Deceleration reads as *arriving and settling*, which is calm. Ease-in-out reads as
*travelling*. Spring and overshoot read as *playful*. The requirement is calm, so the curve is
deceleration.

---

## 6. Why the type decisions

**Why Doto only at 36sp and above.** Doto builds each glyph from a 6×10 dot matrix. At 36sp the ten
vertical dot cells get ≈3.6sp each — about 3.6px at mdpi, 7.2px at xhdpi. Below 36sp those cells fall
under roughly 3px, and the dots either merge into strokes or drop out entirely. The face stops being a
dot matrix and becomes mush. The floor is a legibility limit, not a style rule.

**Why labels are 11sp with 0.08em tracking.** All-caps removes ascenders and descenders, which are the
cues used for whole-word shape recognition. Without them, reading falls back to identifying letters
individually, and letters set tight at small sizes interfere with each other. Tracking buys back the
separation. The needed amount scales inversely with size, so 0.08em at 11sp — about 0.88sp — is the
compensation for caps at that size. Below 11sp, monospace caps fall under comfortable angular size at
a normal phone viewing distance of roughly 35cm.

**Why tracking is em and not sp.** em is proportional to font size, so one tracking figure holds across
the scale. Writing an em figure as sp produces roughly no tracking at all — `0.08.sp` is eight
hundredths of a pixel. This was a live bug in `NothingTypography.kt`, where every web em value had been
transcribed as sp, leaving labels around 11× under-tracked.

**Why tabular figures.** Proportional digits have different advance widths — `1` is narrower than `0`.
In a value that updates in place, the whole number reflows on every tick, so a timer visibly jitters.
Tabular figures give every digit an identical advance. Nothing's instrument-panel character depends
entirely on numbers holding still.

**Why the scale has a weak link.** `body` 16sp to `bodySm` 14sp is a ratio of 1.14, below the
just-noticeable difference for type size. These two are not reliably distinguishable by size, so in
practice they are separated by colour. Recorded here rather than hidden, because an implementer will
otherwise assume size alone carries that distinction.

---

## 7. Why exactly four text levels, with verified numbers

Computed WCAG 2.x contrast ratios. These are calculated, not estimated.

### Dark mode

| Token | on `bg` #000000 | on `surface` #111111 | on `surfaceRaised` #1A1A1A |
|---|---|---|---|
| `textDisplay` #FFFFFF | 21.00:1 | 18.88:1 | 17.40:1 |
| `textPrimary` #E8E8E8 | 17.14:1 | 15.41:1 | 14.20:1 |
| `textSecondary` #999999 | 7.37:1 | 6.63:1 | 6.11:1 |
| `textDisabled` #666666 | 3.66:1 | 3.29:1 | 3.03:1 |

Display, primary and secondary clear 4.5:1 on every surface. Disabled clears 3:1 on every surface,
which is the correct threshold for a level whose entire purpose is to read as inactive.

### Why not five levels

The gaps are uneven by design. Display to primary is small (18.88 → 15.41) — those two are separated by
*role*, not by easy perception. Primary to secondary is large (15.41 → 6.63), and secondary to disabled
is large (6.63 → 3.29).

A fifth level could technically sit around 10:1 and be reliably ordered. It is excluded because four
levels already cover every semantic need — hero, content, supporting, inactive — and a fifth adds an
ambiguous option to every future decision without adding meaning.

### The place where low contrast is correct

`borderVisible` #333333 on #000000 measures **1.66:1**, far below any legibility threshold. That is the
requirement, not a failure. The dot-matrix texture must sit below the threshold of reading as content,
or it competes with the Sudoku grid. Dot coverage supports the same conclusion: a 2dp dot on a 16dp
pitch covers about 1.2% of the area, and on a 24dp pitch about 0.55%. Under roughly 2% reads as
texture; over roughly 5% starts reading as a grid.

This is the one token pair where a contrast test must assert a *maximum* rather than a minimum.

---

## 8. Two defects this analysis found

Computing the numbers rather than asserting them surfaced two real problems.

### Defect 1 — light-mode `textDisabled` fails its own requirement

`#999999` on light surfaces measures 2.61:1, 2.85:1, and **2.50:1**. Requirement 8 mandates 3:1 for the
disabled level, so the light theme was unsatisfiable as specified.

**Fix: `#8A8A8A`**, the darkest value clearing 3:1 on all three light surfaces, at 3.03:1 worst case.
That also makes the two themes symmetric — dark-mode disabled is 3.03:1 at worst as well.

### Defect 2 — `accentRed` fails as text on every surface

`#D71921` measures 4.05:1 on `bg`, 3.64:1 on `surface`, 3.64:1 on `surfaceRaised`. Normal-size text
needs 4.5:1. Red text therefore fails on every surface in the system.

Raising the red to pass — `#FF4D57` reaches 5.80:1 — would break fidelity to Nothing's actual red.

**Fix: red is never text — it is the cell.** A conflicting entry fills the whole cell with `accentRed`,
and the numeral over it stays white `textDisplay`: white measures 5.19:1 on `#D71921`, above the 4.5:1
text floor, so the red cell carries legible text while the red itself is never the text. The other end
of a duplicate — a given, or an older entry — renders muted with a non-colour outline, and the red cell
is the screen's one red element (Requirement 14), which keeps the error loud and single rather than
blending into decoration.

---

## 9. The guarantee ladder

Enforcement strength, weakest to strongest. Most design systems stop at level 1 or 2 and call it
governance.

| Level | Mechanism | Catches | Leaks |
|---|---|---|---|
| **L0** | Prose in a document | nothing | everything |
| **L1** | Named tokens | picking a wrong value when a right one exists | ignoring tokens entirely |
| **L2** | Lint / literal scanning | `Color(0xFF…)`, `16.dp` in code | computed values, string-built colours, anything clever |
| **L3** | Value class types | passing a `Dp` where a `Spacing` is required — a compile error | raw APIs still reachable |
| **L4** | Module boundary | *Not reachable for Compose — see below* | — |
| **L3.5** | Build-failing static gate | forbidden types in consumer code, enforced at `check` | a suppression used dishonestly |
| **L5** | Generation from one source | canon/code drift — there is only one artifact | a wrong value in the source |
| **L6** | Derivation tests | a wrong value in the source itself | judgement |

### Why L4 is not reachable, verified

The appealing idea is that `:design-system` declares Compose with `implementation` rather than `api`, so
`Color` and `Dp` never reach the app's compile classpath and a hex literal becomes an unresolved
reference — impossible to write rather than merely forbidden.

It does not work. Inspecting the Gradle module metadata for `androidx.compose.ui:ui` shows it exposes
`ui-graphics` and `ui-unit` as **`api`** dependencies:

```
VARIANT: metadataApiElements
  api -> androidx.compose.ui:ui-geometry
  api -> androidx.compose.ui:ui-graphics     <- Color
  api -> androidx.compose.ui:ui-text
  api -> androidx.compose.ui:ui-unit         <- Dp, TextUnit
```

`Modifier` lives in the `ui` artifact, and `compose-foundation-layout` depends on `ui` as `api`. So any
Consumer_Module that writes even one `Modifier` or one `Column` unavoidably has `Color`, `Dp`, and
`TextUnit` on its classpath. There is no arrangement of standard Compose artifacts that keeps `Modifier`
while removing the other three.

**Consequence.** The achievable ceiling is **L3.5**: a static analysis rule bound to the Gradle `check`
task, so a forbidden reference fails the build rather than merely failing a test someone might skip.
That is weaker than impossibility but stronger than lint-as-advice.

**What the module split still buys**, given L4 is out of reach:

- Value classes at the boundary are a *real* compile-time gate on the design system's own API. Passing a
  raw `Dp` where a `Spacing` is expected genuinely will not compile. That part is L3 and it holds.
- API design pressure: a module boundary forces you to decide what is public.
- Build caching and parallel compilation, which matters on a 2-core machine.
- A clear place for the suppression annotations that Requirements 10 to 12 mandate.

Keeping the split is still right. Claiming it makes violations unwritable was not.

### Where the real guarantees sit

L5 is decision D6 and it is genuine — generation makes drift structurally impossible, not merely
detected. **L6 is what was missing**, and it is the level that matters most, because every level above
it assumes the token set is correct. L2 through L5 will faithfully propagate a bad value everywhere.

### L6: the token set validates itself

JVM tests, no device, fast on a 2-core machine. Each is a property of the token set, not of its use.

| Test | Assertion |
|---|---|
| Grid alignment | every spacing token % 4 == 0 |
| Scale monotonicity | spacing strictly increasing, smallest adjacent ratio ≥ 1.3 |
| Radius ceiling | every radius ≤ 16dp except `radiusPill` |
| Frame alignment | every motion duration is a whole multiple of 16.67ms ±1ms |
| Contrast minimums | every text/surface pair meets 4.5:1, or 3:1 for `textDisabled` |
| Contrast **maximum** | `borderVisible` on `bg` stays below 2:1, so texture cannot become content |
| Red is not text | `accentRed` appears in no type style; only as outline, border, or a shape fill whose numeral clears 4.5:1 (white on red: 5.19:1) |
| Dot-matrix face floor | no type style below 36sp uses Doto |
| Tabular figures | every counting type style sets `tnum` |
| No redundancy | no two tokens in a category share a value |
| **Derivation present** | every token in `tokens.json` has a non-empty `why` field |

The last two are the strongest governance in the system. **No redundancy** means a duplicate value is a
build failure, because two tokens with the same value means one has no job. **Derivation present** means
you cannot add a token without writing down why it exists — the rule at the top of this document,
enforced by the build rather than by discipline.

### What no level catches

None of this produces good design. A screen can pass every test and still be ugly, badly composed, or
unlike Nothing. That is what the four review tests in `nothing-study.md` §12 are for — squint,
grayscale, gimmick, detail. Automation guarantees consistency and correctness. Judgement is still
required for quality, and pretending otherwise is how design systems produce compliant mediocrity.

---

## 10. `tokens.json` shape

Every entry carries its derivation. Decision D6 generates `Tokens.kt` from this; the `why` field is
required by the L6 derivation test.

```json
{
  "space32": {
    "category": "spacing",
    "value": "32dp",
    "job": "group separation — the minimum that replaces a divider line",
    "why": "16×2 on the doubling spine; divisible by 4 so integer at every density bucket"
  },
  "durationMicro": {
    "category": "motion",
    "value": "150ms",
    "job": "press feedback and state changes",
    "why": "9 frames at 60fps exactly; inside the 100-200ms immediate-response band"
  },
  "textDisabled": {
    "category": "color",
    "dark": "#666666",
    "light": "#8A8A8A",
    "job": "inactive text and exhausted number-pad digits",
    "why": "darkest value clearing 3:1 on all surfaces in both modes; 3.03:1 worst case, symmetric across themes"
  }
}
```
