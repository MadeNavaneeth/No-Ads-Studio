#!/usr/bin/env zsh
# Builds a flavour's debug APK, installs it on the running emulator, and launches it —
# the replacement for "build APK, drag it somewhere, check it manually".
#
# Usage: scripts/run-on-emu.sh [studio|sudoku|nonogram|minesweeper|connect|wordsearch]
#   (default: sudoku)
set -euo pipefail

cd "$(dirname "$0")/.."

FLAVOUR="${1:-sudoku}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"

case "$FLAVOUR" in
  studio)      APP_ID="com.noadsstudio" ;;
  sudoku)      APP_ID="com.noadsstudio.sudoku" ;;
  nonogram)    APP_ID="com.noadsstudio.nonogram" ;;
  minesweeper) APP_ID="com.noadsstudio.minesweeper" ;;
  connect)     APP_ID="com.noadsstudio.connect" ;;
  wordsearch)  APP_ID="com.noadsstudio.wordsearch" ;;
  *) echo "Unknown flavour '$FLAVOUR' — use studio, sudoku, nonogram, minesweeper, connect, or wordsearch" >&2; exit 1 ;;
esac

if ! "$ADB" devices | grep -q "^emulator-.*device$"; then
  echo "No emulator running. Start one with: scripts/emu-start.sh" >&2
  exit 1
fi

TASK="assemble$(echo "${FLAVOUR:0:1}" | tr a-z A-Z)${FLAVOUR:1}Debug"
echo "Building :app:$TASK..."
./gradlew ":app:$TASK" --console=plain

APK="app/build/outputs/apk/$FLAVOUR/debug/app-$FLAVOUR-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Expected APK not found at $APK" >&2
  exit 1
fi

echo "Installing..."
"$ADB" install -r "$APK"

echo "Launching $APP_ID..."
"$ADB" shell am start -n "$APP_ID/com.example.lightapp.MainActivity" >/dev/null

sleep 3
SHOT="/tmp/nas-fast-screenshot.png"
timeout 20 "$ADB" exec-out screencap -p > "$SHOT" 2>/dev/null || true
[[ -s "$SHOT" ]] && echo "Screenshot: $SHOT"

echo 'Logs: adb logcat -s NothingGames:D AndroidRuntime:E'
