# Requirements Document

## Introduction

This spec defines the **Nothing Games Studio Harness**: the workspace structure, the game catalog roadmap, and the codified Nothing design language that together turn the current one-off Android project into a repeatable studio system.

The directory taxonomy of Requirement 1 and the agent-neutral surface of Requirement 17 are in place: `ideas/`, `design-canon/`, `projects/`, and `specs/` exist, the documents carry status markers, the roadmap is written, and `Sudoku` builds clean under `projects/`. What remains is the enforcement machinery and the code. The design canon is prose rather than a machine-readable token table, there is no conformance checker, `theme/` has no spacing, shape, or motion tokens, `studio/` and `components/` are empty, `MainActivity.kt` is still a throwaway prototype that bypasses `NothingTheme` and `GameRegistry`, and nothing yet checks that a screen obeys the design language.

The harness solves three things at once:

1. **Workspace topology** — ideation in one place, buildable projects in another, specs in a third, one canonical design-system source that projects consume, plus a conformance check that keeps documents and code in agreement. Every document sits at a vendor-neutral path so any coding agent can read it, with `AGENTS.md` as the shared entry point and vendor-specific paths reduced to symbolic links.
2. **Games roadmap** — explicit admission criteria for candidate games, a tiered rollout order, and a stated harness capability that each new game exercises.
3. **Design language, end to end** — color/surface hierarchy, the four-level text hierarchy, the three-font role system, shape language, the 4dp/8dp spacing grid, dot-matrix texture, motion rules, and the single-red-accent rule, all expressed as tokens and reusable components with mechanical enforcement.

**Scope.** This spec covers structure and system definition: directory taxonomy, token and component contracts, the roadmap document, the bootstrap path for a new project, and the conformance checks. It also covers migrating the existing prototype onto the harness. It does **not** cover implementing the gameplay logic or UI of every catalogued game; each game gets its own spec.

## Glossary

- **Harness**: The whole studio system rooted at the workspace root directory, comprising the Ideas_Library, the Design_Canon, the Catalog_Roadmap, the Projects_Root, the Spec_Library, the Agent_Entry_Document, and the Conformance_Checker.
- **Workspace_Root**: The top-level directory of the harness (`/Users/navaneeth/Desktop/Games/`).
- **Ideas_Library**: The single top-level directory, named `ideas`, holding non-buildable ideation, research, constraint, and concept documents.
- **Design_Canon**: The single top-level directory, named `design-canon`, holding the authoritative Nothing design language documents (tokens, craft rules, component specs, resource provenance).
- **Catalog_Roadmap**: The single Markdown document inside the Ideas_Library that lists candidate and accepted games, their tier, their rollout status, and the harness capability each one exercises.
- **Projects_Root**: The single top-level directory, named `projects`, holding buildable projects; each child is one self-contained Gradle project.
- **Spec_Library**: The single top-level directory, named `specs`, holding one child directory per feature spec, each containing plain-Markdown requirements, design, and tasks documents read by Kiro and every other coding agent.
- **Agent_Entry_Document**: The plain-Markdown file `AGENTS.md` at the Workspace_Root, following the vendor-neutral cross-agent instruction convention, serving as the single instruction entry point for every coding agent.
- **Agent_Adapter_Link**: A symbolic link from a vendor-specific agent instruction path, such as `CLAUDE.md` or `.github/copilot-instructions.md`, to the Agent_Entry_Document.
- **Studio_Project**: One buildable Android project inside the Projects_Root, e.g. the existing `LightApp` project.
- **Project_Template**: The reference skeleton inside the Harness from which a new Studio_Project is bootstrapped.
- **Theme_Package**: The Kotlin package inside a Studio_Project that declares design tokens (`theme/`, containing colors, typography, spacing, shape, and motion tokens).
- **Component_Library**: The Kotlin package inside a Studio_Project holding shared Nothing-styled Compose components (`components/`).
- **Studio_Shell**: The shared navigation, home screen, settings, and persistence layer of a Studio_Project (`studio/`).
- **Game_Registry**: The single list in a Studio_Project that enumerates every registered GameDefinition (`core/GameRegistry.kt`).
- **Game_Module**: A self-contained game package implementing the GameDefinition contract, comprising a definition, a view model, and a screen.
- **Conformance_Checker**: The automated check, runnable from the command line, that verifies harness structure rules, design-token usage rules, and anti-requirement rules.
- **Anti_Requirements**: The prohibition list in `ideas/avoid.md` (no ads, no network permission, no analytics, no external image or sound assets, no gradients, no bright colors, no sharp corners, no bouncy motion, no cross-platform wrappers, no heavy dependencies, no large embedded datasets, no debug release builds, no extra permissions).
- **Dependency_Denylist**: The single list in the Design_Canon naming the Maven coordinates, group ids, and group/module prefixes a Studio_Project may not depend on, each tagged with its prohibited category (networking, advertising, analytics, crash reporting).
- **Design_Token**: A named value representing one color, type style, spacing step, corner radius, or motion duration, authored in the Token_Source and reaching code only as a Generated_Tokens constant.
- **Token_Source**: The single machine-readable file `design-canon/tokens.json`, the only place a design value is authored. Each entry carries `category`, `job`, `why`, and either `value` or the `dark`/`light` pair.
- **Generated_Tokens**: The Kotlin file produced from the Token_Source by the token generator task into `build/generated/`. Never committed, never hand-edited, regenerated on every build.
- **Design_System_Module**: The Gradle module `:design-system`, holding the Theme_Package and Component_Library, and the only module declaring Compose UI dependencies.
- **Consumer_Module**: A Gradle module rendering user interface by consuming the Design_System_Module — `:app`, plus one `:games:<id>` module per tier 2 game.
- **Suppression_Annotation**: The single-declaration annotation defined in Requirement 15 that permits one value to fall outside the Design_Token set, carrying the identifier of the rule it suppresses and a stated reason.
- **Token_Validation_Suite**: The JVM test suite defined in Requirement 18 that validates the Design_Token set itself — grid alignment, scale ratios, contrast minimums and maximums, frame alignment, redundancy, and the presence of a stated job and derivation for every token.
- **Text_Hierarchy**: The four ordered text color levels: display, primary, secondary, disabled.
- **Font_Role_System**: The three-font assignment: Doto for hero display and large numerals, Geist Sans for body and UI text, Geist Mono for labels, timers, and data. All three are SIL OFL licensed. Nothing's proprietary NDot and NType 82 faces are excluded from release builds.
- **Dot_Matrix_Texture**: The Canvas-drawn dot grid background component of the Component_Library.
- **Red_Accent**: The color `#D71921`, reserved for a single urgent or error element per screen.
- **Reference_Machine**: A 2-core, 8 GB MacBook Air, the baseline development machine for build and memory budgets.

## Requirements

### Requirement 1: Workspace Directory Taxonomy

**User Story:** As the sole developer, I want ideation, design canon, roadmap, and buildable code in clearly separated top-level directories, so that the workspace reads as a studio harness instead of a folder with an app in it.

#### Acceptance Criteria

