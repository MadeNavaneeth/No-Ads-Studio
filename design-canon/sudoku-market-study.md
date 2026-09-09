---
status: canonical
---

# Sudoku.com — market-leader study

What the most-played Sudoku app does, why it works, what we already do, where we
deliberately refuse, and what is worth adopting inside our rules.

**Method and honesty.** Grounded in two observed playing sessions (device screenshots,
2026-09-03), the app's own store copy, and Easybrain's public materials. Observed
mechanics are marked *observed*; anything the store describes but the sessions did not
show is marked *reported*. Where a claim is my reading rather than a fact, it says so.
§5A was **adopted 2026-09-03** (D30 amended); §5B–D remain a ledger of refusals.

---

## 1. The sessions — what is actually going on

The two screenshots are two moments in the same interaction language, not two features.

**Session one** (a puzzle mid-solve, a digit in hand): the digit `9` appears four times
on the board, and all four carry the *same stronger light-blue fill*. Around them, one
whole column carries a much fainter tint. Reading: the player is working the digit 9,
and the board answers twice at once —

- the faint layer marks the *region* the selection implicates (here, the column; the
  row and box tints are lighter still or cropped by the capture), and
- the strong layer marks *every cell that already holds 9*.

**Session two** (a different puzzle, an empty cell under the finger): one cell in the
centre block is the brightest blue — the cursor itself — and its entire **row and
column** stay softly tinted, with the 3×3 box implied by the capture. Reading: the
selection is an empty cell, so there is no digit to light; the board shows only the
constraint the player is about to type into.

**The mechanism, stated once.** Tapping or arming a digit does two independent things:
it *always* lights the row/column/box of the active cell (the **cross**), and it
*additionally* lights the sibling cells of whatever digit is active — the digit you
tapped, or the digit you chose on the pad. Both layers are tints of one soft blue, so
the board never reads as "two different kinds of error"; it reads as "one working
context, two strengths of attention". Red does not appear anywhere in either session —
these are assistance tints, not alarms.

**Uncertainty, recorded.** The captures are partial: exact box-tint boundaries, the
padding state, and whether the strong layer came from a cell tap or a pad press cannot
be fully separated from the stills alone. The cross + digit-sibling reading is
corroborated by the store copy (*reported*: "Highlighting of a row, column, and box"
and same-number highlighting are both advertised features), so the mechanism is sound
even if this still's exact composition is not.

## 2. Why it leads — the honest read

Easybrain's own claim is that Sudoku.com is
[the most popular Sudoku game on both stores](https://www.facebook.com/EasybrainTeam/posts/we-tell-a-lot-about-our-current-achievements-but-lets-get-back-to-2017-and-learn/627771587987143/)
by daily use. The retention machinery — daily puzzles, streaks, hints, auto-check,
advertising — is the standard explanation, and it is real but *reported*, and it is
exactly the part we refuse (D26, D27). The part worth studying is underneath it: the
**base interaction is almost friction-free**, and it teaches the rules by showing them.

Tap a cell and the board immediately draws the constraint that cell lives under. That is
the cheapest possible exposure of the mechanism — the same instinct `nothing-study.md`
§1 names as Nothing's core idea, demystification. The player never reads a rule; they
see the row, column and box light up, and the repeated digits answer "where does this
one go?". The aid colour is one soft blue spent on *assistance*, and error red appears
only when a wrong duplicate actually exists. That discipline — zero red in normal play —
is one we share (D2, Requirement 14), arrived at independently.

The design lesson is therefore not "add blue tints". It is: **make the single tap answer
the whole constraint, in two legible layers, without spending the error colour.**

## 3. What we already do — mapping

| Mechanism (leader) | In `sudoku(9)` today | Verdict |
|---|---|---|
| Tap an empty cell → row/column/box tint | Empty tap lights its twenty peers (D30) | **Equivalent** — ours is exactly the cross |
| Tap (or arm) a digit → sibling cells light | Filled-cell tap lights sibling digits (D30) | **Equivalent** in isolation |
| Both layers on one tap | D30 splits them per tap — one or the other | **The gap** (see §5A) |
| Duplicates flagged when they exist | Single red cell + muted outlines while unresolved (D2, R14 c10) | Equivalent and stricter |
| No red in normal play | Same (D2) | Aligned |
| Given vs entry told apart | `textPrimary` vs `textDisplay` weight | Aligned, ours stronger |
| Pencil notes | 3×3 paper-positioned marks | Different, ours deliberate (D13-adjacent) |
| Undo | Bounded in-memory undo (D17) | Covered |
| Mistakes handling | MISTAKES setting, `MISS` semantics (D20) | Covered |

## 4. Refusals — already decided, recorded as a ledger

These come up every time someone studies a market leader. They are decided; nobody
needs to re-argue them:

- **A soft blue assistance colour.** Our colour budget is strict Nothing: zero accents
  normally, single red on a real conflict only (D2, Requirement 14). The TE accent pair
  (sage/amber) is the optional, user-chosen allowance (C8–C11). There is no assistance
  hue, and there will not be one.
- **The continuous line grid** — thin inner hairlines, thick 3×3 rules, flat white
  field. Requirement 11 criterion 8 forbids divider lines; the 3×3 structure is spacing
  rhythm. The owner chose the bare 81 squares on 2026-09-03 over a panel or frame.
- **Hints and auto-solve.** The app deliberately never holds the solution (D26); a hint
  would require it and a code path that could leak it.
- **Daily challenge and streaks — the refusal, and its one exception.** The
  *shared-schedule* daily needs a seed schedule — a network feature wearing a disguise
  (D26) — and streaks need a calendar and a definition of "day". Both stay refused.
  The **local daily is adopted instead (2026-09-03, decision D31)**: one deterministic
  board per UTC day, date-seeded on the device, no network, no streaks, no time-zone
  calendar, in its own session slot. It is the exception that proves the refusal —
  every part of the leader's retention machinery that needed a server or a calendar is
  still out.

## 5. Adoption candidates — inside our rules

### A. Unify the tap: cross *and* sibling digits, composed — **ADOPTED 2026-09-03**

**The change (adopted).** D30 chose per-tap: an empty tap lights the cross, a filled tap lights
the digit's siblings. The leader composes instead — the cross is the constant base
layer, and the digit layer joins it whenever a digit is in hand. Concretely, the
amendment: **a filled-cell tap lights its cross too** (the cross already covers the
empty-tap case, so one rule covers all taps: *light the tapped cell's row, column and
box; additionally light the sibling digits when the tapped cell is filled*).

Why this survives the D30 argument rather than reopening it: D30's table said each
feature alone fails one case. Composing them does not pick a case — it answers both.
The leader's one interaction is the evidence that the two layers do not fight.

**The constraint it hit.** Before the amendment both layers shared one visual: the single
`related` flag raised the fill to `surfaceRaised` and took a hairline outline. Two
simultaneous layers need two legible strengths, and D30 already records why there is no
fourth surface level. The mapping is a decision, not an assumption — decided below.**Mapping (decided 2026-09-03; revised the same day).** Two grey fills cannot carry two
strengths — a `surfaceRaised`-only wash measured ~1.1:1 against `surface` and was invisible on
OLED black, which is exactly why the leader's faint tint has to be blue. So the strengths split
the tokens: the sibling digit set is the **bright raised block** (`surfaceRaised` fill plus its
`borderSubtle` hairline, or the TE accent when the player chose one), and the cross is a **soft
ring** — base `surface` fill unchanged, strength carried by the 1dp `borderSubtle` hairline
alone. The selection keeps its existing `borderVisible` outline, and conflict red stays supreme
(a conflicted cell is whole-fill red, R14). A sibling inside the cross is simply both; no new
token. **Owner confirmed 2026-09-03: neutral rings are the shipped cross language**; the
accent-wash alternative was declined on review of the leader's actual frame.

**What it cost.** `GridCell`'s single `related: Boolean` became two flags (`cross` and
`sibling`); the spec tables in `component-specs.md`, the D30 amendment and the
`RelatedCellsTest` suite were rewritten; the `showPeers` setting gates both layers, and
the mass highlight snaps, so S16 holds.

