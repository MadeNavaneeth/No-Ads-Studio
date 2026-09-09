status: implemented
---

# The Agent Playbook

*How to work in this repo: direction, tips, and hard-won understanding from actually
building the games. Written by the agent that built five of them, for the agents that
come next. Read `AGENTS.md` first for the rules; read this for the reasons and the scars.*

---

## 1. What this project actually is

Not an app — a **design system with games attached**. The product is the language: one
monochrome Nothing-inspired visual canon (`design-canon/`), one shared component library
(`:design-system`), one shell, and games as clients of that language. Every game that
ships proves the language can express one more genre without breaking it.

This inverts the usual instinct. When you add a game, you are not "writing a game," you
are asking: *can the existing tokens, cells, gestures, and shell express this genre?*
Only what the genre genuinely needs may be new — and the new thing must be written as a
canon component with a `component-specs.md` entry, not as a private widget. Connect
needed route channels; wordsearch needed nothing new at all. That difference is a
compliment to the system, and it is measurable.

The conformance gate (`./gradlew conformanceCheck`, 36 rules) is not bureaucracy — it is
the language's enforcement layer. It parses the canon docs and the roadmap. **Write docs
as if a machine reads them, because one does.**

## 2. The direction (where the project is going)

- **Six of six roadmap games are implemented**: sudoku(9), nonogram(10),
  minesweeper(10), connect(7), wordsearch(12), and blockpuzzle(8) — the endless one,
  built on D33's rulings (piece follows the finger then lands by opacity; pieces
  identified by dot density; every dealt set placeable at deal time). Every one ships
  in `studio` *and* as its own standalone APK via a product flavour — one source
  tree, seven applicationIds, and licence isolation (the GPL QQWing engine only ever
  compiles into Sudoku-bearing artefacts).
- **The frontier is no longer "add games."** It is depth: polish, playtesting on
  devices, the four manual visual reviews (squint / grayscale / gimmick / red-count),
  the store/release pipeline (real keystore, `release` builds per flavour are
  already wired). Daily modes shipped for **all five games** (D32, generalising
  sudoku's D31: one UTC-day-seeded board per game, minesweeper's seed being
  [day, first tap] because first-tap safety forbids a pre-laid board).
- New games beyond the roadmap are possible but **must re-enter through the admission
  criteria** (roadmap §3): one quantity, three readouts, a monochrome adaptation,
  no-server constraint, 2–10 min sessions. The gate (`M5-roadmap-agreement`) fails the
  build if code and roadmap disagree, so the roadmap row comes *first*.

## 3. The recipe for adding a game (distilled to what actually works)

1. **Roadmap row first.** Status `in_progress` (and only one game may hold it). The
   table forces the decisions that matter: the one quantity, the win, the input.
2. **Rules object — pure, no Android.** Encode the board as `List<Int>` with named
   constants (`EMPTY`, `FILLED`, `FOUND`…). Every function total where possible; return
   null over throwing for "this input is not a game state."
3. **Generator — seeded, deterministic, budgeted.** `generate(difficulty, seed)`; the
   same seed must reproduce the same board (test it). Millisecond-scale; a timeout is a
   backstop, never the design.
4. **State + Move.** `@Immutable` data class, mutations return new instances. One
   reversible change per move, carrying enough to undo itself.
5. **ViewModel — the shell contract.** Sealed `Ui` hierarchy (Generating /
   GenerationFailed / Playing / ChoosingDifficulty / Complete); timer runs only while
   resumed (`LifecycleResumeEffect` on the screen); completion is a `SharedFlow` event,
   never derived from state; generation off the main thread.
6. **Persistence — D7.** A session data class in `studio/persistence` with `init`
   requiring structural validity; encode/decode functions that return null instead of
   throwing; a restore step *in the game source set* that validates semantics and may
   still return null. `main` **cannot see** game source sets — duplicate the few
   constants the codec needs and note why.
7. **Screen — copy the closest sibling.** Three readouts, top bar with `new`, difficulty
   picker, generating/failed/complete states, haptics at the call site, `pad` +
   `NothingSpacing`, no dp literals.
8. **Definition + icons + registries + flavour.** Dot-matrix icon whose dots *encode*
   (rule G1) — the parenthetical number is the identity; launcher icons are per-flavour
   vectors on the shared dot pitch. Wire build.gradle (flavour block + two sourceSets
   lines), both `GameRegistry` copies, `component-specs.md`.
9. **Tests in `src/test<Flavour>/java/...`** — rules, generator, restore/codec,
   transitions. Pure JVM, milliseconds. Then the full gate.

**Order matters less than completeness** — but writing rules + tests *before* the screen
means the screen is written against a proven core, which is where the fun bugs aren't.

## 4. Hard-won understanding (the scars, so you don't earn them)

### Construction beats search — on grids, decisively
The connect(7) generator first searched for Hamiltonian paths with warnsdorff's
heuristic + restarts. On a grid graph every interior cell has degree 4, so the heuristic
is *random* DFS, and 49 starts × exponential thrash = zero boards, ever (0/49, measured).
The fix was to **stop searching and construct**: partition rows/columns into strips,
walk them boustrophedon, cut after the walk — every such walk *is* a Hamiltonian path,
by construction, in O(cells). Lesson: when a search has no gradient to climb, build the
answer from a family of guaranteed-valid constructions instead. Timeouts are backstops,
not algorithms.

### Encoded values bite: the phantom-cell bug
Writing a cell "back to empty" as `lay(cells, i, 0)` wrote `pathOf(0)` = 8 — a *fifth*
board value that no renderer or validator knew about. Everything downstream quietly
misbehaved. Lesson: clear with the named constant (`EMPTY`), never a raw number; and
when tests fail in bulk across unrelated suites, suspect one shared encoding bug first.

