#!/usr/bin/env bash
set -euo pipefail
main_apk=$(adb shell pm path com.axiom.datacliar | head -n1 | tr -d '\r' | cut -d: -f2-)
test_apk=$(adb shell pm path com.axiom.datacliar.test | head -n1 | tr -d '\r' | cut -d: -f2-)
test -n "$main_apk"
test -n "$test_apk"
# App Standby does not take effect while charging. Only the disposable emulator is changed.
trap 'adb shell dumpsys battery reset >/dev/null 2>&1 || true' EXIT
adb install -r -t fixture/build/outputs/apk/debug/fixture-debug.apk
adb shell am start -W -n com.axiom.datacliar.fixture/.FixtureActivity
adb shell am force-stop com.axiom.datacliar.fixture
adb shell settings put global app_standby_enabled 1
adb shell settings put global adaptive_battery_management_enabled 1
adb shell dumpsys battery unplug
adb shell "CLASSPATH=$main_apk:$test_apk app_process /system/bin com.axiom.datacliar.ControlSmoke" | tee control-smoke.log
grep -q 'CONTROL_SMOKE_OK' control-smoke.log
