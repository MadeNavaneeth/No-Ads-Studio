# `sudoku(9)` — screen reference

Mirror of `design-canon/component-specs.md` §sudoku(9), §Settings and §How to play. That
file is the authoritative source; update this only when the canon changes.

---

### `sudoku(9)`

```
NothingTopBar    "BACK"  ·  "SUDOKU"  ·  "NEW"        ← NEW opens the difficulty picker
content inset space16 horizontally, space32 vertical, scrollable on short screens
  readout row      three items, evenly spaced         ← decision D13
    "TIME"  "04:12"      label / textSecondary  +  data / textPrimary
    "DONE"  "62%"        label / textSecondary  +  data / textPrimary
    "MISS"  "1"          label / textSecondary  +  data / textPrimary
space32
  9×9 grid        square — 1:1, drawn directly on the background. Just the 81
                  cells: boxes space8 apart, cells space4 within one — grouping is
                  spacing rhythm, never lines or a panel
space32
  NumberPad        5 + 4 keys on a phone (keys never shrink below space48)
    action row     UNDO · ERASE · NOTES
space24
```

`DotMatrixReadout` sits behind everything, `progress` bound to completion, wide pitch.

Readout semantics, per decision D20: `TIME` is accumulated play time — it reads `—` while the
timer setting hides it, because the clock runs either way; `DONE` is
`filledPlayerCells / initiallyEmptyCells` as a percentage, so a fresh puzzle reads 0%; `MISS` counts
values committed into an empty cell that conflicted at the moment of entry, once — correcting the cell
does not decrement it. With the MISTAKES setting on it reads `n/3`.

**States.**

| State | Presentation |
|---|---|
| Generating | `Label` reading `GENERATING` with a `DotPulse` beneath, centred. No spinner. |
| Generation failed | `Label` reading `GENERATION FAILED` plus a `RETRY` button. Back stays available, persisted state untouched. Generation is bounded (8 attempts plus a wall-clock timeout); a difficulty that exhausts its attempts silently retries at `MODERATE`, so this state means a real generator failure, not a budget one. |
| Choosing difficulty | the `DifficultyPicker` replaces the whole content area below the top bar — readouts, board and pad hidden. Cancelling returns to exactly what was there before, whatever that was. |
| Playing | the normal screen above. A conflict shows on at most one cell: the most recently entered conflicting cell fills **entirely red** — the screen's single red element — every other conflict renders muted with a non-colour outline (Requirement 14 criterion 10). |
| Complete | the content is replaced by a centred finish panel: the time at `displayMd`, a `Label` reading `SOLVED · <difficulty>` or `FAILED · <difficulty> · n/3`, a `DotPulse`, and a `NEW GAME` button that opens the picker. The completion haptic fires once, the session is cleared, and the result is emitted to the shell. A failed run exists only when the MISTAKES setting is on — otherwise mistakes count but never end the run. |

**One tap lights the whole constraint** (GridCell §cross and sibling): every tap lights the row,
column and box of the tapped cell — *what can go here?* — and a filled tap additionally lights the
other cells holding that digit — *where else is this digit?* The two layers compose (decision D30,
as amended by the market study §5A): the cross is the soft raised wash, the sibling digit set the
framed layer inside it.
Both switch off with the SAME-DIGIT & PEER HIGHLIGHT setting. `NOTES` flips the pad between placing
values and pencil marks; `ERASE` clears the selected cell (never a given); `UNDO` steps back through
recent moves — in-memory, bounded at 50, and deliberately not restored after a restart (decision D17).
A given is never editable: it rejects entry and erase, and persists unchanged across restarts.

**In normal play there is no red on this screen.** Decision D2 — the single `ConflictAccented`
cell (whole fill red) is the only exception, and the accent pair (sage/amber) is absent unless chosen in Settings.

**The day's puzzle — `DAILY`, decision D31.** The picker leads with `DAILY` ahead of the five
difficulties; it is a mode, not a level. One board per **UTC day** — the seed is the UTC date,
so the same puzzle appears on every device that day, with no network and no time-zone calendar.
It is generated at `MODERATE` but carries its own label, so the finish panel reads
`SOLVED · DAILY`, `STATS` keeps a `DAILY` row of its own, and the real difficulties' records are
never diluted by it. It plays in its own session slot (`daily.sudoku9.*`, outside the resume
scan): starting, playing or finishing it never touches an in-progress regular puzzle, and
finishing a regular puzzle never touches it. Picking `DAILY` again the same day resumes today's
unfinished board where it was left; an earlier day's stored session is discarded, and the seeded
generator would rebuild that day's board identically if asked again. Reaching the game screen
still resumes the regular session — the daily is re-entered through `DAILY`, never through a
Home resume card.

