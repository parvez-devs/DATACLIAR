# DATACLIAR 1.1.0 — No-root preview

Runs without root or Shizuku on Android 8+.

- RAM/storage dashboard, processor details, searchable app inventory and disabled-app list.
- Optional Usage Access for per-app storage sizes.
- Manual app management through Android App info; device storage shortcut.
- Clears only DATACLIAR's own temporary cache after confirmation.
- Removed privileged controls, shell execution, Shizuku dependencies and unsupported automatic boost/cleanup claims.
- Recycled app rows and lifecycle-safe background delivery; saved search/page/filter.
- Preview publication requires unit tests, lint, compilation and Android 35 emulator tests.

Debug-signed sideload preview. Signature changes may require uninstalling an older preview. Restore any apps frozen by version 1.0 before removing that version. This release cannot automatically freeze, unfreeze, stop or clean other apps. OEM compatibility still requires actual-device testing. SHA256SUMS.txt verifies the APK download.
