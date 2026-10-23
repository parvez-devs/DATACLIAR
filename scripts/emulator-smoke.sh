#!/usr/bin/env bash
set -euo pipefail
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.axiom.datacliar.test/androidx.test.runner.AndroidJUnitRunner | tee navigation-test.log
grep -Eq 'OK \([0-9]+ tests?\)' navigation-test.log
adb pull /sdcard/Android/data/com.axiom.datacliar/files/screenshots screenshots

