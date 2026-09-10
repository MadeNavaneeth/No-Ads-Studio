---
status: implemented (pre-launch)
---

# intent — akari(10)

Tier 2 · archetype: light-placement (`design-canon/archetypes.md` #7) · ships standalone as `akari`

## Mechanic
Place bulbs so every white cell is lit, no two bulbs see each other, and every numbered wall is
exactly satisfied. Lights are the theme and the mechanic at once.

## Grid rules
10×10, one array: ground, blank walls, numbered walls (0–4), placed bulbs. Walls carry data —
the studio's only informative voids. Lit ground raises over dark.

## Generation
Seeded wall layout, greedy bulb cover as the hidden solution, clues read off it, then a bounded
solution counter **proves uniqueness** (the sudoku admission bar, hand-rolled). The proof runs
under a deterministic node budget: an unproven board takes more numbered walls, so every attempt
terminates. Daily mode uses a fixed engine setting behind a `withTimeoutOrNull` +
`GenerationFailed` guard.

## Win / lose
Win: all lit + no clashes + clues exact — one predicate. No lose state.

## Readouts
`TIME` · `LIT` (lit share of white cells). Clash cells are surfaced on the board itself, not as
a counter.

## Input verbs
Tap cycles ground → bulb → ground; walls refuse — the refusal is structural.

## Undo & aid
Bounded one-cell-delta undo (D17/D34); restored sessions start with no history.

## Persistence
`AkariSessionStore` key `akari`, fields `cells` / `walls` / `difficulty` / `elapsedMs` /
`mistakes` / `progress`. The wall mask carries **every** wall kind and is the layout's
authority at restore — a board drifted from its mask is a puzzle that never shipped.

## Theme overrides
None.

## Tests
`AkariRulesTest`, `AkariGeneratorTest`, `AkariRestoreTest` (restore + codec, hand-built
fixture — restore never pays the generator's cost), `AkariDailyTest`, `AkariTransitionsTest`.
