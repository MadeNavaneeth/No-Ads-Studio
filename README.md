# Nothing Games Studio

Offline-first Android puzzle games in the Nothing OS design language. No ads, no tracking, no
network, no ongoing cost.

## The system

| Path | Purpose |
|---|---|
| `AGENTS.md` | Instruction entry point for every coding agent; `CLAUDE.md` links to it. |
| `ideas/` | Ideation, research, constraints, and the games roadmap — thinking, not values. |
| `design-canon/` | Authoritative design language: tokens, craft rules, component specs, provenance. |
| `projects/` | Buildable Gradle projects; each child is one self-contained Android app. The app lives in `Sudoku/`. |
| `specs/` | Feature specs in plain Markdown, readable by any agent or human. |
| `ideas/games-roadmap.md` | The catalog: admission criteria, tier order, and rejected candidates. |

Three rules hold the system together. The canon is the only source of design values. Ideas explain
reasoning and defer to the canon. Nothing vendor-specific holds content — agent-specific paths are
symbolic links.

## Build

```bash
cd projects/Sudoku
./gradlew assembleDebug      # builds all six APKs (studio + one per game)
./gradlew bundleRelease      # .aab for Play Console
```

APKs land in `app/build/outputs/apk/{studio,sudoku,nonogram,minesweeper,connect,wordsearch}/debug/` —
the studio app plus one standalone app per game, all from one source tree.

## Where things stand

The design system, the studio shell, and **all five roadmap games** — `sudoku(9)`, `nonogram(10)`,
`minesweeper(10)`, `connect(7)`, `wordsearch(12)` — are implemented. Each ships inside the studio app
*and* as its own standalone APK from the same source tree. The conformance gate (36 rules) plus 245
unit tests are green; device playtesting is the remaining human step. `AGENTS.md` carries an honest
per-directory status table; read it before assuming any document describes running code.
