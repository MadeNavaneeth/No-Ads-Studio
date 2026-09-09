---
status: canonical
---

# Component Specs

Exact specifications for every component in the component library, precise enough to implement without
asking questions. Values are token names, never literals.

Required by Requirement 5 of the harness spec: every component listed with the token set it consumes.

---

## Token names

The generated `Tokens.kt` (from `tokens.json`, see decision D6) exposes exactly these.

### Colour

| Token | Dark | Light |
|---|---|---|
| `bg` | `#000000` | `#F5F5F5` |
| `surface` | `#141414` | `#FFFFFF` |
| `surfaceRaised` | `#1F1F1F` | `#F0F0F0` |
| `borderSubtle` | `#2A2A2A` | `#E8E8E8` |
| `borderVisible` | `#3A3A3A` | `#CCCCCC` |
| `textDisplay` | `#FFFFFF` | `#000000` |
| `textPrimary` | `#E8E8E8` | `#1A1A1A` |
| `textSecondary` | `#999999` | `#666666` |
| `textDisabled` | `#707070` | `#8A8A8A` |
| `accentRed` | `#D71921` | `#D71921` |
| `accentRedSubtle` | `#D71921` @ 15% | `#D71921` @ 15% |
| `accentSage` | `#7A9E8B` | `#7A9E8B` |
| `accentSageSubtle` | `#7A9E8B` @ 15% | `#7A9E8B` @ 15% |
| `accentAmber` | `#C2A878` | `#C2A878` |
| `accentAmberSubtle` | `#C2A878` @ 15% | `#C2A878` @ 15% |

The sage and amber accents are the optional third accent, chosen in Settings and normally absent — at `none`, the default, only `accentRed` exists and it stays error-only, at most one element per screen (C8–C11).

### Spacing, radius, motion

| Token | Value |
|---|---|
| `space4` … `space96` | 4, 8, 16, 24, 32, 48, 64, 96dp |
| `optical2` | 2dp — optical offsets and dot diameter only |
| `radiusPill` | 999dp |
| `radiusCard` | 16dp |
| `radiusCell` | 8dp |
| `durationMicro` | 150ms |
| `durationTransition` | 300ms |
| `easingStandard` | `cubic-bezier(0.25, 0.1, 0.25, 1)` |

### Type

Tracking in **em**. Tabular figures on `data`, `displayLg`, `displayMd`, `cellNumeral`.

| Token | Face | Size | Line | Tracking | Use |
|---|---|---|---|---|---|
| `displayLg` | Doto | 72sp | 72sp | −0.03em | hero numerals |
| `displayMd` | Doto | 48sp | 50sp | −0.02em | screen titles |
| `displaySm` | Doto | 36sp | 40sp | −0.02em | section heroes |
| `heading` | Geist Sans Light | 24sp | 29sp | −0.01em | headings |
| `body` | Geist Sans | 16sp | 24sp | 0 | body |
| `bodySm` | Geist Sans | 14sp | 21sp | 0.01em | secondary body |
| `label` | Geist Mono | 11sp | 13sp | 0.08em | ALL-CAPS labels |
| `data` | Geist Mono | 14sp | 20sp | 0 | timers, counters |
| `cellNumeral` | Geist Sans | 20sp | 20sp | 0 | grid numbers |

Doto is display-only, 36sp and above. Never below.

---

## Components

### `Label`

All-caps tracked mono text. The workhorse — Nothing labels things instead of using icons.

| | |
|---|---|
| Type | `label` |
| Colour | `textSecondary` default, `textPrimary` when emphasised |
| Transform | uppercase, applied in the component, not by the caller |
| Tokens | `label`, `textSecondary`, `textPrimary` |

### `NothingText`

The text primitive — every piece of text in every game goes through here. That is the
mechanism by which `:app` never names a colour or a type size (Requirement 15 criterion 4).

| | |
|---|---|
| Colour | resolved from the `TextRole` argument — the component takes a role, never a `Color` |
| Type | a `NothingTheme.typography` style, default `body`; a caller cannot invent a size |
| Alignment | optional `TextAlign` |
| Guard | debug-only: any style at 36sp or larger reports a display element, so a screen cannot quietly grow a second one (Requirement 9 criterion 7) |
| Tokens | consumes `TextRole` roles and typography styles; defines none of its own |

`Label` (above) is the workhorse form built on it; `NothingBackground` (below) is the other
component every screen touches — the base plane it all sits on.

