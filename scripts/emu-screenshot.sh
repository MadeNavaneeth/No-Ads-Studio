#!/usr/bin/env zsh
# Captures a screenshot from the running emulator to a local PNG.
#
# Usage: scripts/emu-screenshot.sh [output-path]
#   (no arg) — writes to /tmp/nas-fast-screenshot.png
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
OUT="${1:-/tmp/nas-fast-screenshot.png}"

if ! "$ADB" devices | grep -q "^emulator-.*device$"; then
  echo "No emulator running. Start one with: scripts/emu-start.sh" >&2
  exit 1
fi

# 20s timeout: a cold or heavily-loaded emulator (software rendering is CPU-bound) can
# take several seconds longer than a normal adb command to answer this one.
timeout 20 "$ADB" exec-out screencap -p > "$OUT"

if [[ ! -s "$OUT" ]]; then
  echo "Screenshot came back empty — see the -no-window note in emu-start.sh" >&2
  exit 1
fi

echo "Saved: $OUT"