1. THE Harness SHALL provide exactly four Harness-owned top-level directories under the Workspace_Root — the Ideas_Library named `ideas`, the Design_Canon named `design-canon`, the Projects_Root named `projects`, and the Spec_Library named `specs` — and SHALL place ideation documents only in the Ideas_Library, design language documents only in the Design_Canon, buildable Studio_Projects only in the Projects_Root, and feature spec documents only in the Spec_Library, excluding from this count any directory whose name begins with a dot, including `.kiro` and `.github`.
2. THE Workspace_Root `README.md` SHALL carry one entry for each of the four Harness-owned top-level directories and one entry for the Catalog_Roadmap document, each entry naming the directory or document and stating its purpose in exactly one sentence of 30 words or fewer.
3. THE Projects_Root SHALL contain the existing `LightApp` project as a single child directory retaining its Gradle wrapper, `settings.gradle`, `gradle.properties`, and `app/` module, and THE moved project SHALL complete a Gradle debug build with exit status zero within 10 minutes on the Reference_Machine.
4. THE Ideas_Library SHALL contain the complete content of the current `DESIGN_SPEC.md` and `AVOID.md` files, preserving every heading and every list item of the originals with additions limited to the document status marker and cross-reference links, and THE Workspace_Root SHALL NOT retain those two files after migration completes.
5. WHERE an Ideas_Library document and a Design_Canon document both specify the same Design_Token, the same Component_Library component, or the same Anti_Requirement, THE Design_Canon document SHALL be designated the authoritative version and THE Ideas_Library document SHALL contain a link to the Design_Canon document.
6. THE Harness SHALL name every Harness-owned directory and every Markdown file inside the Ideas_Library, the Design_Canon, and the Spec_Library using only lower-case letters `a` to `z`, digits `0` to `9`, and hyphen separators, with no underscore, space, or upper-case character, excepting the Workspace_Root files `README.md`, `AGENTS.md`, and `CLAUDE.md`, and SHALL exclude the contents of each Studio_Project under the Projects_Root, including the `LightApp` directory name and the Gradle-mandated file names, from this naming rule.
7. THE Harness SHALL hold the Catalog_Roadmap as exactly one Markdown document inside the Ideas_Library rather than as a top-level directory.
8. IF the Gradle debug build of the moved `LightApp` project exits with a non-zero status, THEN THE Harness SHALL report the failing Gradle task and the path of each file requiring a location correction, and SHALL retain the pre-move project content unchanged until a debug build exits with status zero.
9. IF a Harness-owned top-level directory is absent, or the Workspace_Root contains a Markdown file whose name is not exactly one of `README.md`, `AGENTS.md`, or `CLAUDE.md`, or a Harness-owned directory or Markdown file name violates the naming rule of criterion 6, THEN THE Conformance_Checker SHALL report the offending path and SHALL exit with a non-zero status.
10. THE Workspace_Root SHALL contain exactly three Markdown entries — the regular file `README.md` for human orientation, the regular file `AGENTS.md` as the Agent_Entry_Document, and `CLAUDE.md` as an Agent_Adapter_Link resolving to `AGENTS.md` — and THE content, structure, and conformance rules for the Agent_Entry_Document and every Agent_Adapter_Link SHALL be those stated in Requirement 17 rather than in this requirement.
11. THE Spec_Library SHALL contain one child directory per feature spec and no other child, each child directory holding a requirements document, a design document, and a tasks document as plain-Markdown files carrying no agent-vendor-specific markup, and THE Harness SHALL provide, for each such child directory, a symbolic link at `.kiro/specs/<feature>` whose resolved target is the Spec_Library child directory whose name matches `<feature>` by exact, case-sensitive comparison, so that Kiro and every other coding agent read the same files.
12. IF a `.kiro/specs/<feature>` entry is absent for a Spec_Library child directory, or exists as a regular directory rather than a symbolic link, or resolves to any path other than the Spec_Library child directory whose name matches `<feature>` by exact, case-sensitive comparison, or resolves to no existing path, THEN THE Conformance_Checker SHALL report the link path, the resolved target path or an indication that the target is absent, and the expected Spec_Library child directory path, and SHALL exit with a non-zero status.

### Requirement 2: Canonical Design Token Source

**User Story:** As the sole developer, I want one authoritative definition of every design token, so that a color or spacing value is never invented twice.

#### Acceptance Criteria

1. THE Harness SHALL hold the Token_Source as exactly one machine-readable file at `design-canon/tokens.json`, carrying one entry per Design_Token, each entry declaring the fixed fields token name, `category`, `job`, `why`, and either a single `value` field or the pair of `dark` and `light` fields, where `category` is exactly one of color, type style, spacing step, corner radius, stroke, opacity, motion duration, or easing, and where the presence and non-emptiness of the `job` and `why` fields are validated by the Token_Validation_Suite of Requirement 18.
2. THE Token_Source SHALL record each Design_Token value in the unit fixed for its category: a color as a 6-digit hexadecimal RGB value with an optional alpha as an integer percent from 0 to 100, a spacing step, a corner radius, and a stroke width in dp, an opacity as a decimal fraction between 0 and 1, a type style size and line height in sp with letter spacing in em, a motion duration in ms, and an easing as a four-parameter cubic Bézier.
3. WHERE a Design_Token holds the same value in dark mode and light mode, THE Token_Source SHALL record that token with a single `value` field and SHALL omit the `dark` and `light` fields, and WHERE a Design_Token of category color differs between modes, THE Token_Source SHALL record both the `dark` and the `light` field and SHALL omit the `value` field, so that no token requires two values to be restated.
4. THE Design_Canon SHALL record one naming rule that maps a token name in the Token_Source to its Kotlin constant name in the Generated_Tokens, such that the expected constant name for any token name is determined by that rule alone.
5. WHEN a Gradle build of a Studio_Project runs, THE token generator task SHALL read the Token_Source and write the Generated_Tokens as a Kotlin file under that project's `build/generated/` directory, declaring exactly one Kotlin constant for every Token_Source entry named according to the naming rule of criterion 4, declaring no constant that the Token_Source does not name, completing within 5 seconds on the Reference_Machine, and completing before any Kotlin source that consumes a Design_Token is compiled.
6. THE token generator task SHALL write the Generated_Tokens with a leading header stating that the file is generated from the Token_Source, that it is not committed, and that hand edits do not survive a build, and THE Harness SHALL exclude the `build/generated/` directory of every Studio_Project and of the Project_Template from version control, so that the Generated_Tokens exists only as a build output.
7. IF a file whose path lies under a `build/generated/` directory is tracked in version control, or a committed Kotlin source file outside `build/generated/` declares a Design_Token constant named by the naming rule of criterion 4, THEN THE Conformance_Checker SHALL report the file path and, for a declared constant, its constant name and line number, for every such file found in that single run, and SHALL exit with a non-zero status.
8. WHEN the token generator task runs and a file already present at the Generated_Tokens path differs in any byte from the content the generator derives from the Token_Source, THE token generator task SHALL replace that file in full and SHALL report the replaced path, so that a hand edit to the Generated_Tokens is discarded on the next build rather than propagated.
9. IF the Token_Source cannot be parsed as machine-readable entries, or an entry omits any fixed field of criterion 1, or an entry declares a `value` field together with a `dark` or `light` field, or a recorded value is not expressed in the unit fixed for its category by criterion 2, THEN THE token generator task SHALL report the offending entry together with the missing or malformed field, SHALL leave no partial or stale Generated_Tokens file at the generated path, SHALL NOT report generation as succeeded, and SHALL fail the build with a non-zero status.
10. THE Harness SHALL author every design value only in the Token_Source, and IF a committed file under a Studio_Project, under the Project_Template, or under the Design_Canon states a 6-digit hexadecimal color value, a dp value, an sp value, an em value, or a ms value as the definition of a Design_Token, THEN THE Conformance_Checker SHALL report the file path, the line number, and the stated value for every such occurrence found in that single run and SHALL exit with a non-zero status, excepting a value carrying the Suppression_Annotation of Requirement 15 and excepting a Design_Canon document that names a Design_Token by name without stating its value.
11. WHEN the token generator task runs, THE token generator task SHALL additionally emit the Design_Token block that the Agent_Entry_Document restates, holding at most the 40 token values permitted by Requirement 17 criterion 10, and IF the corresponding block in the Agent_Entry_Document differs from the emitted block, THEN THE Conformance_Checker SHALL report the Agent_Entry_Document line number, the restated value, and the emitted value for every disagreement in that single run, and SHALL exit with a non-zero status.
12. THE Design_Canon SHALL record, for every third-party source it draws from, the source name, the source URL, and the licence, and SHALL name the GPL-3.0 obligation attached to the vendored QQWing engine.

