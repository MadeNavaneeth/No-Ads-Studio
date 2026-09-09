#!/usr/bin/env zsh
# Starts the lightweight "nas-fast" AVD for local device checks: no audio, no camera, no
# GPS/gyro/sensors, no SD card, no device frame, 1.5GB RAM, small_phone profile, API 34
# google_apis image. Cold boot is roughly 45-75s once; every boot after that resumes from
# a saved snapshot in a few seconds, because this omits -no-snapshot-save.
#
# Tried and rejected: the android-35 aosp_atd image, which is lighter and boots faster but
# never produced a working screenshot on this host — screencap returned an identical black
# frame for every app and even the bare system UI, on every GPU mode tried. google_apis on
# API 34 has a proven capture path and is still a reasonably light image.
#
# Package verification is disabled on first boot (adb install otherwise hits
# INSTALL_FAILED_VERIFICATION_FAILURE, since this device has no real path to Google's
# verification service) — see the `settings put global package_verifier_enable 0` call
# below.
#
# Usage: scripts/emu-start.sh [foreground]
#   (no arg)     — runs in the background, detached, logs to /tmp/nas-fast-emulator.log
#   foreground   — runs attached, for watching boot output directly
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
EMULATOR="$SDK/emulator/emulator"
ADB="$SDK/platform-tools/adb"
AVD_NAME="nas-fast"
LOG="/tmp/nas-fast-emulator.log"

if [[ ! -x "$EMULATOR" ]]; then
  echo "emulator binary not found at $EMULATOR — check ANDROID_HOME" >&2
  exit 1
fi

if "$ADB" devices | grep -q "^emulator-.*device$"; then
  echo "An emulator is already running:"
  "$ADB" devices
  exit 0
fi

# Two host-specific fixes, found by diagnosing a screenshot pipeline that returned a
# byte-identical black PNG for every capture regardless of which app or AVD image was
# running:
#
# 1. -gpu swiftshader_indirect, not host. Host-GPU passthrough on this machine's Intel
#    UHD 617 rendered the app correctly (HWUI logged "Displayed", no crash) but the shared
#    framebuffer that screencap/screenrecord read from came back empty. Software
#    rendering is slower per frame but the capture is trustworthy, which matters more for
#    a device meant for visual checks.
# 2. No -no-window. Headless mode (-no-window) on this host never drives real content
#    into the capturable surface at all — confirmed by testing screencap against the
#    bare system UI with zero apps installed, before host-GPU was ever a suspect. A real
#    window is required for a real framebuffer, even though nothing here displays it.
ARGS=(-avd "$AVD_NAME" -no-audio -gpu swiftshader_indirect -no-boot-anim)

if [[ "${1:-}" == "foreground" ]]; then
  exec "$EMULATOR" "${ARGS[@]}"
fi

nohup "$EMULATOR" "${ARGS[@]}" >"$LOG" 2>&1 &
disown

echo "Starting $AVD_NAME in the background (log: $LOG)..."
"$ADB" wait-for-device

echo -n "Waiting for boot to finish"
for _ in $(seq 1 90); do
  booted="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  [[ "$booted" == "1" ]] && break
  echo -n "."
  sleep 1
done
echo

if [[ "$booted" != "1" ]]; then
  echo "Still booting after 90s — check $LOG" >&2
  exit 1
fi

# Without this, `adb install` on a debug or locally-signed APK can hit
# INSTALL_FAILED_VERIFICATION_FAILURE: this device has no real route to Google's
# verification service, and the check times out instead of failing fast.
"$ADB" shell settings put global package_verifier_enable 0 2>/dev/null || true
"$ADB" shell settings put global verifier_verify_adb_installs 0 2>/dev/null || true

echo "Ready:"
"$ADB" devices