### `NothingBackground`

The full-screen base plane. Exists so a screen cannot forget its background and inherit a
Material default.

| | |
|---|---|
| Draw | `Box`, `fillMaxSize` |
| Fill | `bg` (resolved as `SurfaceRole.Background`) |
| Tokens | `bg` |

### `NothingButton`

| | |
|---|---|
| Shape | `radiusPill` |
| Height | `space48` |
| Padding | horizontal `space24`, vertical 0 |
| Touch target | minimum 48×48dp, extended beyond drawn bounds if smaller |
| Background | `surface`; `bg` for the low-emphasis (`Quiet`) variant |
| Border | `borderSubtle`, 1dp |
| Selected | border → `borderVisible`, text stays `textPrimary`. The selection language across the difficulty picker, settings and theme options — no accent colour is spent on a non-error state. |
| Label | `label` token, `textPrimary` |
| Pressed | border → `borderVisible`, text → `textDisplay`, over `durationMicro`. No scale, no hue shift. |
| Disabled | text → `textDisabled`, border → `borderSubtle`, no press response |
| Tokens | `radiusPill`, `space48`, `space24`, `surface`, `bg`, `borderSubtle`, `borderVisible`, `label`, `textPrimary`, `textDisplay`, `textDisabled`, `durationMicro`, `easingStandard` |

### `NothingCard`

| | |
|---|---|
| Shape | `radiusCard` |
| Background | `surface` |
| Border | none — elevation is the surface shift alone |
| Shadow | 0dp, always |
| Padding | `space24` |
| Tokens | `radiusCard`, `surface`, `space24` |

### `NothingTopBar`

| | |
|---|---|
| Height | `space64` |
| Background | `bg`, no divider beneath |
| Title | `label`, `textSecondary`, uppercase, centred |
| Back affordance | text `BACK` in `label`/`textSecondary`, left aligned, `space16` inset. Not an icon. |
| Trailing slot | optional, one `Label` action |
| Tokens | `space64`, `space16`, `bg`, `label`, `textSecondary` |

Words over icons is deliberate: see `nothing-study.md` §6.

### `DotMatrixReadout`

The Glyph reference. **Always carries information** — never decorative. See decision D12.

| | |
|---|---|
| Draw | Compose `Canvas`, single pass, no image asset |
| Dot diameter | `optical2` |
| Grid pitch | `space16` or `space24`, uniform both axes |
| Colour | `borderVisible` at 0.10–0.20 alpha |
| Caching | dot positions computed once per size; recomputed only when pixel bounds change |
| Max dots | 5,000 per pass |
| Input | `progress: Float` 0f–1f. Dots below the progress threshold draw at 0.20 alpha, above at 0.10. |
| Pointer | consumes nothing, exposes no accessibility node |
| Reduced motion | static, no animation of dot state |
| Tokens | `optical2`, `space16`, `space24`, `borderVisible` |

Tier 1 use: `progress` is Sudoku completion percentage.

### `DotPulse`

The instrument's idle breath — shown while a puzzle generates and again on the finish panel.
Not a spinner: three dots breathing on a slow cycle read as "a real process is running",
where a spinner reads as progress the app does not actually have.

| | |
|---|---|
| Draw | Compose `Canvas`, three dots in a row |
| Dot diameter | `optical2` |
| Dot pitch | `space16` |
| Colour | `borderVisible` |
| Motion | each dot fades 0.18 → 0.85 alpha on a 600ms cycle, staggered 120ms per dot, infinite `Reverse`. Opacity only (S13) — no scale, no spin, no spring |
| Accessibility | no node; the surrounding `Label` carries the meaning |
| Reduced motion | static end state; the caller gates with the platform flag |
| Tokens | `optical2`, `space16`, `borderVisible` |

### `SlideDotIndicator`

Pagination for the Home resume cards — a row of dots that tells you *where* you are rather
than *how much* is left. Same Glyph vocabulary as `DotMatrixReadout`, axis changed from
progress to index.

| | |
|---|---|
| Draw | Compose `Canvas`, one row of dots |
| Input | `total`, `current`; renders nothing when `total <= 1` |
| Active dot | `textDisplay` — or the chosen TE accent (sage/amber) when one is set (C8–C11) |
| Other dots | `textDisabled` |
| Dot diameter | `optical2`, drawn at 0.90 scale so the pager reads secondary to the card above it |
| Dot pitch | `space16` |
| Tokens | `optical2`, `space16`, `textDisplay`, `textDisabled`, `accentSage`, `accentAmber` |

