# Persistent release signing

The public certificate fingerprint is pinned in `distribution/certificate.sha256` and `UpdatePolicy.SIGNER`. The private PKCS12 key and password must stay outside this public repository, public Actions artifacts, logs and caches. Reuse the private backup named `DATACLIAR-signing-backup.zip`; never generate a replacement for an update. The maintainer's private backup is kept separately.

The current GitHub connector cannot configure repository signing secrets. Therefore CI deliberately builds unsigned candidates and never falls back to a rotating debug key for published releases. A maintainer signs locally with the persistent private key, then supplies only the public signed APKs. CI validates their signatures and compares every non-signature ZIP entry against a fresh build from the current source before publishing. Signing files is not automated inside public CI; a future secret-backed signing job may be configured by the repository owner.

## Release procedure

1. Bump `app/version.properties`: both versionCode and versionName. Preserve the public signing fingerprint.
2. Push source. CI uploads `unsigned-signing-candidate`: the release APK, a lower-version upgrade fixture, instrumentation APK and official Android apksigner.jar. No release is published without a matching signed candidate.
3. Download that artifact to a private local workspace. Materialize the original private signing backup separately. Run:

```sh
python scripts/sign-release.py --candidate /absolute/unsigned --key-directory /absolute/private-signing --output /absolute/signed
```

4. Supply the resulting `DATACLIAR.apk`, `upgrade-base.apk` and `instrumentation.apk` under `distribution/`. Never supply the key directory, backup ZIP, passwords or .idsig files.
5. CI checks the pinned certificate and exact unsigned build contents for all three APKs, runs tests, performs an actual version-increment installation with `adb install -r`, and verifies favourites/history persisted and interrupted operations were not repeated.
6. CI publishes the signed main APK, `update.json` and checksums at `v<versionName>`. Published releases are immutable; reruns compare existing asset bytes instead of replacing them.

The lower-version APK is only a CI upgrade fixture with the same source and key. This does not make old debug-signed previews upgrade-compatible; they may require a one-time migration. Only the main APK is included in release assets.
