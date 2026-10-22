# Device verification checklist

Use a disposable test app. Do not test data deletion on important apps.

- Launch with no Usage Access and no Shizuku: RAM/storage available; unknown storage and processes clearly labeled.
- Grant/revoke Usage Access and return: refresh updates storage; total = code + data, without double-counting cache.
- Start Shizuku, grant/deny access, kill/restart it: no crash; controls show connection errors.
- Rotate, background/resume, use split-screen and large fonts; navigation remains reachable.
- Start a disposable app: refresh shows its process; foreground app is skipped by boost.
- Freeze the disposable app: launcher entry disappears, Frozen lists it; unfreeze restores launchability.
- Sleep/wake: refresh verifies inactive state; expect Android to override inactivity after interaction.
- Fill test app cache and clear cache: login/data survive; reported cache changes after refresh.
- On Android 8–12 cache-only is refused without executing pm clear.
- Clear data: confirmation is required, disposable app resets.
- Attempt system/launcher/keyboard/VPN/admin targets: engine refuses.
- Batch with a mix of denied and allowed targets: successes, failures and skips are reported independently.
- Reboot with frozen test app: it remains in Frozen after Shizuku reconnect; unfreeze before uninstalling.

CI success proves build/test/lint only. Keep device validation separate from CI claims.
