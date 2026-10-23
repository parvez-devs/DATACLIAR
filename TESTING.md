# Verification

## Automated release gate

- Unit tests: query normalization/filters, unknown storage, own-cache boundaries, command injection rejection, profile-scoped freeze/unfreeze, cache-only fail-closed behavior and exact package/component matching.
- Compile app/test APKs, unit tests and lint.
- Android 35 emulator without Shizuku: navigate Home/Apps/Disabled/Setup, render Bengali setup/Connect, attempt Connect without crashing, preserve search through Activity recreation, assert no Internet permission; screenshots.
- Separate shell-identity harness (emulator battery unplug/reset for App Standby): reject self/Shizuku/system/cross-profile/injection targets; freeze/unfreeze, independently verified sleep or explicit OS refusal, wake, stop and clear data of only the separate disposable fixture APK; cache-only succeeds or is explicitly refused if unsupported. This is NOT a real Shizuku pairing test.

## Actual-phone acceptance checks (not certified by CI)

- With Shizuku absent: all basic dashboard/list/search features work. Setup opens official download and Settings shortcuts.
- Android 11+: pair/start official Shizuku over Wi-Fi/hotspot, authorize DATACLIAR, verify connection and a disposable app's controls.
- Deny/revoke authorization: controls refuse, Setup explains how to authorize again.
- Disconnect Wi-Fi, use mobile data and repeat a disposable app control. DATACLIAR has no network dependency, but verify OEM server survival.
- Screen off/on, background/reopen, rotate during an operation: no duplicate execution, result visible on Home, existing Shizuku session reconnects without pairing.
- Stop Shizuku: connection loss is visible, commands fail honestly, no automatic retries. Restart server and reconnect.
- Reboot: guide requires Shizuku startup again. Pair again only if the prior pairing was lost.
- Restore a frozen disposable app in Disabled; verify it launches. Unfreeze all apps before uninstalling DATACLIAR.
- Test cache-only with populated disposable cache and existing login; data must survive. Android 8–12 or unsupported shell refuses; use App info manually.
- Essential launcher/keyboard/VPN/admin/system targets refuse changes. Clear data requires explicit destructive confirmation.
- Grant/revoke Usage Access; unknown storage values are not shown as zero. Large fonts, split screen and large app lists remain usable.
- Verify APK checksum and signature compatibility separately. Never use important apps as deletion fixtures.
