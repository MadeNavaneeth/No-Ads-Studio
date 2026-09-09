---
status: canonical
---

# Designing the Next Game in This Language

The canon defines the language and specifies `sudoku(9)` exactly. It does not say how to get from "we
picked Nonogram" to "here is what Nonogram looks like." This file is that method.

Run it **before writing code**, on paper. Every step produces a written answer that goes into the game's
row in `games-roadmap.md` §4. A game that cannot answer all eight either needs more design or does not
belong in the catalog — and saying so is a valid outcome, recorded in §6.

The order matters. Steps 2 through 6 are the translation work, and step 4 is where candidates die.

---

## 1. Name it

Parenthetical lowercase, following Nothing's own verbal signature — `phone(2a)`, `ear(open)`. The number
in the parentheses is the game's **defining dimension**, not a version.

`sudoku(9)` · `nonogram(10)` · `minesweeper(10)` · `connect(7)` · `wordsearch(12)`

This costs nothing and is the highest-signal-per-effort decision in the whole method. Anyone who knows the
brand reads it instantly as Nothing.

---

## 2. Find the one quantity

Every puzzle has a single number that best represents "how far along am I". Find it. It does two jobs:
it drives `DotMatrixReadout`, and it is the candidate for the screen's one display-level element.

The rule that makes this non-obvious: **exclude what the puzzle gave you.** A fresh board must read 0%
and a finished one 100%. Count only what the player supplied, over what the player was asked to supply.

| Game | The one quantity |
|---|---|
| `sudoku(9)` | player-filled cells ÷ cells empty at start |
| `nonogram(10)` | correctly filled cells ÷ filled cells in the solution |
| `minesweeper(10)` | revealed safe cells ÷ total safe cells |
| `connect(7)` | cells covered by a path ÷ total cells |
| `wordsearch(12)` | words found ÷ words placed |

Get this wrong and the number means nothing. A Sudoku that counts all filled cells starts at roughly 40%,
which is worse than showing no number at all.

---

## 3. Choose exactly three readouts

Three. Not two, not seven. This is decision D13, and it is the direct consequence of the sharpest
criticism of Nothing's whole premise: iFixit found Phone (1)'s transparency *deceptive*, because you can
see the internals but seeing them does not help you.

So "expose the mechanism" does **not** license dumping state into the UI. Nothing does not expose the
mechanism; it exposes a composed, art-directed view that reads as exposed mechanism. A debug overlay of
generator internals is not this language — it is the absence of design.

> Three readouts chosen well beat twelve dumped honestly.

The shape that works: **one effort measure, one progress measure, one cost measure.** Labels are all-caps
mono, and short — four characters keeps the three-column row balanced without truncation at large font
scales.

| Game | Effort | Progress | Cost |
|---|---|---|---|
| `sudoku(9)` | `TIME` | `DONE` | `MISS` |
| `nonogram(10)` | `TIME` | `DONE` | `LINE` (rows + columns solved) |
| `minesweeper(10)` | `TIME` | `DONE` | `MINE` (mines not yet flagged) |
| `connect(7)` | `TIME` | `DONE` | `PAIR` (endpoint pairs joined) |
| `wordsearch(12)` | `TIME` | `DONE` | `LEFT` (words remaining) |

Every value is tabular figures in the mono role. A number that shifts width as it counts is the most
visible craft failure available on a screen this sparse.

---

## 4. Replace colour — this is where games die

Most puzzle games lean on hue somewhere, and this palette has none to lend. Work down this ladder and
stop at the first rung that separates the states:

1. **Numeral** — a digit or letter is unambiguous and needs no legend
2. **Fill vs. empty** — the strongest two-state signal available
3. **Surface role** — `bg` / `surface` / `surfaceRaised`
4. **Border role** — `borderSubtle` / `borderVisible`
5. **A word** — an all-caps mono label naming the state
6. **Dot pattern or opacity** — for series that must be told apart
7. **Position or grouping** — move it rather than tint it

If a mechanic still needs hue after all seven, the game is rejected. `Sort Puzzle` was, despite +170.4%
revenue growth: the entire mechanic is colour matching, and there is no rung that saves it.

The translations for the accepted catalog:

| Game | What normally uses colour | Replacement |
|---|---|---|
| `nonogram(10)` | nothing — already two-state | filled = `surfaceRaised`, marked-empty = a centred dot, unknown = `surface`. **The solution is literally dot-matrix art.** |
| `minesweeper(10)` | adjacency counts 1–8, one hue each | numeral alone, all at `textPrimary`. The glyph already distinguishes them; the colours were always redundant. |
| `minesweeper(10)` | the flag | a filled dot at `optical2` — a dot that means "I think a mine is here", which is the motif doing real work |
| `connect(7)` | one hue per path | **numerals at the endpoints**, paths at `textPrimary`, the path being drawn at `textDisplay` |
| `wordsearch(12)` | one highlight hue per found word | a pill outline at `borderVisible` around the run; letters in two found words at `textDisplay` |