### Requirement 3: Project Bootstrap From The Harness

**User Story:** As the sole developer, I want a new project to start from the harness rather than from a blank Android Studio template, so that a second app inherits the design system and the constraints on day one.

#### Acceptance Criteria

1. THE Harness SHALL provide a Project_Template containing the Theme_Package, the Component_Library, the Studio_Shell skeleton, the GameDefinition contract, the Game_Registry, the font resources, and the Conformance_Checker configuration, with every listed item present as a non-empty file or package, with the Game_Registry declaring zero registered GameDefinition entries, and with zero Game_Module directories.
2. WHEN a new Studio_Project is bootstrapped from the Project_Template and no game-specific code has been added, THE new Studio_Project SHALL compile its release build with zero build errors within 10 minutes of wall-clock time on the Reference_Machine, and THE Conformance_Checker SHALL exit with status zero when run against that Studio_Project.
3. THE Harness SHALL document the bootstrap procedure as an ordered step list of 20 steps or fewer, with each step stating exactly one command or exactly one file edit together with the observable result that confirms the step succeeded, and with the whole procedure completable within 30 minutes of wall-clock time on the Reference_Machine excluding first-time Gradle and SDK downloads.
4. WHERE a Studio_Project needs a design value that no Design_Token provides, THE bootstrap procedure SHALL direct the developer to add the token to the Design_Canon and to the Theme_Package in the same change, and SHALL state that the value is not written as a literal in screen code.
5. THE Project_Template SHALL declare `minSdk` 26 or higher, `compileSdk` 35, `targetSdk` 35, and R8 minification enabled for the release build type, where the `minSdk` floor of 26 is set by the API level at which Compose applies `FontVariation` settings, on which the variable dot-size axis of the display face depends.
6. THE bootstrap procedure SHALL list, as one step each, the rewrite of the Gradle `namespace`, the release application id, the launcher app label, the root Kotlin package directory, and the Gradle root project name to the identifiers of the new Studio_Project.
7. IF a bootstrapped Studio_Project retains a Project_Template placeholder value in its Gradle `namespace`, its application id, its launcher app label, its root Kotlin package directory, or its Gradle root project name, THEN THE Conformance_Checker SHALL report each retained placeholder with its file path and SHALL exit with a non-zero status.
8. WHEN a Design_Token, a Component_Library component, a GameDefinition contract member, or a font resource changes in the `LightApp` Studio_Project, THE Project_Template copy of that item SHALL be updated in the same change, and THE Conformance_Checker SHALL apply its structure, token, and Anti_Requirement checks to the Project_Template as it does to a Studio_Project.

### Requirement 4: Harness Baseline Conformance

**User Story:** As the sole developer, I want the offline, zero-dependency, low-footprint promises checked mechanically, so that they cannot erode as the catalog grows.

#### Acceptance Criteria

1. THE Conformance_Checker SHALL be runnable from the command line as a single command invoked from the Workspace_Root, and SHALL exit with status zero when no violation is found and with a non-zero status when one or more violations are found.
2. IF a Studio_Project manifest declares one or more `uses-permission` elements, THEN THE Conformance_Checker SHALL report the manifest path and the permission name of every such element, and SHALL exit with a non-zero status.
3. IF a Studio_Project build script declares a direct dependency whose coordinate matches an entry in the Dependency_Denylist, where a match is a case-insensitive comparison of the coordinate's group and module segments against the entry with the version segment ignored, and where an entry naming a group id or a group/module prefix matches every coordinate that begins with that entry followed by `.`, `:`, or the end of the coordinate, THEN THE Conformance_Checker SHALL report the dependency coordinate, the matched Dependency_Denylist entry, and the path of the declaring build script, and SHALL exit with a non-zero status.
4. IF a Studio_Project contains a file under `res/` or `assets/` whose extension, compared case-insensitively, is a raster image extension (`.png`, `.jpg`, `.jpeg`, `.gif`, `.bmp`, `.webp`, `.heic`, `.heif`, `.tif`, `.tiff`, `.ico`, `.psd`) or an audio extension (`.mp3`, `.wav`, `.ogg`, `.oga`, `.m4a`, `.aac`, `.flac`, `.opus`, `.mid`, `.midi`, `.wma`, `.3gp`), THEN THE Conformance_Checker SHALL report the file path and the matched extension, and SHALL exit with a non-zero status.
5. THE Studio_Project SHALL declare its application id through the Gradle `namespace` property alone.
6. IF a Studio_Project manifest declares the deprecated `package` attribute, THEN THE Conformance_Checker SHALL report the manifest path and SHALL exit with a non-zero status, including the case where the Gradle `namespace` property is also declared.
7. THE Harness SHALL record the Anti_Requirements as a single list in the Ideas_Library, and THE Conformance_Checker SHALL state, for each Anti_Requirement, whether the check is automated or manual.
8. WHEN THE Conformance_Checker detects a violation, THE Conformance_Checker SHALL record the violation together with the identifier or file path that triggered it, and SHALL continue every remaining check to completion instead of exiting at the first violation.
9. THE Conformance_Checker SHALL report structure, token, and Anti_Requirement violations independently of whether the Studio_Project compiles, and SHALL end each run with a report that lists every recorded violation and states the total count of violations found in that run.
10. THE Conformance_Checker SHALL complete a full run, measured from command invocation to process exit and including dependency resolution and the reporting of every violation, within 60 seconds on the Reference_Machine.
11. THE Harness SHALL record the Dependency_Denylist as a single list in the Design_Canon, in which each entry states one exact `group:module` coordinate, one group id, or one group/module prefix, together with the prohibited category it belongs to, drawn from networking, advertising, analytics, and crash reporting, and THE Dependency_Denylist SHALL hold at least one entry for each of those four categories.
12. IF a coordinate in the resolved dependency graph of a Studio_Project matches an entry in the Dependency_Denylist while not being declared directly by a Studio_Project build script, THEN THE Conformance_Checker SHALL report the transitive coordinate, the matched Dependency_Denylist entry, and the direct dependency that introduced it, and SHALL exit with a non-zero status.
13. THE Conformance_Checker SHALL exempt from the raster image and audio check every file whose extension, compared case-insensitively, is `.otf` or `.ttf` under `res/font/` and every file whose extension, compared case-insensitively, is `.xml` under `res/`, so that a Studio_Project whose only such files are the existing font files under `res/font/` and the vector and adaptive-icon XML resources under `res/mipmap/` records no violation from that check.

### Requirement 5: Documents Stay In Step With Code

**User Story:** As the sole developer, I want the architecture and design documents to describe what the code actually does, so that the documents remain trustworthy after months away.

#### Acceptance Criteria

