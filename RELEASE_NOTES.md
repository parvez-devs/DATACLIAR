# DATACLIAR 1.3.0

- Batch freeze/unfreeze/force-stop with explicit app selection and confirmation, per-app results and stop-after-current cancellation. No bulk app-data wipe.
- Persistent favourites, Favourites filter and local history of the last 200 results. Interrupted operations are marked and never automatically retried.
- Native Updates screen: manual GitHub check, optional daily-on-launch check, verified download and Android-confirmed installation. SHA-256, package, version and signing certificate are checked.
- Persistent private release signing. In-place upgrades from this signing lineage retain app data; release CI tests a signed version increment and verifies favourites/history survive.
- Bengali Shizuku setup, live status and in-app controls remain available. Dashboard works without Shizuku.

Migration: earlier debug-signed previews use different certificates. Unfreeze apps before removing an old preview; a one-time reinstall may be needed to enter the new signing lineage. Subsequent releases must reuse the new private key. The key is not in this repository or public CI artifacts.

Updates now require Internet permission only for optional GitHub checks/downloads, and installation permission for the user-confirmed Android installer. Device inventory, favourites and operation history are never uploaded. Daily checks are off by default. No background push notifications or silent installs.

Platform limits remain: Shizuku must be paired/started separately and restarted after reboot or service loss. OEM policies can stop it before power-off. Sleep is an OS-dependent inactivity hint. Android 35 emulator refuses the sleep hint and times out its advertised cache-only command; these outcomes are reported accurately and cache-only never falls back to clearing data. Use Android App info when automatic cache cleanup is unconfirmed.
