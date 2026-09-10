# Nothing Games Studio

Offline-first Android puzzle games in the Nothing OS design language. No ads, no tracking, no
network, no ongoing cost.

## The system

| Path | Purpose |
|---|---|
| `AGENTS.md` | Instruction entry point for every coding agent; `CLAUDE.md` links to it. |
| `ideas/` | Ideation, research, constraints, decisions (D1–D37), and the games roadmap — thinking, not values. |
| `design-canon/` | Authoritative design language: tokens, craft rules, component specs, provenance. |
| `projects/` | One Gradle build per project: eight game apps + two shared libraries (see below). |
| `conformance/` | The repo-root conformance gate (D37) — 33 rules wired into every project's `check`. |
| `scripts/` | Emulator tooling (boot, run a game, screenshot) — decision D28. |
| `specs/` | Feature specs in plain Markdown, readable by any agent or human. |
| `ideas/games-roadmap.md` | The catalog: admission criteria, tier order, and rejected candidates. |

Three rules hold the system together. The canon is the only source of design values. Ideas explain
reasoning and defer to the canon. Nothing vendor-specific holds content — agent-specific paths are
symbolic links.

## The projects (composite layout, D36)

Every game ships as its own app with its own `applicationId`, consuming two shared libraries via
Gradle composite builds:

| Project | Game |
|---|---|
| `projects/sudoku-app` | `sudoku(9)` — with the vendored GPL-3.0 QQWing engine |
| `projects/nonogram-app` | `nonogram(10)` |
| `projects/minesweeper-app` | `minesweeper(10)` |
| `projects/connect-app` | `connect(7)` — numberlink |
| `projects/wordsearch-app` | `wordsearch(12)` |
| `projects/blockpuzzle-app` | `blockpuzzle(8)` — endless placement |
| `projects/akari-app` | `akari(10)` — light-up |
| `projects/binairo-app` | `binairo(10)` — Takuzu |
| `projects/shell` | Game-agnostic chrome: home, stats, settings, rules, navigation, persistence |
| `projects/design-system` | Tokens generated from `design-canon/tokens.json`, theme, components |

## Build

```bash
cd projects/sudoku-app        # or any projects/<name>
./gradlew check               # that project's tests + the 33-rule repo gate
./gradlew :app:assembleDebug  # its debug APK
```

Changed a shared library? Check every consumer:

```bash
for p in projects/*-app; do (cd "$p" && ./gradlew check); done
```

On a device: `scripts/emu-start.sh`, then `scripts/run-on-emu.sh <game>` (build + install + launch
+ screenshot in one command).

## Where things stand

The design system, the shell, and **eight games** are implemented. **Nothing is shipped** — no store
listing, no users; "implemented" means built, gated, and unit-tested, not device-playtested. The
repo-root conformance gate (33 rules) and every project's unit tests are green. Remaining before
first launch: device playtesting per app, Paparazzi goldens, and an AGP upgrade to restore Android
Lint. `AGENTS.md` carries an honest per-directory status table; read it before assuming any document
describes running code.