1. THE Design_Canon SHALL list every component of the Component_Library as one entry per component, each entry naming the component, the source file that declares it, and every Design_Token the component consumes by token name drawn from the Design_Canon token list.
2. IF a component file named in the Design_Canon component list is absent from the Component_Library, THEN THE Conformance_Checker SHALL report the component name together with the expected file name for every such component found in the run, and SHALL exit with a non-zero status.
3. IF a component file exists in the Component_Library that the Design_Canon component list does not name, THEN THE Conformance_Checker SHALL report the file name and the component name it declares, and SHALL exit with a non-zero status.
4. IF a Game_Module directory exists in a Studio_Project that the Catalog_Roadmap does not list, THEN THE Conformance_Checker SHALL report the Game_Module identifier and SHALL exit with a non-zero status.
5. THE Catalog_Roadmap SHALL record, for each accepted game, an implementation status of exactly one of `planned`, `in_progress`, or `implemented`, independent of its tier assignment, and THE Conformance_Checker SHALL require a corresponding Game_Module only for an accepted game recorded as `implemented`.
6. IF an accepted game recorded as `implemented` has no corresponding Game_Module directory in a Studio_Project, THEN THE Conformance_Checker SHALL report the game identifier and SHALL exit with a non-zero status.
7. THE Harness SHALL mark each Design_Canon and Ideas_Library document with a `status` field in a metadata block at the top of the document above the first heading, holding exactly one of `canonical`, `research`, or `superseded`, and THE Conformance_Checker SHALL report the path of any such document whose `status` field is absent or holds any other value and SHALL exit with a non-zero status.
8. IF a document `status` field holds `superseded`, THEN THE document SHALL name its replacement in a `superseded-by` field in the same metadata block, and THE Conformance_Checker SHALL report the document path when that field is absent or names a document that does not exist in the Harness, and SHALL exit with a non-zero status.

### Requirement 6: Game Catalog Admission Criteria

**User Story:** As the sole developer, I want written criteria for what qualifies as a studio game, so that scope stays inside the offline and low-footprint envelope.

#### Acceptance Criteria

1. THE Catalog_Roadmap SHALL state, for each admission criterion, the criterion name, its measurable threshold, and the observable check that decides pass or fail, covering offline operation with no network access, absence of raster image and audio assets, monochrome compatibility, grid or text based presentation rendered through Component_Library components, embedded data size, runtime memory, puzzle validity, and session length.

2. THE Catalog_Roadmap SHALL admit only candidate games that operate with zero declared Android permissions, counting permissions merged into the manifest from the candidate game's dependencies.
3. THE Catalog_Roadmap SHALL admit only candidate games whose embedded data totals 512 KB or less uncompressed, measured as the sum of every data file, word list, and generated lookup table the Game_Module contributes to the release artifact.
4. THE Catalog_Roadmap SHALL admit only candidate games whose puzzle generation for the default difficulty completes within 2 seconds on the Reference_Machine, measured as the slowest of 10 consecutive generations, where the default difficulty is the difficulty the Game_Module selects when a player starts a game without choosing one.
5. THE Catalog_Roadmap SHALL admit only candidate games that render through the Theme_Package and the Component_Library without adding any dependency coordinate beyond those declared by the Project_Template.
6. IF a candidate game fails any admission criterion, THEN THE Catalog_Roadmap SHALL record the candidate game as rejected together with the name of the criterion it failed and the measured value beside that criterion's threshold, and SHALL NOT assign the candidate game a tier.
7. THE Catalog_Roadmap SHALL record at least three rejected candidate games, each naming a different failing criterion, so that the boundary of the catalog is legible.
8. THE Catalog_Roadmap SHALL admit only candidate games whose resident memory stays at or below 64 MB throughout a 10 minute continuous play session of the default difficulty on the Reference_Machine emulator.
9. THE Catalog_Roadmap SHALL admit only candidate games whose generator produces, for every difficulty it offers, puzzles that have at least one solution satisfying the stated win condition and that are solvable from the presented start state by deduction alone.
10. THE Catalog_Roadmap SHALL admit only candidate games whose default difficulty round has a stated target completion time between 2 and 10 minutes, and SHALL record that target for each accepted game.
11. THE Catalog_Roadmap SHALL admit only candidate games that remain playable using the Text_Hierarchy, surface, and border tokens alone, with no colour carrying game meaning, so that a mechanic depending on colour discrimination is rejected regardless of its market performance.

### Requirement 7: Tiered Game Rollout

**User Story:** As the sole developer, I want the catalog ordered into tiers, so that I ship one polished game before spreading effort across four.

#### Acceptance Criteria

1. THE Catalog_Roadmap SHALL assign every accepted game to exactly one tier numbered 1, 2, or 3, and SHALL record for each accepted game exactly one rollout status drawn from `planned`, `in_progress`, and `implemented`.
2. THE Catalog_Roadmap SHALL assign Sudoku to tier 1 as the only tier 1 game, and SHALL record the vendored QQWing engine as its generator.
3. THE Catalog_Roadmap SHALL assign Nonogram and Minesweeper to tier 2 and Numberlink and Word Search to tier 3, and SHALL state the reason for each tier assignment in one sentence of 200 characters or fewer.
4. THE Catalog_Roadmap SHALL state, for tier 2 and tier 3, an entry condition composed of two observable facts: every accepted game of the preceding tier is recorded with status `implemented`, and the most recent Conformance_Checker run on the Studio_Project containing those games exited with status zero.
5. THE Catalog_Roadmap SHALL state that tier 1 has no preceding tier and that its entry condition is satisfied once Sudoku is recorded as an accepted game under the Requirement 6 admission criteria.
6. THE Catalog_Roadmap SHALL record at most one accepted game with status `in_progress` across all tiers at any one time, so that games within a tier are worked on strictly one at a time.
7. IF the Catalog_Roadmap records a game of tier 2 or tier 3 with status `in_progress` or `implemented` while any accepted game of the preceding tier is not recorded as `implemented`, THEN THE Conformance_Checker SHALL report the game identifier, its tier, and the preceding-tier game that is not `implemented`, and SHALL exit with a non-zero status.
8. IF the Catalog_Roadmap records two or more accepted games with status `in_progress`, THEN THE Conformance_Checker SHALL report every such game identifier and SHALL exit with a non-zero status.
9. THE Catalog_Roadmap SHALL state, for each accepted game, at least one shared harness capability that the game exercises, drawn from the Studio_Shell, the Component_Library, the persistence layer, and the Theme_Package.
10. WHERE two accepted games exercise the same harness capability, THE Catalog_Roadmap SHALL name the Component_Library, Studio_Shell, persistence layer, or Theme_Package component that both games reuse.
11. THE Catalog_Roadmap SHALL record, for each accepted game, its grid model, its input model, and its win condition, each on one line of 200 characters or fewer.

### Requirement 8: Color And Surface Hierarchy

**User Story:** As a player, I want a calm monochrome surface hierarchy, so that the puzzle is the only thing competing for attention.

#### Acceptance Criteria

1. THE Theme_Package SHALL declare the dark-mode background as `#000000`, the elevated surface as `#111111`, the raised surface as `#1A1A1A`, the subtle border as `#222222`, and the visible border as `#333333`, as five separately named Design_Tokens.
2. THE Theme_Package SHALL declare the light-mode background as `#F5F5F5`, the elevated surface as `#FFFFFF`, the raised surface as `#F0F0F0`, the subtle border as `#E8E8E8`, and the visible border as `#CCCCCC`, as five separately named Design_Tokens.
3. THE Theme_Package SHALL express elevation through the surface color tokens alone, with shadow elevation set to exactly 0dp on every surface in both dark mode and light mode.
4. IF a Compose modifier in a Studio_Project applies a gradient brush, a blur, or a shadow elevation greater than 0dp, THEN THE Conformance_Checker SHALL report the file path and line and SHALL exit with a non-zero status.
5. THE Studio_Project SHALL render every screen through the Theme_Package theme wrapper.
6. IF a `setContent` block in a Studio_Project applies a bare `MaterialTheme` rather than the Theme_Package theme wrapper, THEN THE Conformance_Checker SHALL report the file path and line and SHALL exit with a non-zero status.
7. WHEN a Studio_Project starts and the system dark-mode preference cannot be read, THE Theme_Package SHALL apply the dark-mode token set as the default and SHALL keep that mode for the remainder of the session unless the system preference becomes readable.
8. THE Theme_Package SHALL declare each Text_Hierarchy color token at a contrast ratio of at least 4.5:1 against the background, elevated surface, and raised surface tokens of the same mode, except the disabled level, which SHALL reach at least 3:1, and THE Conformance_Checker SHALL report any token pair whose computed ratio falls below its threshold, naming both token names and the computed ratio, and SHALL exit with a non-zero status.

