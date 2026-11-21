# Releasing Bou

Bou ships two ways: a ready-to-install **APK** and a **patch bundle** (`.mpp`) that people apply
to stock Gboard themselves with Morphe Manager (ReVanced-style). Both come from the same build.

## Licence rule (read first)

Bou is based on PixelBoard (GPL-3.0). The repo may stay private while we develop, but **the day
an APK or patch bundle is published, the source must be public too** (same commit as the
release). Never publish a release from a private repo. Keep `LICENSE` and the upstream credits.

## Base app

Gboard **18.6.3.100** arm64 only: `input/gboard.apk` (gitignored), SHA-256
`2b1c28cf650894ead8a95224ac15fce246e61cd54d11e2149c2a13fc24b30d84`, Google-signed
(signer SHA-256 `f0fd6c5b…db83`). Download page: APKMirror "Gboard 18.6.3.1002251336-release
arm64-v8a nodpi". Check the hash before every build.

## One-time setup (Windows)

- JDK 21: `winget install Microsoft.OpenJDK.21`
- Android SDK build-tools 36.1.0 and platform-tools (adb) under `%LOCALAPPDATA%\Android\Sdk`.
- GitHub CLI logged in with scopes `repo`, `read:packages` (Morphe Gradle plugin lives on GitHub
  Packages): `gh auth refresh -h github.com -s read:packages`
- Release key: `tools/patcher-data/bou-release.keystore`, alias `bou`, password in
  `.env.secrets.local` as `BOU_KEYSTORE_PASSWORD` (both gitignored; see `LOCAL-NOTES.md`).
  Back them up. Losing the key means users must uninstall to update.

Environment for every command below (Git Bash):

```bash
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
export JAVA_HOME="$(ls -d "/c/Program Files/Microsoft/"jdk-21* | head -1)"
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"   # Java downloads time out over IPv6 here
export GITHUB_ACTOR=mohajansreview-cmyk GITHUB_TOKEN="$(gh auth token)"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/build-tools/36.1.0:$PATH"
```

## Release steps

1. **Version.** Bump `version` in `pixelboard-patches/gradle.properties` (e.g. `1.4.0`).
2. **Word data** (only when the word lists changed):
   `python -I -X utf8 tools/build_banglish_data.py <bn_50k.txt> <en_50k.txt>`
   (lists from github.com/hermitdave/FrequencyWords, content/2018; regional words are read from
   `tools/data/onubad_regional_words.txt`).
3. **Test and build the patch bundle:**
   `cd pixelboard-patches && ./gradlew --no-daemon :extensions:extension:testDebugUnitTest :patches:test :patches:buildAndroid`
   Every test must pass. Output: `pixelboard-patches/patches/build/libs/patches-<version>.mpp`.
4. **Patch and sign the APK:**
   ```bash
   KP=$(grep ^BOU_KEYSTORE_PASSWORD= .env.secrets.local | cut -d= -f2-)
   java -jar tools/patcher.jar patch --patches=pixelboard-patches/patches/build/libs/patches-<version>.mpp \
     --options-file=options.json --striplibs=arm64-v8a --signer=Bou \
     --keystore=tools/patcher-data/bou-release.keystore --keystore-password="$KP" \
     --keystore-entry-alias=bou --keystore-entry-password="$KP" \
     --temporary-files-path=tools/patcher-data/tmp -o=output/Bou.apk -r=output/patching-result.json input/gboard.apk
   KEYSTORE_PASSWORD="$KP" KEYSTORE_ALIAS=bou python -I -X utf8 tools/sanitize_apk_resources.py \
     output/Bou.apk input/gboard.apk tools/patcher-data/bou-release.keystore
   git checkout -- tools/morphe-data/imported.keystore   # the patcher rewrites it
   ```
   The patch log must show 4 `Applied` lines.
5. **Verify:**
   - `java -jar $ANDROID_HOME/build-tools/36.1.0/lib/apksigner.jar verify --print-certs output/Bou.apk`
     → signer `CN=Bou, O=Subrata Mohajan, C=BD`.
   - `aapt2 dump badging output/Bou.apk` → package `com.subratamohajan.bou`, label `Bou`.
6. **Phone test** (owner's HONOR 200 over adb): `adb install -r output/Bou.apk`, then speak with
   the Rambler mic: one standard sentence, one mixed English sentence, one regional sentence.
7. **Rename assets:** `Bou-<version>.apk`, `patches-<version>.mpp`.
8. **Patch bundle manifest:** update `patches-bundle.json` at the repo root:
   `version`, `created_at`, a short changelog in `description`, and
   `download_url` = `https://github.com/mohajansreview-cmyk/<repo>/releases/download/v<version>/patches-<version>.mpp`.
9. **Merge and tag** (owner's go-ahead): PR `feat/*` → `main`, then
   `git tag v<version> && git push origin v<version>`. No AI attribution anywhere.
10. **Make the source public** (first release only, owner's go-ahead): repo Settings → General →
    Danger Zone → Change visibility → Public. Check `git ls-files` has no key, `.env` or APK first,
    and run `gitleaks git --no-banner --redact`.
11. **GitHub Release:** `gh release create v<version> output/Bou-<version>.apk patches-<version>.mpp --title "Bou <version>" --notes-file <notes>`
12. **Website:** the download buttons point at
    `https://github.com/mohajansreview-cmyk/<repo>/releases/latest/download/Bou.apk` style URLs;
    when the repo goes public, set `NEXT_PUBLIC_RELEASES_LIVE=true` in Vercel and redeploy so the
    "শীঘ্রই আসছে" buttons turn into downloads.

## Morphe Manager (patch route) for users

1. Install Morphe Manager.
2. Patch sources → Add → paste the raw URL of `patches-bundle.json` from the public repo.
3. Pick stock Gboard 18.6.3.100 (APKMirror) → select all 4 patches → Patch → Install.
4. Keep the patched app; it installs beside stock Gboard as "Bou".

## In-app updates

The app checks `https://api.github.com/repos/mohajansreview-cmyk/Bou/releases/latest`
(`PixelBoardUpdateManager.GITHUB_REPO_API`) and offers the first `.apk` asset of the latest
release. For that to work:
- Rename the GitHub repo from `PixelBoard` to `Bou` before the first public release
  (Settings → General → Repository name), or change the two URLs in `PixelBoardUpdateManager`.
- Every release must be signed with `bou-release.keystore`; Android refuses an update signed
  with a different key.
- Bump the version so it is higher than the installed one. Android never installs silently
  outside the Play Store: the user gets a notification and taps Install once.

## If a release is taken down

The patch bundle route keeps working as long as the source repo is up: users patch their own
Gboard. Keep the website guide for that route current.
