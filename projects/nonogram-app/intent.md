---
status: implemented (pre-launch)
---

# intent — nonogram(10)

Tier 2 · archetype: clue-paint (`design-canon/archetypes.md` #2) · ships standalone as `nonogram`

## Mechanic
Fill cells to reveal the picture the row and column clues describe. The solution is monochrome
pixel art — the game *is* the dot matrix.

## Grid rules
10×10 tri-state cells: filled, marked-empty (a centred dot), unknown — plus clue strips per row
and column. Solved lines dim.

## Generation
Seeded solution bitmap, clues derived from it; mirrored halves for picture-logic density; a
board is accepted only when deduction-solvable.

## Win / lose
Win: the filled set matches the solution exactly. No lose state — a wrong fill is found by
contradiction, never announced.

## Readouts
`TIME` · `DONE` (correctly filled ÷ filled cells in the solution) · `LINE` (rows + columns
solved). No background dot readout — the grid is the matrix.

## Input verbs
Tap to fill, long-press to mark empty, drag to paint a run. Nonogram introduced drag-to-paint;
connect and wordsearch inherit it.

## Undo & aid
Bounded undo, D34 parity. Red: **none, ever** — the archetype's defining zero.

## Persistence
`SessionStore` key `nonogram`; session and daily slots (D32); stats via `GameResult`.

## Theme overrides
None.

## Tests
`NonogramRulesTest`, `NonogramGeneratorTest`, `NonogramRestoreTest`, `NonogramDailyTest`,
`NonogramTransitionTest`, `NonogramCodecTest`.
