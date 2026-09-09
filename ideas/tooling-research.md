---
status: research
---

# Tooling Research — open-source tools that make this project materially better

Vetted 2026-09 against three hard filters, in order: **license permits commercial
use** (Apache-2.0 or equivalent — R4's GPL isolation stays intact), **build/test-time
only** (nothing lands in the release artifact, so the zero-permission, offline
product promise is untouched), and **it enforces something this repo already
believes** rather than adding a new belief. A tool that fails filter three is a
toy, however good its license.

Cross-references: `ideas/learning-library.md` §6 already tracks Vkompose,
Paparazzi, Konsist, and detekt. This file is the wider sweep and the adoption
order. Nothing here is wired up yet — each entry names its first concrete step.

## Tier 1 — adopt first; each enforces an existing rule mechanically

| Tool | License | What it enforces here | First step |
|---|---|---|---|
| **[Vkompose](https://github.com/VKCOM/vkompose)** `com.vk.vkompose` | Apache-2.0 (Maven Central artifacts) | `avoid.md`'s recomposition bans become compile errors: the skippability checker fails the build when a composable takes unstable parameters. Also ships a recomposition-highlighter IDEA plugin for the 81-cell grid. | Apply the Gradle plugin in `build-conventions`, enable `skippabilityCheck` with a stability config for `List<Int>` board types. |
| **[compose-rules](https://github.com/mrmans0n/compose-rules)** (detekt) | Apache-2.0 (Twitter fork, actively maintained) | Catches `@Composable` footguns our conformance grep cannot: state read without derived state, missing keys in grids, modifier chains rebuilt per call. | Add the detekt plugin to the check task; start with `ComposableNaming` + `ContentEmitterReturningValues`. |
| **[Roborazzi](https://github.com/takahirom/roborazzi)** | Apache-2.0 | Turns decision D9 from an idea into a gate: JVM screenshot tests of every screen at token-boundary states, no emulator. Catches the regressions greps never see — a spacing token swapped for another, a cell clipped by one dp. | Add to `:design-system`'s test source set; first capture: `BlockCell` filled/empty, `BlockPiece` at all three densities. |
| **[Robolectric](https://github.com/robolectric/robolectric)** (Roborazzi's engine) | Apache-2.0 (MIT-preferring for pure JVM) | The same JVM tests gain Android framework semantics — ViewModel + DataStore flows become testable without a device. | Arrives with Roborazzi; nothing extra. |

## Tier 2 — adopt when the reward loop is real

| Tool | License | What it gives here | First step |
|---|---|---|---|
| **[Maestro](https://github.com/mobile-dev-inc/maestro)** | Apache-2.0 (CLI is free/open; Maestro Cloud is the paid part — not needed) | YAML E2E flows on a real emulator: install the studio APK, start a sudoku, place three digits, assert the readout. This is `scripts/run-on-emu.sh` promoted from "device proof" to "asserted proof". | One flow per game: launch → complete one move → assert the cost readout. |
| **[Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/screenshot-testing)** (Google, official) | Apache-2.0 (AndroidX) | Google's own answer: snapshots every `@Preview` automatically. Complements Roborazzi — previews already exist as the design contract; this makes them tests. | Requires AGP 8.5+; check the version floor, then enable on `:design-system`. |
| **[Macrobenchmark + Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview)** (AndroidX) | Apache-2.0 | The 1-second launch budget gets a number instead of a feeling. Baseline profiles pre-compile the home → game → first-tap path; Macrobenchmark asserts startup stays inside budget on every release. | `:baselineprofile` module; startup metric on the studio flavour; generate profiles for the six game entry paths. |

## Tier 3 — noted, not recommended now

| Tool | Why it is parked |
|---|---|
| **[Paparazzi](https://github.com/cashapp/paparazzi)** | Superseded by Roborazzi here: Roborazzi runs on Robolectric, handles multi-frame interaction shots, and is the current community default. D9 named Paparazzi; the name updates, the decision stands. |
| **[Konsist](https://docs.konsist.lemonappdev.com/)** (Apache-2.0) | Real, but our `conformance.gradle` already implements architecture tests at Gradle level with the same power. Adding Konsist would create a second architecture-gate to keep in sync. Revisit only if conformance.gradle's grep rules hit their ceiling. |
| **[Renovate](https://github.com/renovatebot/renovate)** | Dependency freshness for CI, not local builds. Worth it the day this repo gains CI — no sooner. |
| **[Gradle Build Cache / Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)** | Gradle 9 already warns us about config-cache compat; fixing those warnings is free speed before adding any plugin. |

## The one-line summary

The repo's discipline is currently *grep-shaped* — conformance.gradle reads source
text. The tools above upgrade the three weakest gates to *semantic*: Vkompose
compiles against recomposition rules, Roborazzi *looks* at the rendered screen,
Macrobenchmark *times* the launch. Grep stays the first line; these are the
second. Adopt in tier order; each tier assumes the one before it.
