---
status: canonical
---

# Anti-Requirements

> The authoritative prohibition list. The conformance checker enforces the mechanically
> checkable entries; the rest are review obligations.

=== THINGS THAT DON'T PERFORM — AVOID FOR NOTHING SUDOKU ===

APP / DESIGN (Nothing / Offline / Light)
- NO ads, banners, interstitials (kills clean launch, memory, trust)
- NO network permission / INTERNET in manifest (violates offline promise)
- NO analytics / tracking SDKs (Firebase, Ads, Crashlytics — heavy, network)
- NO external images, photos, sound files (use Compose shapes/colors only)
- NO decorative gradients (Nothing is flat monochrome)
- NO bright accent colors / brand colors (must stay black/gray/white)
- NO sharp corners / decorative borders (must be round/pill/circle)
- NO complex animations (bounce, spring, long transitions) — Nothing uses calm 150ms fades
- NO non-geometric fonts (must use clean sans / Geist-style)

BUILD / CODE (Kotlin Compose / Slow Mac / Play Console)
- NO React / Flutter / WebView wrapper (unnecessary bridge overhead for grid game)
- NO huge dependency libraries (avoid heavy Compose add-ons)
- NO large embedded datasets (keep puzzle inline or minimal resource file)
- NO Leanback / TV support (unneeded code bloat)
- NO redundant Compose recompositions (use remember / derivedStateOf / LazyColumn only when needed)
- NO unoptimized Int array operations (use simple loops, avoid nested object creation on every frame)
- NO background threads / coroutines for simple grid logic (not needed for Sudoku)
- NO heavy error-handling libraries (simple try/catch is enough)

DEVICE / MAC (Slow MacBook Air — 2 core, 8GB)
- NO Android Studio + heavy emulator running at same time (close browser tabs, use Preview only)
- NO downloading Android Studio plugins / SDK extras not needed
- NO running Gradle sync repeatedly (build once, test with Preview)
- NO keeping large image folders in project
- NO using external build services (EAS/Cloud) if you already have SDK — but EAS is okay for Play Console build if local is too slow
- NO opening multiple heavy IDE windows

PLAY CONSOLE / RELEASE
- NO debug builds for release (.aab must be release/minified with R8)
- NO unproguarded code (enable minification for size/speed)
- NO extra permissions (only what is essential — none for this offline game)
- NO unnecessary language resources (keep en/bare minimum to reduce APK size)
- NO splash screen delay / heavy launch animation
