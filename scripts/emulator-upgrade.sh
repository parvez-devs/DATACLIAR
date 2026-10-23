#!/usr/bin/env bash
set -euo pipefail
adb uninstall com.axiom.datacliar.test
adb uninstall com.axiom.datacliar
adb install -t distribution/upgrade-base.apk
adb install -t distribution/instrumentation.apk
adb shell am instrument -e class com.axiom.datacliar.UpgradePersistenceTest#seedData -w com.axiom.datacliar.test/androidx.test.runner.AndroidJUnitRunner | tee upgrade-seed.log
grep -q 'OK (1 test)' upgrade-seed.log
adb install -r distribution/DATACLIAR.apk
adb shell am instrument -e expectedVersionCode "$APP_VERSION_CODE" -e class com.axiom.datacliar.UpgradePersistenceTest#verifyData -w com.axiom.datacliar.test/androidx.test.runner.AndroidJUnitRunner | tee upgrade-verify.log
grep -q 'OK (1 test)' upgrade-verify.log