### Requirement 9: Four-Level Text Hierarchy And Font Roles

**User Story:** As a player, I want type alone to carry the hierarchy, so that screens stay legible without color coding or boxes.

#### Acceptance Criteria

1. THE Theme_Package SHALL declare exactly four Text_Hierarchy color tokens per color mode, valued `#FFFFFF` for display, `#E8E8E8` for primary, `#999999` for secondary, and `#666666` for disabled in dark mode, and valued `#000000` for display, `#1A1A1A` for primary, `#666666` for secondary, and `#8A8A8A` for disabled in light mode, where the light-mode disabled value is the darkest grey clearing a 3:1 contrast ratio against every light-mode surface token.
2. THE Theme_Package SHALL assign the Doto face to display and hero numeral type styles, the Geist Sans face to body and UI type styles, and the Geist Mono face to label, timer, and data type styles, per the Font_Role_System.
3. THE Studio_Project SHALL bundle no font file that is not licensed under the SIL Open Font License or an equivalent grant permitting commercial redistribution, and THE Conformance_Checker SHALL report any font resource absent from the Design_Canon provenance list or recorded there without such a licence, and SHALL exit with a non-zero status.
4. THE Theme_Package SHALL apply the Doto face only to type styles of 36sp or larger, and SHALL apply no dot-matrix face to any type style below 36sp, matching the Design_Canon rule that the dot-matrix face is reserved for 36sp and above.
5. THE Theme_Package SHALL declare every label type style with the Geist Mono face, all-caps rendering, a size of 11sp or 12sp, and letter spacing between 0.06em and 0.10em inclusive, expressed in em units rather than sp units.

6. THE Studio_Project SHALL render each screen with at most three distinct type sizes and at most two distinct font weights, counting the game grid numeral style as one of the three type sizes and excluding in-cell pencil-mark styles from the count.
7. THE Studio_Project SHALL render at most one display-level text element per screen, where a display-level text element is one rendered with a type style of 36sp or larger drawn from a Design_Canon display scale step, and where game grid numerals below 36sp are not display-level.
8. THE Theme_Package SHALL declare exactly one type style for every scale step named in the Design_Canon type scale, each type style stating its font family, its size in sp, its line height, and its letter spacing.
9. IF a screen composable of a Studio_Project references more than three distinct Theme_Package type sizes or more than two distinct font weights, THEN THE Conformance_Checker SHALL report the file path and the referenced type style names, and SHALL exit with a non-zero status.
10. IF a screen composable of a Studio_Project references a display-level type style more than once, THEN THE Conformance_Checker SHALL report the file path and the number of display-level references, and SHALL exit with a non-zero status.
11. THE Theme_Package SHALL declare every type style carrying a numeral that updates in place, including timers, counters, scores, and grid numerals, with tabular figures enabled, so that digit width does not change as the value changes.

### Requirement 10: Shape Language

**User Story:** As a player, I want rounded, instrument-like geometry throughout, so that the app feels like one designed object.

#### Acceptance Criteria

1. THE Theme_Package SHALL declare exactly three corner radius tokens: a pill token of 999dp, a card token of 16dp, and a grid cell token of 8dp.
2. THE Component_Library SHALL render every button with the pill corner radius token, producing a corner radius equal to half the rendered button height for any button height between 48dp and 64dp.
3. THE Theme_Package SHALL declare no card corner radius token greater than 16dp.
4. IF a Compose call in a Studio_Project applies `RectangleShape`, a `RoundedCornerShape` of 0dp, or a card corner radius greater than 16dp to a visible surface, THEN THE Conformance_Checker SHALL report the file path and line and SHALL exit with a non-zero status, and SHALL treat as compliant any surface that fills the full window width and height, including the Dot_Matrix_Texture canvas layer and full-bleed screen backgrounds, when the declaration carries the Suppression_Annotation defined in Requirement 15.
5. THE Component_Library SHALL render Glyph dot markers as circles of 4dp, 6dp, or 8dp diameter drawn from the marker size token set, with no border stroke.
6. THE Component_Library SHALL render every icon as a monoline stroke of 1.5dp on a 24dp by 24dp base with no fill, inheriting the Text_Hierarchy color token in effect at the call site.
7. THE Component_Library SHALL render every interactive element, including pill buttons and icon buttons, with a touch target measuring at least 48dp by 48dp, extending the touch target beyond the drawn bounds where the drawn element is smaller than 48dp in either dimension.
8. THE Component_Library SHALL express 3x3 box grouping in a 9x9 grid through the 8dp spacing token between adjacent boxes and the visible border color token on each box outline, and SHALL render every cell in the grid with the grid cell corner radius token.

### Requirement 11: Spacing Grid

**User Story:** As the sole developer, I want every gap to come from a named step on a 4dp grid, so that alignment is consistent without measuring screenshots.

#### Acceptance Criteria

1. THE Theme_Package SHALL declare exactly eight spacing tokens, with the values 4dp, 8dp, 16dp, 24dp, 32dp, 48dp, 64dp, and 96dp, and SHALL declare no further spacing token.
2. THE Theme_Package SHALL declare exactly one optical adjustment token of 2dp, and THE Studio_Project SHALL apply that token only to optical alignment offsets and to no padding, gap, or margin.
3. THE Studio_Project SHALL express every padding, every gap, every margin, and every fixed element width or height as a spacing token or the optical adjustment token from the Theme_Package.
4. IF a Compose call outside the Theme_Package supplies a `dp` literal that does not carry the Suppression_Annotation defined in Requirement 15, THEN THE Conformance_Checker SHALL report the file path, the line number, and the literal value, and SHALL exit with a non-zero status.
5. THE Studio_Project SHALL separate every pair of adjacent element groups with a spacing token of 32dp or larger.
6. WHERE an element dimension scales with the available space, THE Studio_Project SHALL compute that dimension from the measured container size at composition time without a `dp` literal, and THE Conformance_Checker SHALL treat such a computed dimension as conforming.
7. THE Component_Library SHALL mark every `dp` literal that Requirement 10 or Requirement 12 mandates, namely the 1.5dp icon stroke, the 4dp to 8dp Glyph dot diameter, and the 1dp to 2dp Dot_Matrix_Texture dot diameter, with the Suppression_Annotation defined in Requirement 15, naming the requirement that mandates the value.
8. IF a Compose call in a Studio_Project renders a divider line or a horizontal or vertical rule between two adjacent element groups, THEN THE Conformance_Checker SHALL report the file path and the line number and SHALL exit with a non-zero status.

### Requirement 12: Dot-Matrix Glyph Texture

**User Story:** As a player, I want a barely-there dot texture that recalls the Glyph interface, so that empty space has character without becoming noise.

#### Acceptance Criteria

