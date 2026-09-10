#!/usr/bin/env zsh
# Builds a game's standalone debug APK, installs it on the running emulator, and
# launches it — the replacement for "build APK, drag it somewhere, check it manually".
#
# Composite-build edition (D37): each game is its own project under projects/<game>-app.
#
# Usage: scripts/run-on-emu.sh [sudoku|nonogram|minesweeper|connect|wordsearch|blockpuzzle|akari|binairo]
#   (default: sudoku)
set -euo pipefail

GAME="${1:-sudoku}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"

case "$GAME" in
  sudoku)      APP_ID="com.noadsstudio.sudoku" ;;
  nonogram)    APP_ID="com.noadsstudio.nonogram" ;;
  minesweeper) APP_ID="com.noadsstudio.minesweeper" ;;
  connect)     APP_ID="com.noadsstudio.connect" ;;
  wordsearch)  APP_ID="com.noadsstudio.wordsearch" ;;
  blockpuzzle) APP_ID="com.noadsstudio.blockpuzzle" ;;
  akari)       APP_ID="com.noadsstudio.akari" ;;
  binairo)     APP_ID="com.noadsstudio.binairo" ;;
  *) echo "Unknown game '$GAME' — use sudoku, nonogram, minesweeper, connect, wordsearch, blockpuzzle, akari, or binairo" >&2; exit 1 ;;
esac

PROJECT="$ROOT/projects/$GAME-app"
if [[ ! -d "$PROJECT" ]]; then
  echo "No project at projects/$GAME-app" >&2
  exit 1
fi

if ! "$ADB" devices | grep -q "^emulator-.*device$"; then
  echo "No emulator running. Start one with: scripts/emu-start.sh" >&2
  exit 1
fi

echo "Building $GAME-app..."
(cd "$PROJECT" && ./gradlew :app:assembleDebug --console=plain)

APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
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
