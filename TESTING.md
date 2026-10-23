# Verification

## CI gate

- Unit tests: search normalization, package matching, filters and unknown/storage total accounting.
- Compile both APKs, run unit tests and lint.
- Android 35 emulator, no root/Shizuku grants: Home, Apps, Disabled, Access and return navigation; screenshots.
- Verify search survives Activity recreation and the APK does not request Internet or contain Shizuku. Unit tests verify own-cache cleanup preserves sibling app-data files and refuses symlinks outside the cache.

## Actual-device checks (not certified by emulator CI)

- Android 8+ on target OEMs: launch with Usage Access denied; RAM/storage and app list work.
- Grant/revoke Usage Access and return; storage sizes refresh or become unavailable.
- Large installed-app list: scroll/search/filter remain responsive.
- Rotate, background/resume, split screen and large fonts; all navigation stays reachable.
- App info opens the selected package. Confirm any cache/data/stop/disable action manually in Settings; Android may hide controls.
- Clear DATACLIAR cache: only its temporary files disappear; preferences and other apps remain intact.
- No root/Shizuku dependency or request. No automatic boost, other-app process count or batch cleanup claims.
- Install preview APK and verify SHA-256. Check upgrade signing behavior separately.
