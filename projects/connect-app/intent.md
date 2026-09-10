---
status: implemented (pre-launch)
---

# intent — connect(7)

Tier 3 · archetype: path-walk (`design-canon/archetypes.md` #4) · ships standalone as `connect`

## Mechanic
Join every numbered endpoint pair with a path — and use every cell doing it (numberlink with a
Hamiltonian guarantee).

## Grid rules
7×7; each cell holds an endpoint id or a path segment. The board is full at the win state, so
coverage is the puzzle, not just the pairing.

## Generation
Construction-built boards — every pair joined and every cell used by construction, no search —
from a seed; endpoints extracted from the laid paths.

## Win / lose
Win: every pair joined **and** every cell used. No lose state.

## Readouts
`TIME` · `DONE` (cells covered by a path ÷ total cells) · `PAIR` (endpoint pairs joined).

## Input verbs
Drag from endpoint to endpoint to walk a path; paths retract on backtrack. Inherited
drag-to-paint, specialised into path rendering.

## Undo & aid
Retraction is the undo — the walk itself. Red: none; a crossed path is simply not accepted.

## Persistence
`SessionStore` key `connect`; session and daily slots (D32); stats via `GameResult`.

## Theme overrides
None.

## Tests
`ConnectRulesTest`, `ConnectGeneratorTest`, `ConnectRestoreTest`, `ConnectDailyTest`,
`ConnectTransitionTest`.