### Encode invariants into the type, not the comment
`Placement.cells` now *always* stores first-letter-first; a reversed walk stores its
mirror and `reversed` is descriptive. Before that fix, the flag lied about the payload
and the legality check compared the wrong letters. A comment saying "cells may be
walk-order" is a trap for every future reader; an invariant in the type is a promise
the compiler helps keep. Same shape of lesson as sudoku's "the app never holds the
solution" — the architecture carries the guarantee, not the documentation.

### A guard that looks safe can kill a valid case
`if (abs(dr) > 1 && abs(dc) > 1) return emptyList()` in wordsearch's `evaluate` reads
like a knight-move filter. It actually rejects **every diagonal longer than 2 cells** —
so half the generator's placements were unfindable while the board looked normal. The
general straightness test was already there and subsumed it. Lesson: prefer the single
complete predicate (axis-flat *or* perfect diagonal) over stacked special-case guards;
and never accept a generator/rules split where each half is tested only against itself.
The `evaluate`-matches-a-generated-placement test is what closes that loop.

### StateFlow dedupes equals — re-emitting the same value is a no-op
Wordsearch's live drag highlight initially "updated" state that compared equal, so Compose
never recomposed. Gestures that must render without changing the model belong in their own
flow (`selection: StateFlow<List<Int>?>`), not in the `@Immutable` state. The rule of thumb:
**state is what persists; transient visuals that must move are flows.**

### Flavour source-set walls are real
`src/main` cannot reference `gameConnect`/`gameWordsearch` code — the codecs that need
board constants duplicate them locally with a note. This is the same wall that keeps GPL
code out of non-GPL flavours; it is load-bearing, don't fight it with cleverness.

### Tests ride per-variant: `src/test<Flavour>` auto-attaches; studio needs the explicit union
`testStudio` is *not* a free-floating source set — it is an AGP flavour name, and before
the fix studio silently ran only the shared 34 tests. Now `testStudio` attaches all five
game test directories and studio's variants run the union (226 tests, zero duplication).
When you add a game: one `test<Flavour>` dir + one line in the `testStudio` union.

### The `!!` ban reaches tests
Conformance scans test source sets too. Use `requireNotNull(x) { "..." }` — which is
better anyway: the message names the invariant that broke.

### Diagnostic tests are a legitimate tool — then delete them
When a generator returns null for every seed and no test tells you why, write a
temporary test that prints internals, run the one task, read the XML, **delete the
file**. Two real bugs (strip-walk connectivity, adjacent-endpoint routes) were found
this way in minutes after hours of guessing.

### When many tests fail, find the one root cause
15 connect failures collapsed into four roots; the wordsearch failure pointed straight
at a generator semantic bug. Don't patch tests to pass — *each* bulk failure was a real
product bug (or, at most, a test asserting a contract the code never had; fix the
contract or the code, explicitly, and say which).

### Ratchet ethics
The conformance gate only tightens. If a rule blocks you, the move is to *fix the code*
or, rarely, to argue the rule in the canon and change it deliberately — never to weaken
the check to get green. Every `in_progress` marker, every count, every status in the
docs is checked; keep them honest and the gate stays your ally.

## 5. Judgement the canon already made for you (don't relitigate)

- **Red is a budget**: at most one red element per screen, only for terminal/urgent
  state. Sudoku/nonogram/connect/wordsearch correctly use none; minesweeper's detonation
  is the one live spend.
- **Three readouts** (D13), tabular figures, `TIME` first.
- **Words, not icons** (T9): labels, buttons, nav.
- **One quantity** per game (method §2): % filled, % revealed, words found ÷ placed,
  pairs joined — the completion readout is that one quantity and nothing else.
- **No server, no network, no accounts.** Persistence is DataStore strings (D7); a
  corrupted save means "start fresh", never a crash.
- **Undo restores exactly.** Auto-cleaned pencil marks travel with the move that caused
  them (sudoku's sweep proves the pattern: capture before, restore after).
- **Difficulty pickers are modals with honest transitions**: opening one cancels
  in-flight generation, and dismissing it never lands on work that no longer exists.

## 6. The verification workflow (in order)

```
./gradlew :app:compile<Flavour>DebugKotlin     # fastest loop while writing a game
./gradlew :app:test<Flavour>DebugUnitTest      # the pure core
./gradlew check                                # all variants + 36 conformance rules
./gradlew assembleDebug                        # all six APKs
```

Test totals per variant are the no-duplication tripwire: studio must equal the union of
the others plus the shared 34. APKs land in
`app/build/outputs/apk/{studio,sudoku,nonogram,minesweeper,connect,wordsearch}/debug/`.

The **manual obligation no gate covers**: install, play one puzzle per game, then the
four reviews (squint, grayscale, gimmick-off, red-count). A game is not done until it
has been *played*; that is why roadmap statuses say `implemented` only with the honest
caveat that device playtesting is the remaining human step.

## 7. If you are the next agent, start here

1. Run `./gradlew check`; confirm 36 rules / 0 violations and a green studio suite.
2. Pick the smallest honest gap in this file's §2 (a stats surface, a playtest
   finding). The catalogue is complete — six of six roadmap games implemented — so
   the frontier is depth, and any *new* game must re-enter through the admission
   criteria (roadmap §3) before a line of code exists.
3. Read the closest sibling implementation end-to-end *before* writing anything; the
   codebase is deliberately repetitive across games so patterns are learnable by
   example.
4. Leave the docs and the code agreeing, the gate green, and one fewer thing in §2.
