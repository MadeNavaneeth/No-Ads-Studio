#!/usr/bin/env zsh
# Stops the running emulator cleanly (saves its snapshot for a fast next boot).
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"

if ! "$ADB" devices | grep -q "^emulator-.*device$"; then
  echo "No emulator is running."
  exit 0
fi

DEVICE="$("$ADB" devices | awk '/^emulator-/{print $1; exit}')"
echo "Stopping $DEVICE..."
"$ADB" -s "$DEVICE" emu kill
