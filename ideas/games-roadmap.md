---
status: canonical
---

# Games Roadmap

Ordered by player market, filtered to what we can build with UI alone.

---

## 1. Market context, 2026

From the [mobile puzzle genre heatmap](https://www.gamigion.com/mobile-puzzle-genres-heatmap-2026/) and
[Business of Apps](https://www.businessofapps.com/data/puzzle-games-market/). Puzzle is ~20% of all
mobile game installs and has ranked third on revenue every year since 2019.

**Growing**

| Genre | Revenue | YoY | Asset-free? |
|---|---|---|---|
| Block Puzzle | $248.5M | **+176.1%** | yes |
| Sort Puzzle | $477.6M | **+170.4%** | no — depends on colour |
| Merge | $2.21B | **+65.4%** | no — heavy art |
| Connect Puzzle | — | **+40.2%** | yes |
| **Sudoku** | — | **+29.2%** | **yes** |

**Declining** — do not build into these

| Genre | YoY |
|---|---|
| Match-3 | −2.3% (still #1 at $5.02B, but shrinking) |
| Match 3D | −14.6% |
| Bubble Shooter | −22.1% |
| Find The Difference | −23.2% |
| Brick Breaker | −32.4% |

**The convenient finding:** the two biggest revenue genres, Match-3 and Merge, are ruled out by our
no-assets constraint anyway. Everything that is both *growing* and *asset-free* is a genre we can
actually build — Block Puzzle, Connect Puzzle, and Sudoku.

---

## 2. Candidates, ranked by market × fit

Fit means: no image or audio assets, monochrome-compatible, grid or text based, generated not bundled,
and buildable with our motion rules.

| # | Game | Market signal | Fit | Verdict |
|---|---|---|---|---|
| 1 | **Sudoku** | growing +29.2%, evergreen | grid + numerals. Engine already vendored. | **Tier 1** |
| 2 | **Nonogram** | very large established category | filled/empty cells revealing monochrome pixel art — *this is literally our dot matrix* | **Tier 2** |
| 3 | **Minesweeper** | evergreen, universally known | grid + numbers. Mines give the red accent a real home. | **Tier 2** |
| 4 | **Numberlink** (Connect) | +40.2% growth | dots + drawn paths. Numbered endpoints replace colour. | **Tier 3** |
| 5 | **Word Search** | large established category | letter grid. First bundled data, ~20KB word list. | **Tier 3** |
| 6 | Block Puzzle | **+176.1%**, fastest growing | shapes on a grid, no assets | **Tier 3** (ruled D33) |
| 7 | **Akari** (Light Up) | niche, Nikoli classic | dots + numbers. *Placing lights* — a thematic bullseye for Nothing. | **Tier 2**, implemented |
| 8 | Binairo | niche | a grid of two states. Monochrome by definition. | backlog |
| 9 | Kakuro | established Nikoli | numbers only, sum constraints | backlog |
| 10 | Hitori | niche | blacken cells to remove duplicates — monochrome by design | backlog |

### Why Nonogram is second and not fifth

It was tier 2 before on reuse grounds. The market and aesthetic arguments are stronger than that:

- The category is enormous — dozens of established apps under Picross, Griddlers, Hanjie, Pic-a-Pix
- A nonogram's *solution is monochrome pixel art on a grid*. Every other game we could build wears the
  Nothing aesthetic; nonogram **is** the aesthetic. The reveal is a dot-matrix image.
- Zero bundled data — solutions are generated, clues derived from them
- It exercises drag-to-paint, which Numberlink and Word Search then reuse

### The Block Puzzle question — ruled, decision D33

Fastest-growing genre in the data and genuinely asset-free. Two rules needed a ruling before it
could enter the catalog; both are now decided (2026-09-07, decision D33 in `decisions.md`):

1. **Drag placement versus rule S13** — ruled: direct manipulation is input, not animation. A
   piece follows the pointer during an active drag; after release everything resolves by opacity
   only. Nonogram's shipped drag-to-paint was the precedent; D33 writes down the distinction.
2. **Piece distinction in monochrome** — ruled: dot density (solid / half-pitch / hollow), the
   language's own data-bearing device, not a colour workaround.

Admitted **Tier 3, status `planned`**. The admission criterion "puzzle validity" is reinterpreted
for an endless placement game: every dealt piece set must admit at least one legal placement at
deal time, so a run ends by accumulation, never by a dead deal.

---

## 3. Admission criteria

A candidate enters the catalog only by passing every criterion. Measured on the reference machine
(2-core, 8 GB MacBook Air) unless stated.

| Criterion | Threshold | How it is checked |
|---|---|---|
| Offline | no network access of any kind | zero permissions, no networking dependency |
| Permissions | zero declared, including merged from dependencies | merged manifest inspection |
| No binary assets | no raster image or audio file | file-extension scan |
| Monochrome-compatible | playable with no colour beyond the text hierarchy | design review |
| Presentation | grid or text, rendered through the component library | code review against the component list |
| Embedded data | ≤ 512 KB uncompressed, all data files and tables combined | sum contributed file sizes |
| Generation time | ≤ 2 s for the default difficulty, slowest of 10 runs | timed generation loop |
| Runtime memory | ≤ 64 MB resident over a 10 min session | profiler over a continuous session |
| Puzzle validity | ≥ 1 solution, solvable from the start state by deduction alone | solver run over generated puzzles |
| Session length | default difficulty completable in 2–10 min | playtest, recorded per game |
| Dependencies | nothing beyond the project template set | build script diff |

---

## 4. Accepted games

### Tier 1

**`sudoku(9)`** — status `implemented`

| | |
|---|---|
| Generator | vendored QQWing (GPL-3.0) |
| Grid | 81 cells as 9×9, nine 3×3 boxes, each cell flagged given or player-entered |
| Input | select cell, tap 1–9 pad; erase; long-press for pencil marks |
| Win | every row, column, and box holds 1–9 exactly once |
| Exercises | the full shared surface — number pad, grid cell, top bar, persistence, readouts |
| Target | 4–8 min at Moderate |
| Data | none, generated |

### Tier 2

**`nonogram(10)`** — status `implemented`, ships as part of `studio` and standalone as `nonogram`

| | |
|---|---|
| Generator | seeded solution bitmap, clues derived from it; mirrored halves for picture-logic density, accepted only when deduction-solvable |
| Grid | 10×10 tri-state (filled, marked-empty, unknown) plus row and column clue strips |
| Input | tap to fill, long-press to mark empty, **drag to paint a run** |
| Win | filled set matches the solution exactly |
| Exercises | reuses grid cell; introduces drag input and clue-strip typography |
| Target | 3–8 min at 10×10 |
| Data | none, generated from a seed |

**`minesweeper(10)`** — status `implemented`, ships as part of `studio` and standalone as `minesweeper`

| | |
|---|---|
| Generator | layout placed at the player's first reveal, excluding the tap and its neighbours — first-tap safety is structural, not retried; adjacency counts derived |
| Grid | 9×9, each cell holding mine, adjacent count, revealed, flagged |
| Input | tap to reveal with flood-fill on zeros, long-press to flag |
| Win | every non-mine cell revealed |
| Exercises | reuses grid cell and Nonogram's long-press; first live use of the red accent |
| Target | 2–6 min at 9×9 with 10 mines |
| Data | none, generated |

**`akari(10)`** — status `implemented`, ships as part of `studio` and standalone as `akari`

| | |
|---|---|
| Generator | seeded wall layout, greedy bulb cover as the hidden solution, clues read off it, then a bounded solution counter **proves uniqueness** — the sudoku admission bar, hand-rolled (QQWing's analogue); ambiguous boards take more numbered walls until the count collapses to one |
| Grid | 10×10, one array: ground, blank walls, numbered walls (0–4), placed bulbs |
| Input | tap cycles ground → bulb → ground; walls refuse — the refusal is structural |
| Win | every white cell lit, no two bulbs see each other, every numbered wall exact — one predicate over the board |
| Exercises | reuses grid cell and minesweeper's numeral-over-void; introduces walls that carry data |
| Target | 5–15 min at 10×10 |
| Data | none, generated |

### Tier 3

**`connect(7)`** — status `implemented`, ships as part of `studio` and standalone as `connect`

| | |
|---|---|
| Generator | seeded path layout, endpoints extracted |
| Grid | 7×7 cells, each holding an endpoint id or a path segment |
| Input | drag from endpoint to endpoint to lay a path |
| Win | every endpoint pair joined, every cell used |
| Exercises | reuses Nonogram's drag input; introduces path rendering |
| Target | 2–5 min at 7×7 |
| Data | none, generated |
| Monochrome note | endpoints carry **numerals**, not colours — this is the whole adaptation |

**`wordsearch(12)`** — status `implemented`, ships as part of `studio` and standalone as `wordsearch`

| | |
|---|---|
| Generator | places curated words into a char grid across eight directions, fills the rest |
| Grid | 12×12 chars plus the placed-word list with start and end coordinates |
| Input | drag from first letter to last; a matched line locks |
| Win | every placed word found |
| Exercises | reuses drag input; **first bundled data**, so it proves the 512 KB budget |
| Target | 3–7 min at 12×12 |
| Data | bundled curated pool (~90 words, a few KB) over a generated layout — the 512 KB budget's first real entry |

### Shared components, by reuse

| Component | Used by |
|---|---|
| Grid cell | all eight |
| Top bar, dot-matrix readout, pill button, persistence | all eight |
| Number pad | Sudoku |
| Drag input | Nonogram → Connect, Word Search, Block Puzzle |
| Long-press | Nonogram → Minesweeper |
| Path rendering | Connect |
| Numeral over a void | Minesweeper → Akari |
| Piece tray | Block Puzzle |

**`blockpuzzle(8)`** — status `implemented`, ships as part of `studio` and standalone as `blockpuzzle`

| | |
|---|---|
| Generator | piece deal with the D33 validity analogue: every dealt set admits ≥ 1 legal placement at deal time |
| Grid | 8×8 board plus a three-piece tray, each piece distinguished by dot density (solid / half-pitch / hollow) |
| Input | drag from the tray; the piece follows the finger (S13 as amended by D33); on release it lands by opacity fade, never by travel |
| Win | none — endless placement; the run ends when nothing fits and the cleared-lines score is the record |
| Exercises | reuses grid cell and Nonogram's drag; adds free (non-line-locked) placement and a piece tray |
| Target | 2–10 min per run, open-ended |
| Data | none, generated |

Each tier earns the next one something. That is the point of the ordering.

---

## 5. Tier entry conditions

**Tier 1** has no preceding tier; its condition is met because Sudoku passes admission.

**Tier 2** opens when `sudoku(9)` is `implemented` and the most recent conformance run exited clean.

**Tier 3** opens when both `nonogram(10)` and `minesweeper(10)` are `implemented` and the most recent
conformance run exited clean.

Only one game carries `in_progress` at a time. Ship one, then start the next.

---

## 6. Rejected candidates

| Candidate | Failed criterion | Measurement against threshold |
|---|---|---|
| **Crossword** | embedded data | a usable clue-and-answer corpus runs to several MB against 512 KB, and clues cannot be generated offline |
| **Solitaire** | no binary assets | 52 card faces need art or a specialised font. Also fails puzzle validity — a random deal can be unwinnable. |
| **Wordle-style** | puzzle validity | not solvable by deduction from the start state; the first guess is necessarily blind |
| **25×25 Sudoku** | generation time | QQWing at 25×25 exceeds 2 s on the reference machine by a wide margin |
| **Sort Puzzle** | monochrome-compatible | the entire mechanic is colour matching, despite +170.4% growth |
| **Match-3 / Merge** | no binary assets | the two largest revenue genres, both art-dependent. Out by constraint. |
| **2048 / sliding puzzle** | motion rules | the feel depends on tiles sliding, which rule S13 forbids |
| **Mahjong solitaire** | no binary assets | 144 tile faces |
| **Chess puzzles** | no binary assets + embedded data | piece glyphs plus a puzzle database |

Recorded so the boundary of the catalog stays legible, and so a good-looking market number does not
quietly override a constraint.
