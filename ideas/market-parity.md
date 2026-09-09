---
status: canonical
---

# Market Parity — everything the leaders have, offline only

Principle: **a player switching from the market leader must miss nothing
except the network.** Every mechanic, aid, difficulty, stat, and convenience
the leaders ship is MUST — unless it needs network, server, account, ad,
calendar, or store connection. Those are REFUSE, and each names its offline
substitute or ships as nothing at all. See `ideas/games-roadmap.md` §3
(admission criteria) and `design-canon/sudoku-market-study.md` (the worked
example: adopt §5A, refuse §5B–D).

Builders: implement every MUST row for your game. Do not implement REFUSE rows.
Status words: **MUST** (ship it), **REFUSE** (decided, do not re-argue),
**OPTIONAL** (owner decides, default no).

## 1. Global MUST — every game ships all of these

| # | Feature | Offline implementation |
|---|---|---|
| G1 | New game + continue after process death | DataStore restore, validated codec, corrupt-state starts clean |
| G2 | Difficulty select (≥3 levels) | Local generator per level, default level ≤2s slowest-of-10 (roadmap §3) |
| G3 | Timer as instrument readout + show/hide setting | Lifecycle-aware, mono tabular, persists with session |
| G4 | Undo (bounded, in-memory) | Same-session undo stack, no persistence needed |
| G5 | Mistake handling with limit setting | Count + optional limit; conflict shown the moment it exists |
| G6 | Peer/context highlight | Cross + sibling pattern per `sudoku-market-study.md` §5A, gated by setting |
| G7 | Remaining-counts readout | Per-option counts, honoured by setting |
| G8 | Per-game per-difficulty stats (played, won, best time) | Local stats store consuming `GameResult`, no calendar/streaks |
| G9 | Settings honoured (theme, haptics, timer, peer highlight, mistakes) | All persist and apply; haptics gated through one channel |
| G10 | First-run teaching without a tutorial page | Board answers the first tap (constraint lights up); no coach marks |
| G11 | Rotation + large-font-scale safe | Hand-verified; no instrumented tests exist, so this is a review duty |
| G12 | Zero red in normal play | Red only on real error/destructive/urgent, one element, non-colour indicator alongside |

## 2. Per-game MUST

### `sudoku(9)` — leader: Sudoku.com (Easybrain). Authority: `design-canon/sudoku-market-study.md`.

| # | Feature | State |
|---|---|---|
| S1 | Cross + sibling highlight composed on every tap | Adopted §5A — `cross` and `sibling` flags, `showPeers` gates both |
| S2 | Pencil notes (paper-positioned 3×3) | Shipped |
| S3 | Erase, undo, mistakes limit, remaining counts | Shipped |
| S4 | Local daily board (one deterministic UTC-day seed, own slot) | Adopted D31 — the one exception to the daily refusal |
| S5 | Difficulty levels via QQWing generator | Shipped (Simple→Challenge, bounded tries + MODERATE fallback) |

### `nonogram(10)` — leaders: Picross/Nonogram.com category

| # | Feature | Offline implementation |
|---|---|---|
| N1 | 10×10 tri-state (filled / marked-empty / unknown) + clue strips | Seeded bitmap, clues derived locally |
| N2 | Tap fill, long-press mark-empty, **drag-to-paint runs** | Introduces drag primitive for tier 3 reuse |
| N3 | Line-solved feedback (rows + columns solved count) | `LINE` cost readout, no error red — contradiction is found, not announced |
| N4 | Lives/mistake mode optional | Local counter only |

### `minesweeper(10)` — leaders: classic Minesweeper apps

| # | Feature | Offline implementation |
|---|---|---|
| M1 | First-tap-safe guarantee + flood-fill on zeros | Generator constraint, local |
| M2 | Tap reveal, long-press flag | Reuses Nonogram long-press |
| M3 | Adjacent counts as numerals at `textPrimary`, no hue | Colour-replacement ladder rung 1 (method §4) |
| M4 | Flag as filled dot; detonated mine is the red home | Only terminal red in tier 2 |
| M5 | Mines-remaining readout | `MINE` cost readout |

### `connect(7)` — leaders: Flow Free / Numberlink apps

| # | Feature | Offline implementation |
|---|---|---|
| C1 | Drag endpoint-to-endpoint path laying, every cell used to win | Reuses Nonogram drag + new path rendering |
| C2 | Endpoints as **numerals, not colours** | The whole monochrome adaptation (method §4) |
| C3 | Crossed/invalid path simply not accepted | No red — refusal is structural |
| C4 | Pairs-joined readout | `PAIR` cost readout |

