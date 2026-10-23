# ◈ DATACLIAR 1.2 — Native dashboard + Shizuku setup

Native Android app (Android 8+). Basic dashboard works without root or Shizuku. Optional Shizuku controls run inside DATACLIAR after setup.

[Download APK](https://github.com/parvez-devs/DATACLIAR/releases) · [Checks](https://github.com/parvez-devs/DATACLIAR/actions)

## Setup inside DATACLIAR

Open **Setup** for Bengali instructions, live connection state, official Shizuku download, Wi-Fi settings, Developer options, Shizuku launch/pairing shortcut, Connect/authorize and battery/app settings.

1. Install the separate official [Shizuku app](https://shizuku.rikka.app/download/).
2. On Android 11+, connect to Wi-Fi or another phone's hotspot. Enable Developer options, USB debugging and Wireless debugging.
3. Open Shizuku, tap Pairing, get the pairing code from Android Wireless debugging, enter it in Shizuku's pairing notification, and tap Start in Shizuku.
4. Return to DATACLIAR and tap **Connect / authorize**. Allow DATACLIAR.
5. Once Shizuku is running, try switching back to mobile data. DATACLIAR controls use local Binder IPC, never require or check Wi-Fi/Internet, and keep the application-scoped connection across rotation/backgrounding. Shizuku itself remains subject to Android/OEM restrictions.

Android 8–10 needs PC/USB or root for Shizuku startup. The guide and connection are embedded; the Shizuku manager, Android pairing dialog and startup remain external. This APK does not silently pair, install the manager, toggle debugging or restart a stopped Shizuku server.

## Keeping the session available

Do not disable Developer options or USB debugging. Allow Shizuku to run in the background; set its battery mode to Unrestricted where available. The guide links to its App info. Where available, enable Disable adb authorization timeout. See [official setup/troubleshooting](https://shizuku.rikka.app/guide/setup/).

Shizuku must be started again after reboot and can also stop due to Android/OEM policies, revocation or service termination. No guarantee of uninterrupted operation until power-off. DATACLIAR does not stop Shizuku on Activity destruction. While the server is alive, reopening DATACLIAR reconnects automatically with existing authorization. A dead server produces a visible reconnect message rather than fabricated success. Operations run only after user confirmation, are serialized across rotation, and are never automatically retried.

## Features

- RAM and internal storage meters, processor core/architecture information.
- Searchable app inventory, recycled rows, user/system filters and Disabled tab; current profile only.
- Optional Usage Access for app/data/cache sizes; unknown remains unknown; cache is already included in data.
- Shizuku: freeze/unfreeze, force stop, sleep/wake, explicit destructive app-data clear.
- Cache-only: Android 13+ AND `pm help` must advertise `--cache-only`; never falls back to data clearing. On unsupported phones, use Android App info manually.
- System apps, DATACLIAR, Shizuku, launcher, active keyboard, device-policy apps and VPN apps are protected. Essential-app checks fail closed if shell queries fail. Unfreeze permits restoring disabled user apps.
- Android App info/device storage shortcuts; own-cache cleanup.

Freeze hides an app's launcher icon. Restore through **Disabled → app → Unfreeze / enable**. **Unfreeze apps before uninstalling DATACLIAR.** Sleep is an OS-dependent, asynchronous inactivity hint, not permanent freezing. If Android refuses or overrides it, the app reports an unconfirmed state instead of success. Stop/sleep may delay notifications. No automatic boost or background surveillance.

## Build and validation

JDK 17, Android SDK 35, Gradle 8.9:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

CI gates publication on unit tests, lint, compilation, Android 35 navigation/recreation/setup tests and a shell-identity control smoke test against only the separate disposable fixture APK. The shell harness verifies command behavior; it does not prove actual-phone Shizuku pairing, Binder lifecycle or Wi-Fi disconnect behavior. See [TESTING.md](TESTING.md).

The privileged UserService avoids app Context PackageManager/ContentResolver calls that mismatch shell UID attribution. No arbitrary shell console or string interpolation is exposed; commands use a closed action vocabulary with separately passed arguments, profile checks and command timeouts.

## Distribution and privacy

Installable **debug-signed prerelease**, not a production-signed store build. Clean CI runners may generate different certificates; uninstall an older preview if Android reports a signature mismatch, after unfreezing apps. Production upgrades require a persistent private release key. SHA256SUMS.txt accompanies the APK.

No Internet permission, ads, analytics or uploads. External setup/download links open in other apps. Shizuku API by RikkaW, MIT license, shown in Setup and included under assets.
