---
status: canonical
---

# Builder Kit — how small agents build games without burning tokens

**Read this first, read little else.** This file is the harness for builders.
It tells you exactly which files to open per task and which to never open.
The full canon is ~5,000 lines; a game task needs at most ~400.

## 1. Token budget (hard)

- Max **6 file reads** per game task. If you need a 7th, you are reading prose — stop.
- Max **1 screenshot** per screen built, of the smallest meaningful frame, after it compiles.
- Never `find`/`grep` the whole tree. Use `Glob` on the one directory you need.
- Never re-read a file you already read this session. Never read a file "just in case".
- Prefer `./gradlew conformanceCheck -Pconformance.fast=true` (seconds) over full builds while iterating.

## 2. Read allowlist — task → files, nothing else

| Task | Read these, in order, then start | Do not open |
|---|---|---|
| New game | This file → `ideas/market-parity.md` (your game's rows only) → `design-canon/game-design-method.md` (fill the 8 answers, §3) → `design-canon/game-scaffold.md` (copy it) | `rules.md`, `nothing-study.md`, `rationale.md` |
| Screen for existing game | `design-canon/component-specs.md` (your screen's section only) + `design-canon/tokens.json` (the tokens it names) | Everything else in `design-canon/` |
| New/changed component | `design-canon/component-specs.md` → `design-canon/nothing-design-system.md` §2, then add the spec heading *and* the function or `R5` fails | `ideas/` entirely |
| Changed design value | `design-canon/rationale.md` (that value's section) → `design-canon/tokens.json` | Generated code under `build/` |
| Bug fix | The failing file only | Canon, roadmap, studies |
| Why does a rule exist | `design-canon/nothing-study.md` (that rule's section) | Code |

Always-on context you already have and must not re-read:
`.kiro/steering/nothing-games.md` (the never-list), `AGENTS.md` (the router).

## 3. The 8-answer packet — fill before writing code

Copy this into your task output. One line each. No code until all eight have answers.
Source: `design-canon/game-design-method.md`.

1. **Name:** `name(n)` — parenthetical lowercase, number = defining dimension (D4).
2. **One quantity:** player-supplied ÷ player-asked, fresh board reads 0% (drives the readout).
3. **Three readouts:** effort + progress + cost, all-caps mono ≤4 chars (`TIME`/`DONE`/`MISS` pattern).
4. **Colour replacement:** ladder rung used (numeral → fill → surface → border → word → dots → position). If none works, reject the game — record in roadmap §6.
5. **Dots' job:** the quantity the dot matrix encodes. No answer = no dot grid.
6. **Red budget:** the single condition earning red, else zero. Red is outline/fill only, never text, always with a non-colour indicator.
7. **Input verb:** reuse in order — tap → long-press → drag-to-paint → path rendering. New verbs need a rule check first.
8. **Paper review:** squint, grayscale, gimmick, detail (`nothing-study.md` §12) on the sketch.

## 4. Market parity — offline only

Your game must implement every `MUST` row for it in `ideas/market-parity.md`
and none of the `REFUSE` rows. Rule of thumb: **everything the market leader
has, except anything needing network, server, account, ad, or calendar —
those ship as their offline substitute or not at all.**

## 5. Build loop (from `projects/<game>-app/`)

```bash
./gradlew :conformance:conformanceCheck -Pconformance.fast=true   # inner loop, seconds
./gradlew check                                        # the gate + this project's tests
scripts/run-on-emu.sh <game>                           # device proof when UI changed
```

- Every conformance failure prints a `FIX:` line — do what it says, re-run, do not open the authority doc.
- Changed a shared library (`design-system`, `shell`)? Check every consumer:
  `for p in projects/*-app; do (cd "$p" && ./gradlew check); done`.
- Order matters on a clean tree: `assembleDebug` before `check` (R4 reads the merged manifest).

## 6. Done gate

1. Packet (§3) filled, 8/8.
2. Parity MUSTs implemented, REFUSEs absent.
3. `assembleDebug check` green, 0 violations, tests pass.
4. Both flavours reinstalled and verified when UI changed.
5. One game `in_progress` at a time (roadmap §5) — ship one, then start the next.