### Settings

A tab destination, like Stats: no back action in the top bar, insets top and sides only.

```
NothingTopBar    "SETTINGS"
space32
  "APPEARANCE"          label / textSecondary
space16
  "SYSTEM / DARK / LIGHT"            NothingButton row, selected = borderVisible
space32
  "FEEDBACK"            label / textSecondary
space16
  "ON / OFF"                         NothingButton pair
  "HAPTIC TICKS ON CELL ENTRY AND CONFLICT"     label / textDisabled
space32
  "REMAINING COUNTS"    label / textSecondary
space16
  "ON / OFF"                         NothingButton pair
  "A BADGE ON EACH NUMBER KEY, SHOWING HOW MANY ARE LEFT"   label / textDisabled
space32
  "TIMER"               label / textSecondary
space16
  "ON / OFF"                         NothingButton pair
  "THE CLOCK RUNS EITHER WAY"        label / textDisabled
space32
  "SAME-DIGIT & PEER HIGHLIGHT" label / textSecondary
space16
  "ON / OFF"                         NothingButton pair
  "THE BOARD LIGHTS WHAT THE SELECTION IMPLICATES" label / textDisabled
space32
  "MISTAKES"            label / textSecondary
space16
  "ON / OFF"                         NothingButton pair
  "3 MISTAKES ENDS THE RUN — THE SINGLE RED STILL MARKS THE LAST"  label / textDisabled
  "UNLIMITED — MISTAKES COUNT BUT NEVER END THE RUN"               label / textDisabled
space32
  "THIRD ACCENT — TEENAGE ENGINEERING"  label / textSecondary
space16
  "NONE / SAGE / AMBER" NothingButton row, selected = borderVisible
  "SAGE #7A9E8B — MUTED TE GREEN FOR PROGRESS/SOLVED"  label / textDisabled
  "AMBER #C2A878 — MUSTARD TE YELLOW FOR HIGHLIGHT"    label / textDisabled
space32
  "ABOUT"               label / textSecondary
space16
  "OFFLINE · NO ADS · NO TRACKING"   label / textDisabled
  "GPL-3.0 · QQWING"                 label / textDisabled
```

No display element here. Settings is not a hero screen, and giving it one would spend the single display
slot on something that does not deserve it.

**Every toggle is a pair of pills, never a switch.** A Material `Switch` carries its on-state in a filled
track, and a filled track needs a colour this palette does not have. Two pills where the selected one holds
a brighter border is the same selection language the theme options and the difficulty picker already use,
so the screen speaks one vocabulary instead of three.

**MISTAKES** is off by default (unlimited — `MISS` counts but never ends the run). On, it sets the
ceiling to three; the third conflicting entry fails the run, and the single red cell still marks the
last mistake.

**THIRD ACCENT — TE** offers `NONE` (the default), `SAGE` and `AMBER`. This is the only place an
accent colour other than red enters the system, and it is a choice, not a default: at `NONE` the game
is strict Nothing. Chosen, the accent tints generated givens at 8%, takes the selected cell's outline,
and joins the highlight and remaining-count readouts — still normally absent, at most one element,
non-colour paired (C8–C11).

All seven settings persist in the same DataStore as the game state, under a `settings.` key prefix:
`themeMode`, `hapticsEnabled`, `showRemaining`, `showTimer`, `showPeers`, `mistakeLimit`,
`accentChoice`. One store, one prefix, no second mechanism. Decision D7.

### How to play

A full-height destination, not a tab: no `NothingBottomNav` beneath it, and the top bar carries
BACK to Home. Reached from the quiet `HOW TO PLAY` link on the start page — under the fixed PLAY
button (decision D26, amended 2026-09-03).

```
NothingTopBar    "BACK"  ·  "HOW TO PLAY"
content inset space16 horizontally, space32 vertical, scrollable
space32 between groups, no dividers
  "THE PUZZLE"          label / textSecondary
  one-line sentences     body / textPrimary, space8 apart
  "GIVENS"              label / textSecondary
  "READING THE BOARD"   label / textSecondary
  "NOTES"               label / textSecondary
  "CONTROLS"            label / textSecondary
  "MISTAKES & STATS"    label / textSecondary
```

Six groups, each a caption over one-line sentences. No display element, no red, no buttons. It
teaches by pointing at behaviour the board already speaks — the cross and sibling set, the single
red conflict cell, NOTES, ERASE/UNDO, and what STATS records — so the page says nothing the player
cannot verify in one tap.
