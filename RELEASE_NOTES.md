# DATACLIAR 1.2.0 — Shizuku setup preview

- Native Bengali setup guide with official Shizuku installation, pairing/start instructions, settings shortcuts and Connect/authorize.
- Live connection state, application-scoped reconnect and serialized controls that survive Activity recreation. No Wi-Fi requirement/check after Shizuku starts.
- In-app freeze/unfreeze, stop, OS-dependent sleep/wake, explicit data clear and capability-gated cache-only cleanup. Inactivity is verified with bounded re-reads; OS refusal is reported as an error.
- Protected system/essential apps, current-profile restriction, bounded commands and no data-wipe fallback for cache.
- Fixed shell UID attribution by avoiding app-context PackageManager/ContentResolver in the privileged service.
- Dashboard, search, optional storage access and own-cache cleanup remain available without Shizuku.

Official Shizuku must be installed separately. Pairing/start happen in Android Settings and Shizuku; this APK guides and connects. Android 11+ supports wireless startup; older Android needs PC/USB or root. Reboot or service termination requires startup again. OEM policies may stop Shizuku before power-off; uninterrupted service is not guaranteed.

CI includes build/unit/lint, Android 35 navigation/setup/recreation tests and a shell control smoke test. Actual-phone pairing and Wi-Fi-to-mobile-data continuity still require device verification.

Debug-signed prerelease; older previews may require uninstall/reinstall due to signing changes. Unfreeze apps before uninstalling. Check SHA256SUMS.txt.

Known emulator limits: Android 35 overrode the sleep hint (the app accurately reports unconfirmed state) and its advertised cache-only command timed out. Automatic cache-only cleanup is not certified on that image; use Android App info when refused or unconfirmed. Release tests independently check state/error reporting, preservation of fixture data after the cache attempt, and actual data deletion after explicit clear-data.