Note what happened with Minesweeper's numbers: removing colour did not cost information, it removed
duplicated information. That is the good case, and it is more common than it looks.

---

## 5. Give the dots a job

State the quantity the dot matrix encodes before drawing a single dot. No answer means no dot grid.

A decorative dot grid is the clearest possible signal that the look was copied without the idea —
Nothing's matrix descends from a transit departure board, and their Glyph has never once been ornamental.
`DotMatrixReadout` takes a `progress: Float` and nothing else, specifically so that drawing unbound dots
is not expressible.

Usually the answer is step 2's quantity. `nonogram(10)` is the special case: its *grid* is the dot matrix,
so the background readout should be dropped rather than competing with it.

---

## 6. Budget the red — usually zero

Name the single condition that earns red. Then check whether the game has one at all; several do not, and
shipping with none is the correct outcome, not an omission.

| Game | Earns red | Count on screen in normal play |
|---|---|---|
| `sudoku(9)` | a player entry that duplicates a peer | 0 |
| `nonogram(10)` | **nothing** — a wrong fill is found by contradiction, not announced | 0, always |
| `minesweeper(10)` | a detonated mine, terminal | 0 |
| `connect(7)` | **nothing** — a crossed path is simply not accepted | 0, always |
| `wordsearch(12)` | **nothing** — a wrong drag just does not lock | 0, always |

Three of five ship with no red anywhere. That is the "zero by default" rule being real rather than
rhetorical: red's rarity is the entire source of its force, and a system that finds a use for it in every
game has already spent it.

Two constraints on the one that does appear: red is **never text** (it clears 3:1 for a non-text element
but fails 4.5:1 for type, so it is outline or fill only), and it must always be paired with a non-colour
indicator so the state survives the grayscale test.

---

## 7. Pick the input verb, reusing before inventing

The roadmap sequences the tiers so each one earns the next an input primitive. Take what exists:

```
tap              sudoku(9)      → every later game
long-press       nonogram(10)   → minesweeper(10)
drag-to-paint    nonogram(10)   → connect(7), wordsearch(12)
path rendering   connect(7)     → —
```

A new primitive needs a rule check before it needs an implementation. Drag placement is the open example:
rule S13 forbids animating position, and a piece following your finger is arguably direct manipulation
rather than animation — but the rule does not say so, which is why `Block Puzzle` is un-tiered despite
being the fastest-growing genre in the data. Resolve the rule first, in `rules.md`, then build.

---

## 8. Sketch it, then run the four review tests on the sketch

Before any code. The tests are in `nothing-study.md` §12 and they cost minutes on paper versus days in
Compose:

1. **Squint** — blur it. Still identifiably ours, or a generic dark grid?
2. **Grayscale** — desaturate. Is every state still readable? Proves red is not load-bearing.
3. **Gimmick** — point at each distinctive element and name its job. No answer means delete it.
4. **Detail** — at 400%, is tracking in em, are figures tabular, is the grid honoured?

Test 1 is the one people skip and the one that matters most. Bates describes their method as designing a
phone to read "almost like a logo" — instant recognition from silhouette alone. If the blurred screenshot
collapses into "generic dark grid app", the layout has failed no matter how many tokens it used correctly.

---

## What each step is accountable to

| Step | Authority |
|---|---|
| 1 · Name | decision D4 |
| 2 · One quantity | decision D20, Requirement 12 |
| 3 · Three readouts | decision D13, `nothing-study.md` §11 |
| 4 · Replace colour | Requirement 14, `ideas/avoid.md`, roadmap admission criteria |
| 5 · Dots | Requirement 12, `nothing-study.md` §3 |
| 6 · Red budget | Requirement 14 criterion 2, decision D2 |
| 7 · Input | `rules.md` S13, roadmap §4 reuse table |
| 8 · Review | `nothing-study.md` §12 |

---

## When translation fails

Record the rejection in `games-roadmap.md` §6 with the criterion it failed and the measurement against
the threshold. Do not leave it as folklore.

The existing rejections are all honest ones, and worth reading before proposing a candidate: Crossword
fails the 512 KB data budget because clues cannot be generated offline; Solitaire needs 52 card faces;
Wordle-style is not solvable by deduction from the start state; 2048 depends on tiles sliding. Match-3 and
Merge — the two largest revenue genres in mobile — are out on the no-assets rule alone.

The convenient finding, from the roadmap's market analysis: everything that is both *growing* and
*asset-free* is something this constraint set can actually build. The constraints are not costing market.
