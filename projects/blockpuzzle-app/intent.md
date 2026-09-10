---
status: implemented (pre-launch)
---

# intent — blockpuzzle(8)

Tier 3 · archetype: placement endurance (`design-canon/archetypes.md` #6) · ships standalone as `blockpuzzle` (D33)

## Mechanic
Endless placement of dealt pieces onto a grid; full lines clear; the run ends when nothing
fits. Score, never win.

## Grid rules
8×8 board plus a three-piece tray. Pieces are distinguished by **dot density** (solid /
half-pitch / hollow) — the language's own data-bearing device, ruled in D33, not a colour
workaround.

## Generation
A placeable deal: every dealt set admits at least one legal placement at deal time — the
puzzle-validity criterion reinterpreted for an endless game, so a dead deal never ends a run.

## Win / lose
No win state, by design. The run ends by accumulation; the cleared-lines score is the record.

## Readouts
`SCORE` · `TIME` · `FILL` (occupied ground share).

## Input verbs
Drag from the tray; the piece follows the finger (S13 as amended by D33 — direct manipulation
is input, not animation) and lands by opacity fade, never by travel.

## Undo & aid
Take the last placement back — the one aid the genre is allowed (D34 parity), quiet emphasis.
Red: none.

## Persistence
`SessionStore` key `blockpuzzle`; session and daily slots (D32); stats via `GameResult`.

## Theme overrides
None.

## Tests
`BlockRulesTest`, `BlockGeneratorTest`, `BlockRestoreTest`, `BlockHistoryTest`.