1. THE Component_Library SHALL provide the Dot_Matrix_Texture component, drawn with the Compose Canvas API, with no image asset and no raster resource.
2. THE Dot_Matrix_Texture SHALL draw dots whose diameter equals the 2dp optical adjustment token of the Theme_Package, on a uniform square grid whose row pitch and column pitch both equal either the 16dp or the 24dp spacing token declared by the Theme_Package, and SHALL use no pitch value absent from that spacing token scale.
3. WHILE the Studio_Project renders in dark mode, THE Dot_Matrix_Texture SHALL draw every dot in the dark-mode visible border token `#333333` at an alpha between 0.10 and 0.20 inclusive.
4. WHILE the pixel width and the pixel height of the Dot_Matrix_Texture drawing bounds are unchanged, THE Dot_Matrix_Texture SHALL draw its retained dot positions in a single Canvas pass of at most 5,000 dots and SHALL NOT recompute dot positions on recomposition.
5. WHILE the Dot_Matrix_Texture fills the full display bounds at its densest permitted configuration of 2dp dots on the 16dp pitch at 0.20 alpha, THE Studio_Project SHALL render at least 99 percent of frames within 16.7ms and no frame beyond 33.4ms, measured over a continuous 10-second window on the Reference_Machine emulator.
6. WHILE a device sustains fewer than 60 frames per second, THE Studio_Project SHALL keep the Dot_Matrix_Texture visible at its configured pitch, diameter, and alpha, and SHALL NOT hide, thin, or fade the texture.
7. THE Component_Library SHALL expose the Dot_Matrix_Texture only as a background layer composed behind content, SHALL NOT expose it as a border or button styling parameter, and THE Design_Canon SHALL record that dots serve neither as container borders nor as button styling.
8. WHEN the pixel width or the pixel height of the Dot_Matrix_Texture drawing bounds changes, THE Dot_Matrix_Texture SHALL recompute its dot positions exactly once for that size and SHALL retain those positions for every subsequent draw at that size.
9. WHILE the Studio_Project renders in light mode, THE Dot_Matrix_Texture SHALL draw every dot in the light-mode visible border token `#CCCCCC` at an alpha between 0.10 and 0.20 inclusive, and SHALL substitute no Text_Hierarchy token and no Red_Accent.
10. THE Dot_Matrix_Texture SHALL pass every pointer event occurring within its bounds to the content composed above it, SHALL consume no pointer event, and SHALL expose no accessibility node.

### Requirement 13: Motion Rules

**User Story:** As a player, I want calm mechanical transitions, so that the app feels precise rather than playful.

#### Acceptance Criteria

1. THE Theme_Package SHALL declare exactly one micro-interaction motion duration token valued 150ms and exactly one screen transition motion duration token valued 300ms, and SHALL declare no other motion duration token.
2. THE Theme_Package SHALL declare exactly one easing token equivalent to `cubic-bezier(0.25, 0.1, 0.25, 1)`, and THE Studio_Project SHALL specify every animation with that easing token and with either the micro-interaction or the screen transition duration token.
3. THE Studio_Project SHALL animate state changes through opacity transitions and through Text_Hierarchy, surface, or border color token transitions alone, and SHALL NOT animate element position, size, scale, or rotation.
4. IF an animation specification in a Studio_Project uses a spring, a bounce, or an overshoot easing, specifies a duration other than 150ms or 300ms, or animates element position, size, scale, or rotation, THEN THE Conformance_Checker SHALL report the file path, the line, and the offending easing, duration, or animated property, and SHALL exit with a non-zero status.
5. WHEN a player presses an interactive element, THE Component_Library SHALL change that element's border color token or text color token to the adjacent brighter Text_Hierarchy level and SHALL complete that change within 150ms of touch-down.
6. THE Component_Library SHALL respond to a press by changing only the pressed element's opacity, border color token, or text color token, without changing its scale, size, position, or hue.
7. IF the operating system reduced-motion accessibility setting is enabled, THEN THE Studio_Project SHALL apply the end state of every state change and screen transition without intermediate animation frames, and SHALL present the same end state as when the setting is disabled.
8. THE Studio_Project SHALL animate at most 8 elements simultaneously on one screen.
9. WHILE 8 elements are animating simultaneously on one screen, THE Studio_Project SHALL sustain a rendering rate of 60 frames per second on the Reference_Machine emulator.

### Requirement 14: Single Red Accent Rule

**User Story:** As a player, I want red to mean something, so that an alert reads instantly instead of blending into decoration.

#### Acceptance Criteria

1. THE Theme_Package SHALL declare the Red_Accent as `#D71921` and its subtle tint as `#D71921` at 15 percent alpha.
2. THE Studio_Project SHALL apply the Red_Accent to zero elements on a screen presenting no error, destructive, or urgent state, and to at most one element otherwise, where an element is one contiguous visual unit: one text run, one monoline icon, one border outline, or one grid cell.
3. THE Studio_Project SHALL apply the Red_Accent only to error states, destructive actions, and urgent states, and SHALL render every other element with the Text_Hierarchy, surface, and border tokens.
4. WHILE two or more error, destructive, or urgent states are present on one screen, THE Studio_Project SHALL apply the Red_Accent to the single element of highest priority in the order destructive action first, then error state, then urgent state, resolving equal priority by the most recently interacted element and, where no interaction has occurred, by reading order from top to bottom and from leading to trailing, and SHALL render the remaining states with the secondary Text_Hierarchy token.
5. WHILE no error, destructive, or urgent state is present, THE Studio_Project SHALL render the screen using the Text_Hierarchy, surface, and border tokens alone.
6. WHEN the Studio_Project applies the Red_Accent to a data value, THE Studio_Project SHALL apply the Red_Accent to the value text run alone and SHALL apply the secondary Text_Hierarchy token to the label of that value.
7. THE Theme_Package SHALL exclude the Red_Accent from the Text_Hierarchy token set, SHALL assign the Red_Accent to no type style, and SHALL apply the Red_Accent only to border, outline, and shape fill roles, because the Red_Accent measures below 4.5:1 against every surface token and therefore cannot carry normal-size text as a type colour. WHILE a shape fill applies the Red_Accent, the fill SHALL carry only white `textDisplay` content, which measures 5.19:1 against `#D71921`.
8. THE Studio_Project SHALL render every error, destructive, or urgent element with at least one non-color indicator, being a monoline icon per Requirement 10, a text label naming the state, a visible-border outline, or a whole-element shape fill that a grayscale render pass distinguishes from its neighbours, such that a grayscale render pass of the screen distinguishes that element from its non-error, non-destructive, non-urgent neighbours.
9. IF a Kotlin source file outside the Theme_Package and the Component_Library references the Red_Accent token or its subtle tint, THEN THE Conformance_Checker SHALL report the file path and the line and SHALL exit with a non-zero status.
10. WHILE two or more cells of a Sudoku 9x9 grid are in conflict at the same time, THE Studio_Project SHALL apply the Red_Accent to the most recently entered conflicting cell alone, SHALL render every remaining conflicting cell with the secondary Text_Hierarchy token together with its non-color indicator, and SHALL select the first conflicting cell in reading order from top to bottom and from leading to trailing when no conflicting cell has been entered by the player.
11. WHILE a screen presents no error state, no destructive action, and no urgent state, including the Sudoku grid in normal play with no cell in conflict, THE Studio_Project SHALL render that screen with zero Red_Accent elements, so that the absence of the accent is the normal condition rather than a budget left unspent.

### Requirement 15: Structural Token Prevention

**User Story:** As the sole developer, I want design values impossible to hardcode, so that the design language holds across every game without review discipline.

#### Acceptance Criteria

