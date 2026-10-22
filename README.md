# ◈ DATACLIAR

**Your phone. In focus.** Native Android device dashboard and Shizuku-powered app manager with a dark sky-blue interface.

[Download APK from Releases](https://github.com/parvez-devs/DATACLIAR/releases) · [Build status](https://github.com/parvez-devs/DATACLIAR/actions)

## What works

| Feature | Access / behavior |
|---|---|
| RAM available / total | Standard Android APIs; includes reclaimable memory |
| Internal storage free / total | Usable data partition, not advertised capacity |
| CPU cores / architecture / load | Load sampled from `/proc/stat`; Shizuku fallback; unavailable when OS denies it |
| Installed app list + search | Includes disabled apps; current Android user only |
| Per-app code, data and cache | Enable Usage Access; unknown values stay unknown; cache is already included in data |
| Running app count | Shizuku `ps` snapshot matched to package process names; not a historical usage estimate |
| Force stop | Shizuku; stops the app and can delay notifications |
| Sleep / wake | Android inactive hint; Android may wake the app again |
| Freeze / unfreeze | Disables/enables user apps; launcher entry disappears while disabled; remains in Frozen tab |
| Clear app data | Explicit destructive confirmation; wipes local accounts/settings/files |
| Cache-only / batch clean | Android 13+ **and** shell help must expose `--cache-only`; no fallback to data deletion |
| Boost | Batch force-stop selected running background apps; skips foreground/unknown process states; no promised speed gain |

System apps, DATACLIAR, Shizuku, the default launcher, active keyboard, VPN apps and device administrators are protected from controls. Batch cache clean targets eligible user apps, **not every byte of OS/system cache**. Package naming and OEM process visibility can limit the running snapshot. Rooted users can start Shizuku with root; direct `su` is not required.

## Install and enable

1. Download the APK from a successful GitHub Release and allow installation from that source.
2. Open **Access → Usage Access**, select DATACLIAR and enable it for storage figures.
3. Install [Shizuku](https://shizuku.rikka.app/download/) and start it. Android 11+ supports wireless debugging; older versions need a computer/ADB or root.
4. Return to **Access → Connect / authorize Shizuku** and approve access.
5. Refresh Apps. Tap any app for controls; use Frozen to restore hidden apps.

**বাংলা:** RAM/storage দেখতে root লাগে না। অন্য app stop/freeze/cache clear করতে Shizuku চালু করে permission দিতে হবে। Cache-only batch Android 13+ প্রয়োজন। Data clear করলে app-এর local data মুছে যাবে। Frozen tab থেকে unfreeze করুন।

After reboot, Shizuku may need restarting. Some manufacturers impose additional restrictions; errors are surfaced, never presented as success. Unfreeze apps before uninstalling DATACLIAR. If needed, recover with `adb shell pm enable --user 0 PACKAGE` (replace user ID for a different profile). Freezing hides the launcher entry; it is not a stealth feature and remains visible in Android Settings.

## Build

Requires JDK 17, Android SDK 35 and Gradle 8.9.

```sh
gradle testDebugUnitTest lintDebug assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

The Actions workflow runs tests, lint and compilation, attaches verification reports, computes SHA-256 checksums and publishes an installable **debug-signed preview** only after all checks pass. These are sideload previews, not Play Store production releases. Each clean CI runner can use a different debug certificate: uninstall an older preview if Android reports a signature mismatch (unfreeze apps first). Production distribution needs a private release signing key stored in repository secrets; no signing secrets are committed.

## Verification scope

Unit tests cover injection rejection, cache-only fail-closed behavior, user-scoped freeze/unfreeze commands, process deduplication, CPU counter accounting and foreground protection. Compilation and lint do not establish OEM device compatibility. Shizuku permission flows and destructive controls should be tested on disposable apps on the actual phone. See [manual test checklist](TESTING.md).

## Privacy

No Internet permission, ads, analytics, account, upload or background monitoring service. Device/app information stays on the phone. Shizuku runs only the allow-listed commands after explicit user actions. The UI samples memory/CPU while visible. Source contains no arbitrary shell console.

## Platform references

- [Android StorageStatsManager](https://developer.android.com/reference/android/app/usage/StorageStatsManager)
- [Shizuku API and user services](https://github.com/RikkaApps/Shizuku-API)
- [AOSP package shell commands](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/pm/PackageManagerShellCommand.java)
