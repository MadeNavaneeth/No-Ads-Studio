# R8 rules for the release build.
#
# app/build.gradle has referenced this file since the release build type was added, and
# the file did not exist — so `bundleRelease`, the command AGENTS.md documents for
# producing a Play Console artifact, could not run.
#
# It is deliberately almost empty, and that is the correct outcome rather than a gap:
#
#   - Compose, Kotlin, Coroutines and DataStore all ship their own consumer rules inside
#     their AARs. Restating them here would create a second copy to keep in sync with
#     upstream, which is how these files rot.
#   - Nothing in this project uses reflection, service loading, JNI, or named-class lookup.
#     Every entry point is either the manifest-declared activity, which AGP keeps, or is
#     statically reachable from it. The vendored QQWing engine is plain arithmetic on int
#     arrays with no reflective access.
#   - There is no serialisation framework. Game state persists through SudokuCodec, which
#     writes digit strings by hand rather than reflecting over field names, so obfuscation
#     cannot change the on-disk format.
#
# Keep it that way. A rule added here should name what breaks without it.

# Crash reports from a minified build are otherwise unreadable, and with no crash
# reporting service (zero network, by design) a stack trace pasted by hand is the only
# diagnostic available. This keeps line numbers; -renamesourcefileattribute hides the
# original file names, which are not needed to map a trace back through mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
