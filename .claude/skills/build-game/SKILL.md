---
name: build-game
description: Build a Nothing Games Studio game or screen with minimal token spend. Use when the task is adding a game, building a screen, adding a component, fixing a game bug, or any work inside projects/Sudoku.
version: 1.0.0
allowed-tools: [Read, Write, Edit, Glob, Grep]
status: canonical
---

# Build a Game, Cheaply

**First action: read `ideas/builder-kit.md` and obey it.** It caps you at 6
file reads, names exactly which files per task, and lists what to never open.

The short version if the kit is already loaded:

1. Fill the 8-answer packet (kit §3) before writing code.
2. Implement your game's MUST rows in `ideas/market-parity.md`, none of REFUSE.
3. Copy `design-canon/game-scaffold.md` for new games; build screens literally
   from `design-canon/component-specs.md`; values only from `design-canon/tokens.json`.
4. Iterate with `./gradlew conformanceCheck -Pconformance.fast=true`, finish
   with `./gradlew assembleDebug check` from `projects/Sudoku/`. Do what every
   `FIX:` line says. Both APKs current before done. One game `in_progress` at a time.
