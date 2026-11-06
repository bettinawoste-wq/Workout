# Releasing

A release is a signed APK attached to a GitHub Release. It is built by
[`.github/workflows/release.yml`](../.github/workflows/release.yml) when a tag like `v1.0.0`
is pushed. There is no Play Store publishing.

## One-time setup: the signing key

An installed app only accepts updates signed with the same key, so create one key and keep it.
The keystore is never committed (`*.jks` doesn't belong in this repo); the workflow gets it from
GitHub Secrets.

1. Create the keystore. `keytool` ships with the JDK / Android Studio's `jbr`. It asks for the
   passwords and your name interactively:

   ```bash
   keytool -genkeypair -v -keystore workout-release.jks -alias workout \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Back up `workout-release.jks` and both passwords** (password manager). If the key is lost,
   people who installed an APK have to uninstall before they can install a new one.

3. Store the four secrets. `gh secret set` asks for each value, so nothing ends up in your shell
   history:

   ```powershell
   # PowerShell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("workout-release.jks")) | gh secret set KEYSTORE_BASE64
   gh secret set KEYSTORE_PASSWORD
   gh secret set KEY_ALIAS          # the alias from step 1, e.g. workout
   gh secret set KEY_PASSWORD
   ```

   | Secret | Content |
   | --- | --- |
   | `KEYSTORE_BASE64` | the keystore file, base64-encoded |
   | `KEYSTORE_PASSWORD` | the keystore password |
   | `KEY_ALIAS` | the key alias |
   | `KEY_PASSWORD` | the key password |

4. Check the secrets without publishing anything: GitHub → Actions → **Release** → *Run workflow*.
   It builds the signed APK and uploads it as a workflow artifact only.

## Cutting a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow runs the unit tests and lint, builds the release APK, and creates a GitHub Release
named after the tag with generated notes and `workout-1.0.0.apk` attached.

- `versionName` is the tag without the leading `v`.
- `versionCode` is the workflow run number, so every newer release can update an older install.
- A release build without the keystore (any local build) is signed with the debug key instead;
  that APK is only meant for your own device.
