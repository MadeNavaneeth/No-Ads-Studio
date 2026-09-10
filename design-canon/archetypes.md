---
status: canonical
---

# Archetypes — the mechanic families games inherit from

A game #16–100 does not answer the eight translation questions from scratch. It declares an
**archetype** — a mechanic family already demonstrated in working code — and inherits that
archetype's settled contracts: its generator shape, its restore validation, its undo semantics,
its red budget, its input verb. Only the puzzle's specific rules are new. This is the layer
between `game-design-method.md` (how a game is translated) and `game-scaffold.md` (what the
code looks like), and it exists so aesthetic and quality drift across 100 apps is structurally
hard rather than a matter of vigilance.

---

## What an archetype is

An archetype is a mechanic family whose blueprint game has shipped, in working code, every
contract the studio refuses to re-litigate per game:

1. **Seeded generator with a validity proof** — the sudoku admission bar (roadmap §3):
   uniqueness proven by solution counter, deduction-carving, or construction — never by hope.
2. **A `GameDefinition`** registered in the project's `GameRegistry`, admitted in the roadmap.
3. **Restore validation** — a corrupted save means "start fresh", never a crash or an
   unwinnable board (D7 lineage).
4. **Undo semantics** under the D34 parity ruling, with the D17 bound.
5. **Stats via `GameResult`** (D26) and **daily mode** (D32) — a one-board-per-UTC-day seed
   contract, or a documented reason the archetype has neither.
6. **A red budget** — usually zero, named in one sentence (D2, method §6).
7. **The four review tests** (`nothing-study.md` §12) passed on paper before code.

The blueprint game's `intent.md` (in its project directory) is the archetype's living
specification: grid rules, generation contract, win/lose, readouts, input verbs, persistence
keys, and the tests that hold it all provable.

---

## The registry

| # | Archetype | Mechanic family | Blueprint | Status |
|---|---|---|---|---|
| 1 | Deduction-fill | single-solution logic grid, number pad + notes | `sudoku(9)` | implemented (pre-launch) |
| 2 | Clue-paint | derived clues, drag-to-paint, line solving | `nonogram(10)` | implemented (pre-launch) |
| 3 | Global-info field risk | hidden layout, first-interaction-safe placement | `minesweeper(10)` | implemented (pre-launch) |
| 4 | Path-walk | draw between anchors, retractable, coverage counted | `connect(7)` | implemented (pre-launch) |
| 5 | Text-grid seek | bundled word pool, drag-a-line selection | `wordsearch(12)` | implemented (pre-launch) |
| 6 | Placement endurance | score-not-win, placeable-deal contract, tray | `blockpuzzle(8)` | implemented (pre-launch) |
| 7 | Light-placement | state cycles, walls that carry data, uniqueness counter | `akari(10)` | implemented (pre-launch) |
| 8 | Binary constraint grid | two-state grid, deduction-carved generator | `binairo(10)` | implemented (pre-launch) |
| 9 | Sum-constraint grid | cross-sum clues over a fill grid | `kakuro` | backlog — blueprint needed |
| 10 | Elimination-paint | blacken duplicates until every line is unique | `hitori` | backlog — blueprint needed |

Archetypes 1–8 are proven: their blueprint passes `./gradlew check` with the full gate.
Archetypes 9–10 name families the roadmap has already admitted to the backlog; they become
archetypes when their first game ships and demonstrates the charter above.

---

## How a new game inherits

1. Read the blueprint's `intent.md` — it is the contract you inherit.
2. Run `game-design-method.md` for the new puzzle; where an answer already exists for the
   archetype (input verb, red budget, dots), inherit it and say so.
3. Copy `game-scaffold.md`; fill the archetype's generator shape with the new rules.
4. Port the blueprint's test *categories*, not its tests: rules, generator + validity proof,
   restore, daily, transitions/history, codec.
5. Record the game in the roadmap §4 with its archetype named; write its own `intent.md`.

A game that departs from its archetype's contract (a different red budget, a new input verb,
no undo) does not silently diverge: the departure is a decision in `ideas/decisions.md`, and
the intent file records the override.

---

## How a new archetype is added

Only when a candidate genuinely fits no family. Adding one is a decision (recorded in
`ideas/decisions.md`) that names the family, its generator-and-proof shape, its input verb,
and the blueprint game that will demonstrate it — before that game's code is written. An
archetype with no blueprint is a backlog row, not an archetype.
