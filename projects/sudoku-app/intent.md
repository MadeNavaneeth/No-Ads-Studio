---
status: implemented (pre-launch)
---

# intent — sudoku(9)

Tier 1 · archetype: deduction-fill (`design-canon/archetypes.md` #1) · ships standalone as `sudoku`

## Mechanic
Fill the grid so every row, column, and 3×3 box holds 1–9 exactly once. The pure deduction fill.

## Grid rules
81 cells as 9×9, nine 3×3 boxes. Each cell is a given (refuses input) or player-entered;
pencil marks ride on unconfirmed cells, with optional auto-clean when a digit is placed.

## Generation
Vendored QQWing (GPL-3.0), seeded, per difficulty. Attempts are bounded (8 tries plus a
wall-clock timeout) with a silent `MODERATE` fallback. Wall time on real hardware is unmeasured.

## Win / lose
Win: every row, column, and box holds 1–9 once — one predicate. Lose: only under the optional
mistake-limit setting; otherwise unloseable.

## Readouts
`TIME` · `DONE` (player-filled ÷ cells empty at start — givens excluded) · `MISS`.
Peer highlight per the two-layer rule (D30 §5A), gated by a setting.

## Input verbs
Tap to select → number pad (nine circular keys, exhausted digits dimmed); `ERASE`, `NOTES`,
`HINT` as words, never icons. The hint engine deduces a naked single and never holds the solution.

## Undo & aid
Bounded undo (D17, depth 50), D34 parity. Red: one player entry duplicating a peer — count 0
in normal play.

## Persistence
`SessionStore` key `sudoku`; session slot and daily slot (D32, seed = UTC day), restored with
validation; stats via `GameResult` (D26).

## Theme overrides
None.

## Tests
`CellStateTest`, `ConflictsTest`, `SudokuHintTest`, `DifficultyPickerTransitionTest`,
`SudokuRestoreTest`, `SudokuCodecTest`.
