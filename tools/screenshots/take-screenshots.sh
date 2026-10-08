#!/usr/bin/env bash
# Takes the store listing screenshots on a phone and a tablet emulator, into store-screenshots/ (ignored
# by git). The same steps as take-screenshots.ps1; docs/store-screenshots.md explains them.
#
#   ./tools/screenshots/take-screenshots.sh [--phone AVD] [--tablet AVD] [--skip-build]
set -euo pipefail

# Git Bash rewrites anything that looks like a path for Windows programs, which turns adb's device
# paths into Windows ones. Turned off, and the local paths adb needs converted by hand (native).
export MSYS_NO_PATHCONV=1
native() { if command -v cygpath >/dev/null; then cygpath -m "$1"; else echo "$1"; fi; }

root="$(cd "$(dirname "$0")/../.." && pwd)"
output="$root/store-screenshots"
phone="Pixel_10"
tablet="Pixel_Tablet"
build=1

while [ $# -gt 0 ]; do
  case "$1" in
    --phone) phone="$2"; shift 2 ;;
    --tablet) tablet="$2"; shift 2 ;;
    --skip-build) build=0; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

app_id="com.spiraclestudios.autoskola.dev"
runner="$app_id.test/androidx.test.runner.AndroidJUnitRunner"
test_class="com.spiraclestudios.autoskola.screenshots.StoreScreenshots"
device_output="/sdcard/Android/data/$app_id/files/store-screenshots"
app_apk="$root/app/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
test_apk="$root/app/androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk"

# The SDK from local.properties (which escapes ':' and '\' the Java way), else ANDROID_HOME.
sdk="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$root/local.properties" 2>/dev/null \
  | sed 's/\\:/:/g; s/\\\\/\//g' | head -n 1)"
sdk="${sdk:-${ANDROID_HOME:?No Android SDK: set sdk.dir in local.properties or ANDROID_HOME.}}"
exe=""
[ -f "$sdk/platform-tools/adb.exe" ] && exe=".exe"
adb="$sdk/platform-tools/adb$exe"
emulator="$sdk/emulator/emulator$exe"

# The serial of the running emulator started from AVD $1, or nothing.
find_emulator() {
  local serial
  for serial in $("$adb" devices | tr -d '\r' | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1 }'); do
    if [ "$("$adb" -s "$serial" emu avd name | tr -d '\r' | head -n 1)" = "$1" ]; then
      echo "$serial"
      return
    fi
  done
}

start_emulator() {
  echo "Starting $1..." >&2
  "$emulator" -avd "$1" -no-snapshot-save >/dev/null 2>&1 &
  local serial attempt
  for attempt in $(seq 100); do
    serial="$(find_emulator "$1")"
    if [ -n "$serial" ] && [ "$("$adb" -s "$serial" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; then
      echo "$serial"
      return
    fi
    sleep 3
  done
  echo "$1 did not boot within 5 minutes." >&2
  exit 1
}

demo() {
  "$adb" -s "$serial" shell am broadcast -a com.android.systemui.demo "$@" >/dev/null
}

scales=(window_animation_scale transition_animation_scale animator_duration_scale)

# A fixed clock, a full battery and no notifications in the status bar, and no animations.
enter_steady_screen() {
  saved=()
  local name
  for name in "${scales[@]}"; do
    saved+=("$("$adb" -s "$serial" shell settings get global "$name" | tr -d '\r')")
    "$adb" -s "$serial" shell settings put global "$name" 0
  done
  "$adb" -s "$serial" shell settings put global sysui_demo_allowed 1
  demo -e command enter
  demo -e command clock -e hhmm 1000
  demo -e command battery -e level 100 -e plugged false
  demo -e command network -e wifi show -e level 4
  demo -e command network -e mobile show -e datatype none -e level 4
  demo -e command notifications -e visible false
}

exit_steady_screen() {
  demo -e command exit
  local index value
  for index in "${!scales[@]}"; do
    # "null" is a setting that was never written, which reads as the default of 1.
    value="${saved[$index]}"
    [ "$value" = "null" ] && value=1
    "$adb" -s "$serial" shell settings put global "${scales[$index]}" "$value"
  done
}

take_screenshots() {
  local avd="$1" folder="$2" started=0 result
  serial="$(find_emulator "$avd")"
  if [ -z "$serial" ]; then
    serial="$(start_emulator "$avd")"
    started=1
  fi
  echo "$folder: $avd ($serial)"
  "$adb" -s "$serial" install -r -t "$(native "$app_apk")" >/dev/null
  "$adb" -s "$serial" install -r -t "$(native "$test_apk")" >/dev/null
  enter_steady_screen
  result="$("$adb" -s "$serial" shell am instrument -w -e class "$test_class" "$runner" | tr -d '\r')" || true
  exit_steady_screen
  if ! grep -q 'OK (1 test)' <<<"$result"; then
    echo "$result"
    echo "The screenshot test failed on $avd." >&2
    [ "$started" = 1 ] && "$adb" -s "$serial" emu kill >/dev/null
    exit 1
  fi
  rm -rf "${output:?}/$folder"
  mkdir -p "$output"
  "$adb" -s "$serial" pull "$device_output" "$(native "$output/$folder")" >/dev/null
  echo "  -> $output/$folder"
  [ "$started" = 1 ] && "$adb" -s "$serial" emu kill >/dev/null
  return 0
}

if [ "$build" = 1 ]; then
  gradlew="$root/gradlew"
  [ -n "$exe" ] && gradlew="$root/gradlew.bat"
  (cd "$root" && "$gradlew" :app:androidApp:assembleDebug :app:androidApp:assembleDebugAndroidTest)
fi

take_screenshots "$phone" phone
take_screenshots "$tablet" tablet