### `blockpuzzle(8)` — leaders: Block Blast / Blockudoku / 1010! category

Written after shipping, closing a §4 gap — the genre table below was back-filled
from general genre knowledge, so claims are marked **reported** (not from observed
sessions of the leaders). An observed-session pass is owed before this table is
treated as settled; the shipped game already exceeds it on the load-bearing rows.

| # | Feature | State |
|---|---|---|
| B1 | Endless run; score is the record; game over by accumulation | Shipped — run ends when no remaining piece fits, checked after every placement |
| B2 | Drag placement; a piece that does not fit refuses without penalty | Shipped — D33 drag geometry, refusal costs nothing, no red |
| B3 | Row and column clears resolved together per placement | Shipped |
| B4 | Deal never dead: every dealt set admits ≥1 placement | Shipped — beyond parity: the D33 placeable-deal contract, tested |
| B5 | Undo (G4) | Shipped — `BlockHistory`, bounded at 50 (D17), terminal never recorded |
| B6 | Unplayable-tray signal before the failed drop | Shipped — `anyMoveAvailable` pre-check dims the tray |
| B7 | Clear-streak/combo scoring (reported: leader bonus ladders) | **Optional** — offline-capable, but adds a scoring rule the 8-answer packet never derived; owner decides |
| B8 | Rotation-safe board + readouts (G11) | Hand-verified review duty |

### `akari(10)` — leaders: Lightwood / Akari apps (Nikoli category)

Written before code, per the §4 standing rule — from genre knowledge of the Nikoli
classic and its app implementations, marked **reported** (no observed sessions
yet; the owed pass applies here as it does to blockpuzzle).

| # | Feature | State |
|---|---|---|
| A1 | 10×10 board; walls with numbers; light every white cell | Planned |
| A2 | Tap cycles empty → bulb → empty | Planned — reuses sudoku's tap; **no new input primitive** |
| A3 | Bulbs never see each other; numbered walls satisfied exactly | Planned |
| A4 | Generated puzzles have exactly one solution | Planned — bounded solver, uniqueness is the admission bar (sudoku's QQWing analogue) |
| A5 | Illegal placement (clash / number exceed) simply refused | Planned — connect's C3 idiom: refusal is structural, no red exists in this game at all |
| A6 | Undo (G4) | Planned — the D34 snapshot shape, bounded at 50 |
| A7 | Hint/auto-solve | **REFUSE** (global row) — the app never holds the solution (D26); win is checked locally |
| A8 | Rotation-safe board + readouts (G11) | Review duty |

### `wordsearch(12)` — leaders: Word Search category

| # | Feature | Offline implementation |
|---|---|---|
| W1 | 12×12 grid, ~10 placed words, 8 directions | Local placer + filler |
| W2 | Drag first-to-last letter, matched line locks | Reuses drag primitive |
| W3 | Found word = pill outline at `borderVisible`, letters to `textDisplay` | No per-word hue (method §4) |
| W4 | Words-remaining readout + word list with strike-through state | `LEFT` cost readout |
| W5 | ~20KB curated word list (~2,000 words) | First bundled data; proves the 512KB budget |

## 3. Global REFUSE — decided, with offline substitute or nothing

| Refused | Why | Ships instead |
|---|---|---|
| Hints / auto-solve / solution reveal | App never holds the solution (D26); leaks by construction | Nothing — contradiction/conflict display is the aid |
| Shared-schedule daily, streaks, calendars | Needs seed schedule + day definition = network wearing a disguise | Local daily only (D31): date-seeded, own slot, no streaks |
| Leaderboards, accounts, cloud sync, social | Network + identity | Local per-game per-difficulty stats (G8) |
| Ads, analytics, crash reporting, any permission | Product promise; zero `uses-permission` | Nothing |
| Colour-carried mechanics (Sort-style matching, per-path hues) | Fails monochrome admission | Numeral/fill/surface ladder (method §4) or rejection |
| Sliding-tile motion, spring/bounce/scale animation | Rule S13 + motion rules | 150/300ms opacity + colour fades only |

## 4. Standing rule for new games

Before building a tier game with no MUST table here, write its market study
next to `design-canon/sudoku-market-study.md`: two observed sessions minimum,
mark each claim *observed* vs *reported*, map leader mechanics to ours, record
adoptions and refusals with decision ids. Small agents do not do this research —
the top model writes the table above first, then builders implement it.