### B. Pad-digit preview (arm a digit before tapping) — considered, rejected

In the leader, arming the digit on the pad lights its instances before any cell tap. Our
input model has no "arming" — a pad press *writes* into the selected cell (and there is
no selection on a fresh board). Adding a preview would mean either a new interaction
mode or a long-press semantics the pad does not have. The same discoverability already
comes free from the filled-cell tap. **Not adopted**; recorded so the idea has a home.

### C. Optional duplicate tint for unlimited-mistake play — already equivalent

The leader's "highlight duplicates" is reported as a setting. Ours flags conflicts the
moment they exist — the single red cell plus muted outlines persist until the duplicate
is resolved, in both mistake modes. There is no gap to close.

### D. Board anatomy — recorded refusal, revisitable only by the owner

The line-weighted box grid is the leader's most visually distinct choice and the one we
most deliberately refuse (§4). The bare 81 squares are decided. If, in playtesting, box
structure ever fails to read (large font scale, low-vision pass, or on a small screen),
the fallback to evaluate first is a *whisper* of box separation inside the existing
spacing rhythm — not the drawn grid.## 6. Outcome

**A is adopted** (2026-09-03): every tap lights the cross; a filled tap adds the sibling
digit layer. `GridCell` takes `cross` and `sibling` flags, D30 records the amendment, and
the `GridCell` and `sudoku(9)` specs describe the composed rule. B, C and D are recorded
refusals, not scheduled.

Still open: whether the cross should persist on the *last-tapped* cell after the selection
moves (the leader keeps the cross until you leave it), whether `showPeers` should gate the
cross and sibling layers separately, and whether the sibling layer keeps its hairline when
the TE accent is on or trades it for the accent outline.

## 7. Sources

- Two observed sessions of Sudoku.com (Easybrain), device screenshots 2026-09-03 — *observed*
- [Sudoku.com product page — Easybrain](https://easybrain.com/sudoku) — *reported*: duplicate highlighting, auto-check
- [Sudoku.com on Google Play](https://play.google.com/store/apps/details?id=com.easybrain.sudoku.android&hl=en_US) — *reported*: row/column/box highlighting, duplicate highlighting
- [Easybrain: "the most popular sudoku game on both the App Store and Google Play"](https://www.facebook.com/EasybrainTeam/posts/we-tell-a-lot-about-our-current-achievements-but-lets-get-back-to-2017-and-learn/627771587987143/) — *reported, their own claim*