1. THE Studio_Project SHALL be composed of exactly one Design_System_Module — the Gradle module named `:design-system`, holding the Theme_Package and the Component_Library — together with one or more Consumer_Modules — Gradle modules that render user interface by consuming the Design_System_Module, namely the application module `:app` plus one `:games:<id>` module for each accepted game the Catalog_Roadmap assigns to tier 2 — and SHALL declare every dependency between the two kinds of module in one direction only, from a Consumer_Module to the Design_System_Module.
2. THE Design_System_Module SHALL declare every Compose UI artifact it depends on with the Gradle `implementation` configuration rather than the `api` configuration, and THE Harness SHALL record that classpath removal of the three forbidden Compose types `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.unit.Dp`, and `androidx.compose.ui.unit.TextUnit` is **not achievable** for a Consumer_Module that renders any Compose user interface, because `androidx.compose.ui:ui` exposes `ui-graphics` and `ui-unit` as `api` dependencies and therefore any use of `Modifier` places all three forbidden types on the compile classpath.
3. THE Studio_Project SHALL enforce the forbidden-type prohibition through a static analysis rule bound to the Gradle `check` task, and IF a Kotlin source file of a Consumer_Module references any of the three forbidden Compose types named in criterion 2 without a valid Suppression_Annotation, THEN THE Gradle build SHALL fail with a non-zero status, reporting the source file path, the line number, and the forbidden type for every such reference found in that single run.
4. THE Design_System_Module SHALL expose design values across the module boundary only through the three inline value classes `Spacing`, `Radius`, and `Duration` that it declares, and its public API SHALL declare no parameter type, no return type, and no public property type that is one of the three forbidden Compose types named in criterion 2.
5. IF a Consumer_Module call site supplies a raw Compose value where the Design_System_Module public API declares a `Spacing`, a `Radius`, or a `Duration` parameter, THEN THE Gradle build SHALL fail Kotlin compilation with a type-mismatch error naming the source file path, the line number, the expected value class, and the supplied type, SHALL produce no build artifact for that Consumer_Module, and SHALL exit with a non-zero status.
6. THE Studio_Project SHALL provide one architecture test asserting that no class declared in any Consumer_Module imports any of the three forbidden Compose types named in criterion 2, that no Consumer_Module is depended upon by the Design_System_Module, and that the Design_System_Module public API declares none of the three forbidden types, and THE architecture test SHALL be bound to the Gradle `check` task and SHALL complete within 60 seconds on the Reference_Machine.
7. IF the architecture test of criterion 6 finds a Consumer_Module class importing one of the three forbidden Compose types, THEN THE architecture test SHALL report the fully qualified name of the violating class together with the forbidden import for every such class found in that single run, and SHALL exit with a non-zero status.
8. IF a Consumer_Module build script declares a Compose UI artifact dependency, or THE Design_System_Module build script declares a Compose UI artifact with the `api` configuration, or THE Design_System_Module build script declares a dependency on a Consumer_Module, THEN THE Conformance_Checker SHALL report the build script path, the declared coordinate, and the declared Gradle configuration for every such declaration found in that single run, and SHALL exit with a non-zero status.
9. THE Conformance_Checker SHALL retain the literal scan as a defence-in-depth check subordinate to criteria 1 through 8, and SHALL scope that scan to Kotlin source files of the Design_System_Module production source set that lie outside the Theme_Package, excluding every file of a test source set, every file produced by code generation, and every file of every Consumer_Module.
10. IF a Kotlin source file within the scan scope of criterion 9 contains a `Color(0x...)` literal or a named `Color.` constant on a declaration that carries no valid Suppression_Annotation as defined in criterion 13, THEN THE Conformance_Checker SHALL report the file path, the line number, and the literal text for every such occurrence found in that single run, and SHALL exit with a non-zero status.
11. IF a Kotlin source file within the scan scope of criterion 9 contains a `dp` literal or an `sp` literal on a declaration that carries no valid Suppression_Annotation as defined in criterion 13, THEN THE Conformance_Checker SHALL report the file path, the line number, and the literal text for every such occurrence found in that single run, and SHALL exit with a non-zero status.
12. THE Design_System_Module SHALL declare every color resource entry of the Studio_Project in exactly one XML file of its resource values, no Consumer_Module SHALL declare a color resource entry, and THE Conformance_Checker SHALL exempt from every color-entry check the two pre-existing XML mipmap launcher icon resources, scoping that exemption to the `:app` Consumer_Module alone.
13. WHERE a value legitimately falls outside the Design_Token set, THE Design_System_Module SHALL annotate exactly one declaration holding that value with a Suppression_Annotation carrying the identifier of the single rule it suppresses and a reason of 20 to 200 characters, and THE Conformance_Checker SHALL list every active suppression in its report with the file path, the line number, the suppressed rule identifier, the reason, and the total count of active suppressions, so that the `dp` literals mandated by Requirement 10, Requirement 11, and Requirement 12 — the 1.5dp icon stroke, the 4dp to 8dp Glyph dot diameters, and the 2dp Dot_Matrix_Texture dot diameter — remain declarable inside the Design_System_Module.
14. IF a Suppression_Annotation omits the suppressed rule identifier, omits the reason, carries a reason shorter than 20 characters or longer than 200 characters, or is applied at file scope, package scope, or to more than one declaration, THEN THE Conformance_Checker SHALL report it as an undocumented suppression together with its file path and line number, and SHALL exit with a non-zero status.
15. IF the total count of active suppressions in the Design_System_Module exceeds 10, THEN THE Conformance_Checker SHALL report the total count together with every active suppression, and SHALL exit with a non-zero status.
16. IF a color entry declared in an XML resource file of the Studio_Project, other than the two pre-existing XML mipmap launcher icon resources exempted by criterion 12, has a value absent from the Design_Canon token list, THEN THE Conformance_Checker SHALL report the entry name, the file path, and the value for every such entry found in that single run, and SHALL exit with a non-zero status.
17. IF the literal scan of criterion 9 reports zero violations, the architecture test of criterion 6 reports zero violations, every active suppression satisfies criterion 13, and the total count of active suppressions is at or below 10, THEN THE Conformance_Checker SHALL exit with status zero.

### Requirement 16: Prototype Migration Onto The Harness

**User Story:** As the sole developer, I want the existing prototype folded into the harness, so that the first game demonstrates the system instead of contradicting it.

#### Acceptance Criteria

1. THE Studio_Project entry point SHALL render the Studio_Shell inside the Theme_Package theme wrapper, SHALL contain no bare `MaterialTheme` wrapper, and SHALL contain no game-specific composable and no hardcoded 81-cell puzzle array, with the prototype Sudoku composable deleted rather than retained as unreferenced code.
2. WHEN the home screen is displayed, THE Studio_Shell SHALL present one selectable entry for every GameDefinition listed by the Game_Registry, showing the name, description, and icon of that definition, and WHEN a player selects one entry, THE Studio_Shell SHALL navigate to the screen produced by that GameDefinition factory as the single active destination.
3. WHEN the Sudoku Game_Module screen becomes visible, THE SudokuViewModel SHALL obtain an 81-cell puzzle of the default difficulty from the vendored QQWing generator on a thread other than the main thread, SHALL retain that puzzle across configuration change, SHALL present a non-blocking in-progress indication until the puzzle is available, and SHALL read no hardcoded grid.
4. WHEN a generated puzzle is available, THE Sudoku Game_Module SHALL render all 81 cells as a 9-by-9 grid through Component_Library components and Theme_Package tokens, SHALL render given cells and player-entered cells with distinct Text_Hierarchy tokens, and SHALL render no puzzle as a text block or newline-joined string.
5. WHEN a player launches the Studio_Project, THE Studio_Shell SHALL present an interactive home screen within 1 second on the Reference_Machine emulator, without waiting for puzzle generation of any Game_Module to complete.
6. WHEN the Sudoku Game_Module screen is reopened after a process restart, THE Studio_Project SHALL restore from on-device persistence the given cells, the player-entered cells, the per-cell pencil marks, the accumulated play time, the difficulty, and the mistake count of the most recent incomplete puzzle, as separately keyed fields, SHALL restore each given cell with the same value and the same given status it held before the restart, and SHALL NOT restore the undo history.
7. WHEN migration completes, THE Conformance_Checker SHALL exit with status zero for the Studio_Project, reporting no structure, token, or Anti_Requirement violation and no undocumented suppression.
8. IF puzzle generation does not yield a puzzle within 2 seconds on the Reference_Machine, or the generator returns no puzzle, THEN THE Sudoku Game_Module SHALL replace the in-progress indication with an error indication stating that puzzle generation failed, SHALL offer a retry action, SHALL leave any previously persisted puzzle state unchanged, and SHALL keep back navigation to the home screen available.
9. WHEN a player triggers back navigation from a Game_Module screen, THE Studio_Shell SHALL return to the home screen within one screen-transition motion duration token, SHALL persist the in-progress puzzle state before the home screen is displayed, and SHALL leave no Game_Module screen active.
10. IF a player enters a value into or clears a given cell, THEN THE Sudoku Game_Module SHALL leave that cell's value unchanged, SHALL record no player entry for that cell, and SHALL indicate that the cell is not editable.

