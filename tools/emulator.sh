#!/usr/bin/env bash
# Starts the LibreCans test emulator. Pass --headless to run without a window.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
AVD="LibreCans_Pixel_9_Pro_API_37"
ARGS=(-avd "$AVD" -no-boot-anim -no-snapshot-save)

if [[ "${1:-}" == "--headless" ]]; then
    ARGS+=(-no-window -no-audio -gpu swiftshader_indirect)
else
    ARGS+=(-gpu host)
fi

"$SDK/emulator/emulator" "${ARGS[@]}" &
"$SDK/platform-tools/adb" wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
echo "$AVD is ready"
