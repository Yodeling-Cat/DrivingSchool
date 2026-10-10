#!/usr/bin/env bash
# Takes the store listing screenshots on a phone, a 7-inch tablet and a 10-inch tablet emulator, into
# store-screenshots/ (ignored by git), and checks the tablet ones against what the store accepts. The
# same steps as take-screenshots.ps1; docs/store-screenshots.md explains them.
#
#   ./tools/screenshots/take-screenshots.sh [--only phone|tablet] [--phone AVD] [--tablet-7 AVD]
#                                           [--tablet-10 AVD] [--skip-build]
set -euo pipefail

# Git Bash rewrites anything that looks like a path for Windows programs, which turns adb's device
# paths into Windows ones. Turned off, and the local paths adb needs converted by hand (native).
export MSYS_NO_PATHCONV=1
native() { if command -v cygpath >/dev/null; then cygpath -m "$1"; else echo "$1"; fi; }

root="$(cd "$(dirname "$0")/../.." && pwd)"
output="$root/store-screenshots"
phone="Pixel_10"
tablet_7="Pixel_Tablet_7-inch"
tablet_10="Pixel_Tablet_10-inch"
only="all"
build=1

while [ $# -gt 0 ]; do
  case "$1" in
    --phone) phone="$2"; shift 2 ;;
    --tablet-7) tablet_7="$2"; shift 2 ;;
    --tablet-10) tablet_10="$2"; shift 2 ;;
    --only) only="$2"; shift 2 ;;
    --skip-build) build=0; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done
case "$only" in
  all | phone | tablet) ;;
  *) echo "--only takes phone or tablet, not $only" >&2; exit 2 ;;
esac

app_id="com.spiraclestudios.autoskola.dev"
runner="$app_id.test/androidx.test.runner.AndroidJUnitRunner"
test_class="com.spiraclestudios.autoskola.screenshots.StoreScreenshots"
device_output="/sdcard/Android/data/$app_id/files/store-screenshots"
app_apk="$root/app/androidApp/build/outputs/apk/sk/debug/androidApp-sk-debug.apk"
test_apk="$root/app/androidApp/build/outputs/apk/androidTest/sk/debug/androidApp-sk-debug-androidTest.apk"

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
  local pid=$! serial attempt
  for attempt in $(seq 100); do
    kill -0 "$pid" 2>/dev/null || break
    serial="$(find_emulator "$1")"
    if [ -n "$serial" ] && [ "$("$adb" -s "$serial" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; then
      echo "$serial"
      return
    fi
    sleep 3
  done
  # An emulator left booting holds gigabytes of memory, so it does not outlive the attempt.
  kill "$pid" 2>/dev/null || true
  echo "$1 did not boot within 5 minutes. Is it an AVD that 'emulator -list-avds' lists?" >&2
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

# Shuts down an emulator this script started, and waits until it is gone, so two never run at once.
stop_emulator() {
  "$adb" -s "$serial" emu kill >/dev/null || true
  local attempt
  for attempt in $(seq 30); do
    "$adb" devices | tr -d '
' | grep -q "^$serial[[:space:]]" || return 0
    sleep 2
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
    [ "$started" = 1 ] && stop_emulator
    exit 1
  fi
  rm -rf "${output:?}/$folder"
  mkdir -p "$(dirname "$output/$folder")"
  "$adb" -s "$serial" pull "$device_output" "$(native "$output/$folder")" >/dev/null
  echo "  -> $output/$folder"
  [ "$started" = 1 ] && stop_emulator
  return 0
}

# What the store accepts for one kind of device, broken by each screenshot in folder $1: up to 8 MB,
# 16:9 or 9:16 (to the pixel), and each side between $2 and $3 px. Prints one line per problem.
check_store_requirements() {
  local file name size width height long short
  while IFS= read -r file; do
    name="${file#"$output"/}"
    size="$(wc -c <"$file" | tr -d ' ')"
    # A PNG's first chunk is IHDR, which starts with the width and height, big-endian, at byte 16.
    read -r width height < <(od -An -tu1 -j16 -N8 "$file" | tr -s ' 
' ' ' |
      awk '{ print ($1 * 16777216 + $2 * 65536 + $3 * 256 + $4), ($5 * 16777216 + $6 * 65536 + $7 * 256 + $8) }')
    long=$((width > height ? width : height))
    short=$((width > height ? height : width))
    [ "$size" -gt $((8 * 1024 * 1024)) ] && echo "$name is $size bytes, over 8 MB"
    local off=$((long * 9 - short * 16))
    [ "${off#-}" -gt 16 ] && echo "$name is ${width}x${height}, not 16:9 or 9:16"
    if [ "$short" -lt "$2" ] || [ "$long" -gt "$3" ]; then
      echo "$name is ${width}x${height}, outside $2-$3 px a side"
    fi
  done < <(find "$output/$1" -name '*.png' | sort)
  return 0
}

if [ "$build" = 1 ]; then
  gradlew="$root/gradlew"
  [ -n "$exe" ] && gradlew="$root/gradlew.bat"
  (cd "$root" && "$gradlew" :app:androidApp:assembleSkDebug :app:androidApp:assembleSkDebugAndroidTest)
fi

[ "$only" != tablet ] && take_screenshots "$phone" phone
if [ "$only" != phone ]; then
  take_screenshots "$tablet_7" tablet/7-inch
  take_screenshots "$tablet_10" tablet/10-inch

  problems="$(check_store_requirements tablet/7-inch 320 3840; check_store_requirements tablet/10-inch 1080 7680)"
  if [ -n "$problems" ]; then
    sed 's/^/  /' <<<"$problems"
    echo "$(wc -l <<<"$problems" | tr -d ' ') tablet screenshot problem(s): the store will not accept them as they are." >&2
    exit 1
  fi
  echo "The tablet screenshots meet the store's requirements."
fi
