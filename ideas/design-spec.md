---
status: research
---

# Design Spec — Original Research

> Ideation and research notes. Where this document and the design canon state the same token
> value, **the canon wins**: see `design-canon/nothing-tokens.md`. Kept for the reasoning and the
> Nothing OS research, not as a source of values.

=== PROJECT IDEOLOGIES ===
- Zero ads, zero tracking, completely free
- 100% offline — no network, no permissions, no connections
- Clean launch, minimal, fast, light memory footprint
- Match Nothing Phone company Design Language (Nothing OS / Glyph)
- Target: Android Play Console (.aab)
- Framework: Native Kotlin + Jetpack Compose (fastest, lowest memory, direct control)
- Games: Sudoku + simple puzzles (nonogram, word, logic) — all offline
- Quality target: best-in-market feel (smooth, precise, elegant)

=== NOTHING PHONE DESIGN LANGUAGE SPEC ===
Inspired by Nothing (Nothing OS / Nothing Phone / Glyph Interface):

1. COLOR PALETTE (monochrome / near-black)
   - Background: #050505 (near pure black) or #0A0A0A (Nothing dark)
   - Surface / Card: #121212 (slightly lifted black)
   - Primary text: #F5F5F5 (soft white, not pure white for comfort)
   - Secondary text: #888888 (muted gray)
   - Tertiary / disabled: #444444
   - Divider / border: #222222 (very subtle)
   - Accent / highlight: #EAEAEA (soft light gray, not bright)
   - No bright colors, no gradients, flat only

2. ROUNDNESS / SHAPE
   - All buttons: fully rounded pill / circle (corner radius = height/2)
   - Cards / containers: 16dp - 24dp rounded corners
   - Icons / markers: circular dots (Glyph dots: 4dp - 8dp circles)
   - Grid cells (Sudoku): subtle rounded square (8dp radius) or pure rounded rect
   - No sharp rectangles anywhere

3. TYPOGRAPHY
   - Font family: Geometric sans-serif (system default or custom: Inter / Space Grotesk feel)
   - Headings: Thin / Light weight, large tracking, generous spacing
   - Body / numbers: Regular, clear, high contrast against black
   - Numbers (Sudoku): Large, bold, centered, thin stroke feel
   - Labels: Very small, uppercase, letter-spacing 0.05em, gray
   - No decorative fonts, only functional clean type

4. LAYOUT / SPACING
   - Centered vertical composition (not edge-to-edge clutter)
   - Generous whitespace — empty space is a design element
   - Grid-based alignment — everything on a strict 4dp / 8dp grid
   - Minimal borders — use spacing and subtle color shifts instead of lines
   - No crowded screens — one purpose per view

5. PATTERN / TEXTURE (Glyph-inspired)
   - Subtle dot matrix background in some screens (drawn with Canvas, very low opacity #222222)
   - Dots arranged in a grid (8dp spacing) — like Nothing Glyph interface
   - Not noisy — dots are barely visible, just texture

6. MOVEMENT / ANIMATION (optional but high quality)
   - Very fast, subtle transitions (fade + slight scale, 150-200ms)
   - No bouncy animations — Nothing is calm, not playful
   - Touchable feedback: subtle gray highlight on press, not color change

7. CONTENT RULES
   - No ads, no banners, no popups
   - No social links, no external URLs in app
   - All puzzles generated / embedded locally (no server)
   - All data stays on device — never writes to cloud
   - Simple start: Sudoku with one pre-built valid grid + manual entry
   - Later: Nonogram (pixel grid), Word search (text grid)

8. TECH / BUILD RULES
   - Android SDK only (no network libraries)
   - AndroidManifest: NO INTERNET permission (explicitly removed)
   - Kotlin Compose only (no React/native bridge overhead)
   - No external image/assets beyond 2-3 small icons (use Compose shapes instead)
   - Target SDK 34/35 (Play Console current)
   - Build .aab via Android Studio or command-line Gradle
   - Release build: ProGuard / R8 enabled, minified, optimized

=== DEEP RESEARCH — NOTHING OS 5.0 / NOTHING PHONE OFFICIAL ===
From nothing.tech/nothing-os (Nothing OS 5.0 Open Beta, Aug 2026):
- Typeface: "Geist" — Nothing's custom geometric sans-serif, used across Settings/Camera/Gallery/Status Bar for consistency
- Design philosophy: "Cleaner interface, deeper personalisation, more expressive design" — but expression comes from minimal precision, not decoration
- Glyph Interface: central design element — light/dot patterns used for control, timer, app interface; dots are structural, not decorative
- Micrographics clockface: "clean, hardware-inspired design" — minimal, geometric, no ornament
- Essential apps (Space, Voice, Key): redesigned for "cleaner notes and better summaries" — typography-first, whitespace-heavy
- Spacing / layout: "refined layouts bring greater consistency" — strict alignment, generous padding, grid-based
- Motion: "smoother motion everywhere" — fast (150-200ms), calm, no bounce
- Performance as design: "Better performance across app launches, scrolling, touch response" — light memory management, no unnecessary reloads
- Dark theme: expanded dark theme, wallpaper-aware system colors (optional for app: respect system dark)
- Icons: updated for consistency across all default apps — monochrome, geometric, uniform weight
- Study reference: Nothing's own apps (Compass, Camera, Gallery, Essential Space) all use near-black surfaces, thin white type, subtle gray dividers, circular/dot markers

=== SUDOKU SCREEN STRUCTURE ===
- Background: #050505
- Title: "Sudoku" — thin, large, soft white
- 9x9 grid: 9 Rows, each with 9 Box cells
  - Empty cells: #121212 fill, #222222 border, 8dp radius
  - Filled cells: #EAEAEA text, #121212 fill
  - Selected cell: #888888 border (subtle highlight)
  - Same-row/col/cbox hint (optional): very subtle #333333 background
- Number pad below grid: 1-9 in circular pill buttons (black bg, white text, 24dp height)
- Erase / Clear button: circular outline button
- Menu / New Game: top-right, small gray text button with circle icon
- No ads, no banners, no external links

=== WHAT MAKES NOTHING "NOTHING" ===
- Nothing = absence of clutter
- Nothing = monochrome discipline (only black, white, gray)
- Nothing = circular geometry (dots, pills, circles — no squares with sharp corners)
- Nothing = typography over decoration
- Nothing = calm (no bright colors, no hype, no noise)
- Nothing = essential only (one screen, one task)
