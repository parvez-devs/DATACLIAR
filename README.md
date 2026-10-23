# ◈ DATACLIAR 1.1 — No root, no Shizuku

Native Android dashboard and app storage explorer. Android 8.0+ (API 26).

[Download APK](https://github.com/parvez-devs/DATACLIAR/releases) · [Build and test](https://github.com/parvez-devs/DATACLIAR/actions)

## Features

- Live available/total RAM and free/total internal storage.
- Available processor cores and architecture.
- Installed apps, including disabled apps, for the current Android profile.
- Search by app label or package, with user/system/disabled filters and recycled list rows.
- Optional Usage Access for per-app app/data/cache sizes. Unknown values remain unavailable; cache is already part of data.
- Android App info shortcuts to manually clear cache/data, force stop or enable/disable, when the OS permits.
- Device storage settings shortcut and cleanup of DATACLIAR's own temporary cache.
- Search, page and filter survive activity recreation. Background results do not update a destroyed Activity.

**বাংলা:** root বা Shizuku লাগবে না। RAM/storage, app list ও search সরাসরি চলবে। App-এর size দেখতে চাইলে ঐচ্ছিক Usage Access দিন। অন্য app-এর cache/data clear, force-stop বা disable/enable করতে app নির্বাচন করে **Open App info** চাপুন; Android Settings-এ কাজটি নিজে নিশ্চিত করুন।

## Platform limits

Ordinary Android apps cannot automatically force-stop, freeze or clear other apps' private data/cache. DATACLIAR does not claim automatic boost, batch cleanup, live system process counts or system CPU load. App info controls differ by device and app. A Settings shortcut does not report that a management action succeeded. Work profiles are not managed.

The old privileged control engine and Shizuku dependencies/provider/AIDL have been removed. There is no shell-command execution path.

## Install

Download the APK from a successful release and allow installation from that source. No root setup or helper service is needed. Usage Access is optional under **Access**. If updating an older debug preview fails with a signature mismatch, uninstall the old preview first. If you previously froze apps in version 1.0, restore them through Android Settings or the old app before uninstalling it; this version cannot automatically restore them.

## Build and verification

JDK 17, Android SDK 35 and Gradle 8.9:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

CI gates preview publication on unit tests, lint, APK compilation and an Android 35 emulator navigation/sandbox test. See [TESTING.md](TESTING.md). This does not certify every OEM device.

Releases are installable **debug-signed prereleases**, not production-signed store builds. A clean runner may generate a different certificate, requiring uninstall/reinstall between previews. APK checksums accompany releases. Production distribution requires a private, persistent release signing key.

## Privacy

No Internet permission, ads, accounts, analytics, uploads, root, Shizuku or background monitoring service. Metrics update only while the app is visible. Usage Access can be revoked at any time.
