# DATACLIAR 1.0.0 preview

Native Android dashboard with RAM/storage meters, CPU sampling, searchable per-app storage, running process snapshot and Shizuku app controls. Freeze/unfreeze, sleep/wake, explicit data clearing and batch cache/stop operations are included.

- Android 8+; safe cache-only operations require Android 13+ and OEM command support.
- Usage Access required for storage sizes; Shizuku required for other-app controls.
- Protected system/essential apps remain read-only.
- Installable **debug-signed preview**. Not a production-signed release; future previews can require uninstall/reinstall due to signing certificates. Unfreeze apps before uninstalling.
- Build, unit tests and lint run before publication. Real-device Shizuku/OEM compatibility is not certified by compilation.
- Verify download using SHA256SUMS.txt. No analytics or Internet permission.
