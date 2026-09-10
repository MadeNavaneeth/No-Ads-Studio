---
status: implemented (pre-launch)
---

# intent — minesweeper(10)

Tier 2 · archetype: global-info field risk (`design-canon/archetypes.md` #3) · ships standalone as `minesweeper`

## Mechanic
Reveal every safe cell on a field hiding mines, reading adjacency counts as the only information.

## Grid rules
9×9, 10 mines. Each cell holds mine / adjacent count / revealed / flagged. Flood fill opens
zero-count regions.

## Generation
Layout placed at the player's **first reveal**, excluding the tap and its neighbours —
first-tap safety is structural, not retried. Adjacency counts derived.

## Win / lose
Win: every non-mine cell revealed. Lose: a detonated mine — **the studio's one live red**,
terminal, never text, always paired with a non-colour indicator.

## Readouts
`TIME` · `DONE` (safe cells revealed ÷ total safe) · `MINE` (mines not yet flagged).

## Input verbs
Tap to reveal, long-press to flag — long-press inherited from nonogram.

## Undo & aid
None: a global-information field has no take-backs; undo would falsify the risk model.
The archetype's documented D34 deviation.

## Persistence
`SessionStore` key `minesweeper`; daily seed = UTC day **+ first tap** (D32), so the daily
board does not exist until the player commits. Stats via `GameResult`.

## Theme overrides
The detonation red. Everything else none.

## Tests
`MinesweeperRulesTest`, `MinesweeperGeneratorTest`, `MinesweeperRestoreTest`,
`MinesweeperDailyTest`, `MinesweeperTransitionTest`, `MinesweeperCodecTest`.
