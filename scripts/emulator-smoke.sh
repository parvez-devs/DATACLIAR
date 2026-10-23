#!/usr/bin/env bash
set -euo pipefail
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.axiom.datacliar.test/androidx.test.runner.AndroidJUnitRunner | tee navigation-test.log
grep -q 'OK (1 test)' navigation-test.log
adb pull /sdcard/Android/data/com.axiom.datacliar/files/screenshots screenshots
bash scripts/emulator-control-smoke.sh