### Requirement 17: Agent-Neutral Documentation Surface

**User Story:** As the sole developer, I want the harness instructions and the specs readable by any coding agent from vendor-neutral paths, so that no knowledge is locked inside one vendor's hidden directory and no adapter can drift into a stale second copy.

#### Acceptance Criteria

1. THE Harness SHALL provide exactly one Agent_Entry_Document — the plain-Markdown file named `AGENTS.md` at the Workspace_Root, following the vendor-neutral cross-agent instruction convention stewarded by the Agentic AI Foundation under the Linux Foundation — as the single instruction entry point read by every coding agent.
2. THE Agent_Entry_Document SHALL state, at minimum, one prohibition entry for every Anti_Requirement, a repo map naming each Harness-owned top-level directory and each top-level package of the `LightApp` Studio_Project with its purpose, and the Workspace_Root-relative location of the Design_Canon and of the Spec_Library.
3. THE Agent_Entry_Document SHALL state the build and verification commands as literal commands together with the directory each is run from, covering at minimum the Gradle debug build, the release build, the Android lint task, and the single-command Conformance_Checker invocation of Requirement 4 criterion 1.
4. THE Harness SHALL provide an Agent_Adapter_Link at `CLAUDE.md` under the Workspace_Root and at `.github/copilot-instructions.md` under the Workspace_Root, and SHALL realise every Agent_Adapter_Link as a symbolic link resolving to the Agent_Entry_Document rather than as a regular file holding copied content, so that a single source of truth cannot diverge across agents.
5. IF an Agent_Adapter_Link path is absent, is a regular file rather than a symbolic link, or resolves to any target other than the Agent_Entry_Document, THEN THE Conformance_Checker SHALL report the adapter path, which of those three conditions it exhibits, and the resolved target where the path is a symbolic link, for every Agent_Adapter_Link found in that single run, and SHALL exit with a non-zero status.
6. THE Spec_Library SHALL hold every spec document as a plain-Markdown file carrying the `.md` extension, in no vendor-specific format and under no vendor-specific extension, and THE Harness SHALL realise `.kiro/specs/<feature>` as a symbolic link resolving into the Spec_Library for every feature, so that Kiro and every other agent read the identical files.
7. IF a spec document exists inside a directory whose name begins with a dot and no path in the Spec_Library resolves to that same document, THEN THE Conformance_Checker SHALL report the document path and the dot-prefixed directory containing it, and SHALL exit with a non-zero status.
8. THE Agent_Entry_Document SHALL restate Design_Token values only within the generated block that the token generator task emits under Requirement 2 criterion 11, so that a restated value cannot disagree with the Token_Source by construction rather than by comparison.
9. IF the Agent_Entry_Document states a 6-digit hexadecimal color value, a dp value, an sp value, or a ms value outside that generated block, THEN THE Conformance_Checker SHALL report the line number and the stated value for every such occurrence found in that single run, and SHALL exit with a non-zero status.
10. THE Agent_Entry_Document SHALL contain at most 400 lines and at most 4,000 words, SHALL restate at most 40 individual Design_Token values, and SHALL link to the Design_Canon document holding the complete token table in place of reproducing that table, because the Agent_Entry_Document is loaded into every agent's context on every task.
11. THE Agent_Entry_Document SHALL record a current-state table carrying one row for every top-level package and directory named in its repo map, each row holding exactly one state drawn from `implemented`, `partial`, `specified-only`, and `empty`, and SHALL state that a document describing structure recorded as `specified-only` is a specification rather than a description of working code.
12. WHEN a change alters the implementation state of an item recorded in the Agent_Entry_Document current-state table, including implementing a Component_Library component, adding a source file to a previously empty directory, or deleting the prototype entry point named in Requirement 16 criterion 1, THE Agent_Entry_Document row for that item SHALL be updated to its new state in the same change.
13. IF the Agent_Entry_Document current-state table records `empty` for a directory that holds one or more source files, records `specified-only` for a package or directory present in the Harness with one or more source files, or omits a row for a package or directory named in its own repo map, THEN THE Conformance_Checker SHALL report the item name, the recorded state, and the observed state, and SHALL exit with a non-zero status.

### Requirement 18: Token Set Self-Validation

**User Story:** As the sole developer, I want the token set itself proven correct, so that the enforcement layers above it cannot faithfully propagate a wrong value everywhere.

Requirements 2, 10, 11, 13, and 15 all assume the declared tokens are correct and enforce their *use*. This requirement validates the tokens themselves, as properties of the token set rather than of any screen.

#### Acceptance Criteria

1. THE Token_Validation_Suite SHALL execute on the Java Virtual Machine without an emulator or a physical device, and SHALL complete within 30 seconds on the Reference_Machine.
2. IF any spacing token value in dp is not an integer multiple of 4, THEN THE Token_Validation_Suite SHALL report the token name and its value, and SHALL fail, excepting the single optical adjustment token of 2dp.
3. IF the spacing token scale is not strictly increasing, or the ratio between any two adjacent spacing tokens is below 1.3, THEN THE Token_Validation_Suite SHALL report both token names and the computed ratio, and SHALL fail.
4. IF any corner radius token other than the pill token exceeds 16dp, THEN THE Token_Validation_Suite SHALL report the token name and its value, and SHALL fail.
5. IF any motion duration token is not a whole multiple of 16.67ms within a tolerance of 1ms, THEN THE Token_Validation_Suite SHALL report the token name, its value, and the computed frame count, and SHALL fail.
6. THE Token_Validation_Suite SHALL compute the WCAG contrast ratio for every pairing of a Text_Hierarchy token with the background, elevated surface, and raised surface tokens of the same color mode, and SHALL fail, reporting both token names and the computed ratio, for any pairing below 4.5:1, excepting the disabled level which SHALL be held to 3:1.
7. IF the contrast ratio of the visible border token against the background token of the same color mode is 2:1 or greater, THEN THE Token_Validation_Suite SHALL report the computed ratio and SHALL fail, so that the Dot_Matrix_Texture cannot rise to the threshold of reading as content.
8. IF any type style declares the Red_Accent or its subtle tint as its color, THEN THE Token_Validation_Suite SHALL report the type style name and SHALL fail, because the Red_Accent measures below 4.5:1 against every surface token.
9. IF any type style below 36sp declares a dot-matrix font family, THEN THE Token_Validation_Suite SHALL report the type style name and its size, and SHALL fail.
10. IF a type style whose role is timer, counter, score, or grid numeral does not enable tabular figures, THEN THE Token_Validation_Suite SHALL report the type style name and SHALL fail.
11. IF two tokens of the same category declare the same value, THEN THE Token_Validation_Suite SHALL report both token names and the shared value, and SHALL fail, because two tokens holding one value means one of them has no job.
12. IF a token entry in the Design_Canon token source omits a non-empty `job` field or a non-empty `why` field, THEN THE Token_Validation_Suite SHALL report the token name and the missing field, and SHALL fail, so that no token can exist without a stated purpose and a stated derivation.
13. THE Conformance_Checker SHALL invoke the Token_Validation_Suite as part of a full run, and SHALL exit with a non-zero status when the Token_Validation_Suite fails.
14. THE Design_Canon SHALL record, for every threshold asserted by the Token_Validation_Suite, the derivation of that threshold, so that a failing test can be argued with rather than only obeyed.
