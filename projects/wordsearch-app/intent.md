---
status: implemented (pre-launch)
---

# intent — wordsearch(12)

Tier 3 · archetype: text-grid seek (`design-canon/archetypes.md` #5) · ships standalone as `wordsearch`

## Mechanic
Find every hidden word in a letter grid — the studio's first and only bundled data.

## Grid rules
12×12 characters plus the placed-word list, each word carrying start and end coordinates.
Found words lock.

## Generation
A curated word pool (~90 words, a few KB — the 512 KB budget's first real entry) placed into
the grid across eight directions with scored overlaps; the rest is filled. Seeded.

## Win / lose
Win: every placed word found. No lose state.

## Readouts
`TIME` · `DONE` (words found ÷ words placed) · `LEFT` (words remaining).

## Input verbs
Drag from the first letter to the last; a matched line locks, a wrong drag just does not lock.

## Undo & aid
None: a found word stays found; seeking has no take-back to simulate. The archetype's
documented D34 deviation. Red: none.

## Persistence
`SessionStore` key `wordsearch`; session and daily slots (D32); stats via `GameResult`.

## Theme overrides
None.

## Tests
`WordsearchRulesTest`, `WordsearchGeneratorTest`, `WordsearchRestoreTest`,
`WordsearchDailyTest`, `WordsearchTransitionTest`.