### `GameIcon`

Renders a `GameIcon.DotMatrix` boolean grid. No image asset — this is the Glyph reference at icon scale.

| | |
|---|---|
| Draw | Compose `Canvas`, single pass |
| Input | `rows: List<List<Boolean>>`, any square size; Sudoku uses 5×5 |
| Box | 24dp square |
| Dot diameter | box side divided by `(2 × gridSize + 1)`, so a 5×5 grid gives ~2.2dp dots |
| Dot pitch | box side divided by `gridSize`, computed — never a literal |
| Colour | `textPrimary` for a true cell; nothing drawn for false |
| States | no press state; icons are not interactive |
| Tokens | `textPrimary` |

Authoring a pattern: draw it in [dotmatrixtool](https://github.com/stefangordon/dotmatrixtool) or
[smittytone/ASCII](https://github.com/smittytone/ASCII), then transcribe to booleans. Keep patterns
legible at 24dp — five rows is the practical ceiling.

### `DifficultyPicker`

Replaces the play area in place when `NEW` is tapped or a game is finished. Decision D18 — not a
screen, not an overlay; a player who never wants to choose a difficulty never sees a chooser.

| | |
|---|---|
| Layout | a `Label` reading `DIFFICULTY`, then the options as full-width `NothingButton`s stacked with `space16` gaps |
| Options | whatever the game passes as strings — `sudoku(9)` hands it `DAILY` first (the day's puzzle, decision D31), then `SIMPLE`, `EASY`, `MODERATE`, `HARD`, `CHALLENGE`. The component never learns what an option means |
| Current | the active difficulty renders with `borderVisible` instead of `borderSubtle` |
| Dismiss | a `NothingButton` labelled `CANCEL` below, `space32` gap |
| Tokens | inherits `NothingButton` and `Label`, plus `space16`, `space32`, `borderVisible` |

### `GridCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Size | computed from available width, never a `dp` literal |
| Background | `surface` default, `surfaceRaised` when selected or **sibling** (another cell holding the selection's digit). The **cross** — the row/column/box of the selection — keeps the base fill |
| Selected | `surfaceRaised` fill **plus** a 1dp `borderVisible` outline (or the TE accent), the bright hero of the highlight |
| Cross layer | the row, column and box of the selection, lit on **every** tap: base `surface` fill unchanged, strength carried by a **soft ring** — a 1dp `borderSubtle` hairline. Two grey fills cannot carry two strengths, so the cross is a faint frame, not a wash |
| Sibling layer | other cells holding the selection's digit, lit when the selection is filled: `surfaceRaised` fill **plus** a 1dp `borderSubtle` outline (or the TE accent) — the bright, framed digit set inside the cross's ring |
| Given value | `cellNumeral`, `textPrimary` |
| Player value | `cellNumeral`, `textDisplay` |
| Conflict, accented | the **whole cell fills `accentRed`** — the alarm is the cell, not a frame. The numeral stays `cellNumeral` / `textDisplay`: white measures 5.19:1 on `#D71921`, above the 4.5:1 text floor, so the red cell carries no red text. No outline — the red fill outranks even a selection border. Red is a shape-fill role (Requirement 14 criterion 7). See `rationale.md` §8. |
| Conflict, non-accented | `cellNumeral`, `textSecondary`, plus a 1dp `borderVisible` outline |
| Empty | no text |
| Pencil marks | positioned like paper — a 3×3 mini-grid inside the cell, each digit where it would sit when solved (1 top-left, 5 centre, 9 bottom-right), `label` size, `textDisabled`, up to 9 marks, excluded from the per-screen type-size count |
| TE accent (optional) | when sage or amber is chosen in Settings: generated given cells carry an 8% tint of the accent in their fill, and the selected cell's and sibling layer's outlines take the accent. The cross layer stays bare — it is fill's whole statement. At `none` — the default — none of this exists; red stays the only error accent (C8–C11) |
| Pressed | background → `surfaceRaised` over `durationMicro` |
| Given cells | reject input; value and given status never change. They are still selectable — selecting a given is how you ask where its siblings are |
| Tokens | `radiusCell`, `surface`, `surfaceRaised`, `cellNumeral`, `textPrimary`, `textDisplay`, `textSecondary`, `textDisabled`, `accentRed`, `accentSage`, `accentAmber`, `borderVisible`, `borderSubtle`, `durationMicro` |

**3×3 box grouping is spacing, not lines — and it lives in the grid, not the cell.** The Sudoku grid sets its boxes `space8` apart on both axes and its nine cells `space4` apart within each box (Requirement 11 criterion 8 forbids divider lines), so the 3×3 structure is conveyed by rhythm alone: the board is just the 81 cells floating on the screen background, each `surface` fill with no resting outline. `GridCell` never draws a divider or a group frame.

**Selected and sibling share the `surfaceRaised` fill**; the cross keeps the base `surface` fill. There
are only three surface levels and no step above `surfaceRaised`, and **two grey fills cannot carry two
strengths** — that is why the market leader's faint tint has to be blue. So the strengths split the
tokens instead: the sibling digit set is the bright raised block, and the cross is a **soft ring**
(1dp `borderSubtle` on unchanged cells) framing it. Selection is carried by the outline — `borderVisible`
(or the TE accent) when nothing is wrong. The screen's one urgent signal is not an outline at all: a
conflicted cell's *whole fill* turns `accentRed`, which no state can override, so a cosmetic outline can
never displace the alarm.

**One rule lights every tap: the cross, and the digit set on top.** The grid lights the selection's
row/column/box on every tap — *what can go here?* — and, when the tapped cell holds a digit, additionally
lights every other cell holding that digit — *where else is this digit?* (decision D30 as amended by the
market study §5A). A fixed either/or rule fails one of the two questions; composing them answers both,
which is what the market leader does.

### `NumberPad`

| | |
|---|---|
| Layout | 9 circular keys in one row if width allows, else 5 + 4 |
| Key shape | circle, `radiusPill` |
| Key size | `space48` |
| Key background | `surface` |
| Key label | `cellNumeral`, `textPrimary` |
| Remaining count | badge on the key's upper-right: `space16` circle, `bg` fill, `label` / `textSecondary` |
| Exhausted digit | `textDisabled` on both the digit and its badge `0`, still tappable, no error state |
| Pressed | background → `surfaceRaised` over `durationMicro`. Digit keys fire **no haptic of their own** — only the caller knows whether the entry conflicted, and decision D15 wants tick and conflict to feel different. The three action buttons tick. |
| Erase | a `NothingButton` labelled `ERASE`, not an icon |
| Notes toggle | a `NothingButton` labelled `NOTES`, border → `borderVisible` when active |
| Undo | a `NothingButton` labelled `UNDO`, disabled styling when history is empty. Decision D17 |
| Hint (optional) | a `NothingButton` labelled `HINT`, rendered **only** when the caller passes a non-null `onHint`. sudoku(9) passes it — its hint engine deduces a forced digit without ever holding the solution (see `SudokuHint`); games with no deduction engine pass nothing and the row stays three actions |
| Tokens | `radiusPill`, `space48`, `space8`, `space4`, `surface`, `surfaceRaised`, `cellNumeral`, `label`, `textPrimary`, `textSecondary`, `textDisplay`, `textDisabled`, `accentSage`, `accentAmber`, `durationMicro` |

**The remaining count** is how many of that digit are still unplaced, shown as a badge on the key's
upper-right.

The badge needs no offset to land there. The key is a circle inscribed in its `space48` box, so that box's
upper-right *corner* is outside the circle — a `space16` badge aligned to the corner therefore sits centred
on the circle's upper-right arc while staying inside the key's own bounds. That last part is what makes it
safe: keys are `space8` apart, and a badge that overhung horizontally would eat most of the gap to its
neighbour on a 360dp phone, where the pad is already the tightest element on the screen.

Its fill is `bg`, not `surfaceRaised`, so it reads as an aperture punched through the key rather than a
sticker on it — and `bg` is the only choice legible against *both* key states, since `surfaceRaised` would
merge into the pressed fill at the moment the player is looking at it.

Zero renders as `0` at `textDisabled` rather than disappearing, because a blank would be ambiguous with the
counts being switched off. The badge carries no accessibility node: the count is already in the key's
click label, so announcing it twice would read the number twice.

It is a preference (`showRemaining`), defaulting on. The count was always computable by the player, so
showing it exposes the mechanism rather than solving anything — but some players read any aid as
assistance, so it is their call.

**Accent interplay.** When sage or amber is chosen in Settings and the key's digit matches the cell
selected on the board, the key's outline and badge fill take the accent, and the badge text reads at
`textDisplay` — the count joins the highlight system instead of sitting beside it (C8–C11). Exhausted
digits keep their disabled styling and take no accent.

### `NonogramCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Size | computed from available width, never a `dp` literal |
| Filled | `surfaceRaised` fill — a decided picture pixel |
| Unknown | `surface` fill, no resting outline |
| Marked (decided empty) | base `surface` fill plus a centred dot in `borderVisible`, sized as a fraction of the cell — the Glyph's own dot colour, never text |
| Selected | a 1dp `borderVisible` outline |
| Peer (the selection's row and column) | a 1dp `borderSubtle` soft ring on the unchanged fill — two grey fills cannot carry two strengths, so the band is a frame, not a wash (the same split as `GridCell`'s cross) |
| Input | tap and long-press, no Material ripple. Taps fire no haptic of their own — only the caller knows whether the entry was a mistake, and decision D15 wants those to feel different |
| Pressed | background → `surfaceRaised` over `durationMicro` |
| Tokens | `radiusCell`, `surface`, `surfaceRaised`, `borderVisible`, `borderSubtle`, `durationMicro` |

**No red exists on this cell.** A wrong fill is found by contradiction, not announced
(method §6) — there is no conflict state to render, and the lives limit ends the run in
words on the completion screen, never on the board.

### `MinesweeperCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Size | computed from available width, never a `dp` literal |
| Unknown | `surface` fill, no resting outline |
| Revealed | `bg` fill — opened ground reads as lower, not higher; the numbers float on it |
| Adjacency numeral | one numeral, `label` style (all-caps mono does not apply — it is a digit), `textPrimary`, centred. Colour is never per-count: the glyph already distinguishes 1–8, and the colours were always redundant (method §4) |
| Flagged | base `surface` fill plus a centred dot in `borderVisible`, sized as a fraction of the cell — the same mark idiom as `NonogramCell`'s decided-empty: a dot that means "I believe a mine is here", the motif doing real work |
| Detonated | the **only red element** on the screen: `accentRed` fill with the numeral `textDisplay` over it — 5.19:1, above the 4.5:1 text threshold. Terminal state, so red never appears during normal play |
| Selected | a 1dp `borderVisible` outline |
| Input | tap to reveal, long-press to flag. No drag: minesweeper has no run to paint |
| Pressed | background → `surfaceRaised` over `durationMicro`, reverting on release — a transient press, unlike the decided fills of the other two games |
| Tokens | `radiusCell`, `surface`, `surfaceRaised`, `bg`, `borderVisible`, `accentRed`, `textDisplay`, `textPrimary`, `durationMicro` |

**Red, spent once.** `minesweeper(10)` is the first game whose single red condition fires in play — the
detonation that ends the run. One cell, outline-and-fill, never text alone, always accompanied by the
numeric `MINE` readout hitting zero and the completion screen naming the loss in words, so the state
survives the grayscale test (method §6).

### `ConnectCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Size | computed from available width, never a `dp` literal |
| Empty | `surface` fill, no resting outline |
| Path | `surfaceRaised` fill — the decided-cell idiom, shared with `NonogramCell` |
| Endpoint | base `surface` fill, numeral centred in `textPrimary`, `label` style — **numerals, never colours** (roadmap §4: the whole monochrome adaptation of Numberlink) |
| Route channel | bars from cell centre to each connected edge, `borderVisible` ink, 34% of the cell — pure geometry, no Canvas, no glyph assets; adjacent bars join across cell boundaries into one continuous line |
| Selected | a 1dp `borderVisible` outline (the live walk's head) |
| Red | none, by construction — every move is retractable, so connect has no wrong move (method §6) |
| Input | tap an endpoint to start a walk; drag to step; tap again or lift to commit |
| Tokens | `radiusCell`, `surface`, `surfaceRaised`, `borderVisible`, `textPrimary`, `durationMicro` |

**Channels, not icons.** There is no icon set in this project and no Canvas anywhere in the
design system, so a route is drawn from the two primitives that exist: a fill that means
"decided", and a bar that means "continues this way". The channel computation (which sides
a route continues toward) belongs to the game layer — the component's contract is only that
channels connect same-value neighbours, which is what keeps two adjacent routes readable
without colour.

### `WordsearchCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Size | computed from available width, never a `dp` literal |
| Hidden | `surface` fill, letter centred in `textSecondary` at `cellNumeral` — the letter is always visible; the game hides positions, not content |
| Found | `surfaceRaised` fill, letter in `textPrimary` — the decided-cell idiom shared with `NonogramCell`; it stays lettered so the grid remains readable |
| Live trace | a 1dp `borderVisible` outline — follows the finger and vanishes on release; never the committed-ink idiom the other boards use for selection |
| Red | none, by construction — a wrong drag costs only the tracing and simply does not lock (method §6) |
| Input | none — the grid owns the drag gesture; the cell has no click handler at all |
| Tokens | `radiusCell`, `surface`, `surfaceRaised`, `borderVisible`, `textPrimary`, `textSecondary`, `cellNumeral`, `durationMicro` |

**Letters, always.** Every other grid cell in the studio starts silent and earns its content; this one
starts lettered. A word search hides where words run, not what the grid holds, so the cell's only two
states are huntable and found — and found keeps the letter, which is the genre's quiet satisfaction.

### `BlockCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Size | computed from available width, never a `dp` literal |
| Ground | `surface` fill |
| Landed | `surfaceRaised` fill — the decided-cell idiom shared with `NonogramCell` and the found `WordsearchCell` |
| Red | none, by construction — a piece that does not fit refuses to land, so nothing here can be wrong (method §6) |
| Input | none — the board is a drop target; the tray piece owns the drag |
| Tokens | `radiusCell`, `surface`, `surfaceRaised` |

**Two states, no drama.** The studio's only bi-state board: ground or block. Placement is validated
before commit, so the cell never expresses error — the one game where nothing can be wrong.

### `BlockPiece`

| | |
|---|---|
| Shape | the catalog geometry, row-major 0/1; empty cells render nothing, so the silhouette is exact |
| Densities | **solid** fill, **half-pitch** centred dot, **hollow** outline — decision D33's monochrome answer to three concurrent pieces |
| Colour | `textPrimary` — density is the identity, never hue; grayscale-safe by construction |
| Cells | circles on the shared cell radius, so a landed piece reads as the board's own material |
| Input | the whole piece is the drag origin (D33: it follows the finger, then resolves by opacity — the app never moves it) |
| Tokens | `textPrimary`, cell radius |

**Density is data.** Three pieces share one tray and one palette; what distinguishes them must survive
grayscale, so the language's own device — dot pitch — does the work a colour scheme would in the
market leaders. Decoration would be the same three shapes drawn the same way; this is information.

### `AkariCell`

| | |
|---|---|
| Shape | `radiusCell` |
| Wall | `background` fill — **the studio's only data-bearing void**: clues render numerals over it, so a wall both blocks light and constrains play |
| Ground | `surface` fill |
| Lit | `surfaceRaised` fill — lit reads *higher*, the opposite of `MinesweeperCell`'s revealed ground, because lighting adds information rather than removing hiding |
| Bulb | a centred dot in `textDisplay` over the raised fill — the shared "something is here" motif, and the game's whole subject |
| Clue numeral | `textPrimary`, one hue for every count (method §4); the digit already distinguishes 0–4 |
| Red | none, by construction (parity A5): a clash is two lit bulbs, an over-numbered wall is a numeral over too many dots — visible on the board, never announced |
| Input | tap on white cells only; walls are not in the cycle — the refusal is structural |
| Transition | fill `animateColorAsState` over `microSpec` — colour and opacity only, per S13 |
| Tokens | `radiusCell`, `surface`, `surfaceRaised`, `background`, `textDisplay`, `textPrimary`, `cellNumeral` |

**Walls are voids with data.** Every other game's void is inert (minesweeper's mine, blockpuzzle's
out-of-bounds); akari's walls carry the constraints that make the puzzle a puzzle. The material
story still holds: dark ground is `surface`, the bulb's light raises it — the light you place is
the same substance the whole studio draws with.

### `NothingBottomNav`

| | |
|---|---|
| Layout | one `Row`, items at equal weight |
| Item height | `space64` — the same as `NothingTopBar`, so the two bars framing a screen agree |
| Item content | a **word**, `label` style, all-caps tracked mono. Never an icon. |
| Selected | `textPrimary` |
| Unselected | `textDisabled` |
| Selection change | `animateColorAsState` over `durationMicro`. Colour only — nothing moves or resizes. |
| Bar fill | `surface` |
| Separation from content | the `surface`/`bg` step alone. **No top border, no shadow.** |
| Insets | consumes `navigationBars` itself |
| Tokens | `space64`, `surface`, `label`, `textPrimary`, `textDisabled`, `durationMicro` |

**Words, not icons.** Every other bottom bar on the platform is a row of pictograms; rule T9 says a word
goes wherever a word fits, and a three-item nav is exactly where one fits. It also happens to suit a project
with no icon set and no image assets — but the rule came first.

**No divider above it.** A top border on a bottom bar is the reflex and rule P8 forbids it. Separation is
the one perceptual step from `bg` to `surface`, which is the only elevation this language has (rule C1).

**It consumes its own bottom inset**, so it reaches the screen edge while its words stay clear of the
gesture area. Screens above it therefore inset **top and sides only** — padding the bottom in both places
leaves a visible dead band. Shown on the three tab destinations, absent on a game screen.

---

## Screen specs

Every screen: `bg` background, at most three type sizes, at most two weights, at most one display
element, group separation `space32` or greater, no divider lines.

### Home

Two forms from one composable, chosen by `GameRegistry.standalone` — decision D25. A studio build shows a
library; a build that ships one game shows that game's start page.

**Library form** (`standalone == null`)

```
space48
  "NO ADS"             label / textSecondary, centred
space8
  "STUDIO"             displayMd / textDisplay      ← the one display element
space96
  resume card          NothingCard, only when a save exists     ← see below
space32
  "ALL GAMES"          label / textSecondary, only when a resume card is above it
space16
  game list            one NothingCard per GameRegistry entry
space16 between cards
    card:  GameIcon dot matrix   24dp
           "sudoku"             body / textPrimary
           "CLASSIC 9×9 NUMBER PUZZLE"  label / textSecondary
space16
  "HOW TO PLAY"        clickable label / textPrimary — last item of the list, only when no
                        fixed PLAY button exists below (library with no single resume)
space32
```

**Start-page form** (`standalone != null`)

```
space48
  "NO ADS STUDIO"      label / textSecondary, centred
space8
  "SUDOKU"             displayMd / textDisplay      ← the one display element
space8
  "CLASSIC 9×9 NUMBER PUZZLE"    label / textDisabled
space96
  resume card          NothingCard, when a save exists          ← see below
space16
  "PLAY"               NothingButton, fixed at the bottom centre — present whenever
                        "play" has one unambiguous answer (standalone, or exactly one resume)
space8
  "HOW TO PLAY"        clickable label / textPrimary — the quiet entry to the rules page,
                        directly under PLAY
space32
```

Both forms sit above a persistent `NothingBottomNav`, so neither carries `STATS`/`SETTINGS` buttons of its
own and both inset top and sides only.

**The rules entry** (decision D26, amended 2026-09-03). Both forms open the same full-height
`HOW TO PLAY` destination. The link is words only — no pill, no second button competing with PLAY —
and exactly one renders per screen: under the fixed PLAY button when one exists (start-page form, or
a library with a single resume), otherwise as the last item of the scrollable list.

**The resume card.** A saved game is a thing you read, not a button you have to trust:

```
NothingCard, clickable, the whole card is the target
  "IN PROGRESS"                        label / textSecondary
  "sudoku"                             body / textPrimary   ← library form only
  "MODERATE · 62% DONE · 04:12"        label / textDisabled
  "CONTINUE"                           label / textPrimary
```

This replaced a lone button whose entire vocabulary was `CONTINUE` versus `PLAY`. That said a save existed
and nothing else — not which puzzle, not how far in, not whether it was the one nearly finished or the one
abandoned. Those are the facts that decide whether you tap it, and they were already in the save.

Three metrics and no more, for the same reason the game screen shows three readouts (decision D13): a
curated view, not a state dump. The game name appears only in the library form, where the display element
above does not already name it.

The whole card is the target rather than a nested button — a button inside a clickable card gives two
overlapping hit areas for one action, and the card is the larger, more forgiving one.

Must present within 1s, without waiting on any generator — which is why there is no resume card for the one
frame before disk answers.

### Stats

A tab destination: `NothingBottomNav` sits below it, so the top bar carries **no back action** — a back
arrow would offer a second, contradictory answer to "where does this go". Insets top and sides only.

```
NothingTopBar    "STATS"
space32
  total won        displayMd / textDisplay          ← the one display element
space8
  "PUZZLES SOLVED" label / textSecondary
space96
  per game:
    "SUDOKU(9)"        label / textSecondary
  space24
    per difficulty, ordered by games played:
      "MODERATE"                      label / textDisabled
    space4
      "SOLVED" 9  ·  "PLAYED" 12  ·  "RATE" 75%  ·  "BEST" 04:03  ·  "AVG" 05:41
      label / textDisabled  +  data / textPrimary, space32 apart
```

Empty state is one line — `NOTHING FINISHED YET` at `textDisabled`. No illustration, no mascot, no
encouragement copy.

**A record here is a reading on a dial, not a trophy.** No badges and no achievement art: `avoid.md` bars
the assets, and a trophy case is the opposite of the instrument panel this language is built on.

The screen is generic over `GameRegistry`, so a second game appears with no change to it. Its per-value
helper is named `StatLine`, **not** `Readout` — `D13-three-readouts` counts `Readout("` per file to hold a
*game* screen to three, and a record page legitimately carries more numbers than that.

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
as amended by `sudoku-market-study.md` §5A): the cross is the soft raised wash, the sibling digit
set the framed layer inside it.
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

**Generalised to every game — decision D32.** The DAILY contract above is not sudoku-specific:
every game's picker leads with it (the shell renders `listOfNotNull(dailyLabel) + difficulties`,
and each game's own `NEW` picker now matches), every generator runs seeded with the UTC epoch
day, and every game stores its daily under `daily.<gameId>.*` — the same outside-the-resume-scan
slot rule. One minesweeper-specific deviation: first-tap safety forbids a pre-laid board, so its
seed is `[day, first tap]` — the same day *and the same opening* always meet the same field,
which is the honest limit of determinism for that game. Each game's restore accepts the `Daily`
label (`KNOWN_LABELS = DIFFICULTIES + DAILY_NAME`); unknown labels are still rejected.

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

The timer caption says the clock runs either way because hiding a running clock would otherwise be a small
deception — the elapsed time still lands on the stats page.

**MISTAKES** is off by default (unlimited — `MISS` counts but never ends the run). On, it sets the
ceiling to three; the third conflicting entry fails the run, and the single red cell still marks the
last mistake.

**THIRD ACCENT — TE** offers `NONE` (the default), `SAGE` and `AMBER`. This is the only place an
accent colour other than red enters the system, and it is a choice, not a default: at `NONE` the game
is strict Nothing. Chosen, the accent tints generated givens at 8%, takes the selected cell's outline,
and joins the highlight and remaining-count readouts — still normally absent, at most one element,
non-colour paired (C8–C11). See `nothing-tokens.md` for why muted TE sage/amber rather than a brand
colour.

All seven settings persist in the same DataStore as the game state, under a `settings.` key prefix:
`themeMode`, `hapticsEnabled`, `showRemaining`, `showTimer`, `showPeers`, `mistakeLimit`,
`accentChoice`. One store, one prefix, no second mechanism. Decision D7.

**Every one of them must actually be read at its call site.** `hapticsEnabled` was persisted here, rendered
here, and consulted by nothing for the whole of its first life — see decision D15 and the
`D15-gated-haptics` build rule that now prevents the recurrence.

### How to play

A full-height destination, not a tab: no `NothingBottomNav` beneath it, and the top bar carries
**BACK to Home** rather than a tab's no-back. Decision D26, amended 2026-09-03 — the original
refusal of any rules page was reversed on the market study's reframing: a *reference* page that
points at the board's own signals is not onboarding. See the decision record for the reasoning.

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

Six groups, each a caption over one-line sentences. No display element, no red, no buttons — the
page is deliberately quieter than every surface it explains. It teaches by pointing at behaviour the
board already speaks: the cross and sibling set (`sudoku(9)` §One tap), the single red conflict cell,
NOTES, ERASE/UNDO, and what STATS records. Every claim refers to a surface that exists, so the page
says nothing the player cannot verify in one tap.

The content is sudoku's for now — the only game in the registry. If a second game joins the studio,
this destination must become per-game like the `sudoku(9)` screen already is.

---

## Review before merge

The four tests in `nothing-study.md` §12, in order: squint, grayscale, gimmick, detail.
