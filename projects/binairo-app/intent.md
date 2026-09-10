---
status: implemented (pre-launch)
---

# intent — binairo(10)

Tier 2 · archetype: binary constraint grid (`design-canon/archetypes.md` #8) · ships standalone as `binairo` (admitted D37)

## Mechanic
Fill a two-state grid so no three consecutive cells agree in any row or column, every line
holds five of each state, and no two lines are identical (Takuzu).

## Grid rules
10×10, three states per cell: dot (one), ring (zero), undecided — plus givens that refuse
input. The two states are dot density itself: solid dot vs hollow ring. No new device needed.

## Generation
Seeded complete solution carved down by a deduction solver: every removed cell is re-derivable
by forced logic, which proves the board has exactly one solution.

## Win / lose
Win: the three line rules hold everywhere — one predicate. No lose state.

## Readouts
`TIME` · `DONE` (player-decided ÷ cells to decide) · `LINE` (lines solved).

## Input verbs
Tap cycles undecided → dot → ring → undecided; givens are structural refusals. Reuses the
tap-cycle verb and bounded undo (D34).

## Undo & aid
Bounded undo, D34 parity. Red: none.

## Persistence
`SessionStore` key `binairo`; session and daily slots (D32); stats via `GameResult`.

## Theme overrides
None.

## Tests
`BinairoRulesTest`. **Known gap:** generator, restore, daily, and transition suites do not
exist yet — the archetype charter requires them before this game may serve as the family's
blueprint for others.
