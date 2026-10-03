# Morphe Manager Downloaders

Standalone APKMirror companion for Morphe Manager. Select it in LinkSheet when
Morphe opens an APKMirror link. It follows unambiguous download steps, downloads
an original APK or APKM and shares it back to Morphe using a temporary content
URI grant. Enable Morphe's **Expert mode** first; this is the default workflow.

**Status:** consolidation and local Android verification are complete. Live
APKMirror page-to-file verification is blocked by Cloudflare human verification.
This is a local verification build, not a published release or a claim of complete
APKMirror compatibility. See [verification](docs/verification.md) and the
[authoritative specification](docs/specification.md).

## Use

1. Install the companion APK alongside your existing Morphe installation.
2. Enable Expert mode in Morphe's settings.
3. Open Morphe's APKMirror download link and select **Morphe Manager Downloaders**
   in LinkSheet's app chooser. Its handlers are limited to APKMirror domains;
   it does not claim every web link or replace your general-purpose browser.
4. Choose a variant if more than one exists. Complete any site verification in
   the displayed page, then use **Continue automatically** if needed.
5. After validation, the original file opens in Morphe. **Open in Morphe** repeats
   that handoff; **Share file** offers another receiving app. If Morphe is absent,
   the companion explains the requirement and offers sharing or keeping the file.

The companion has its own package (`app.morphe.manager.downloaders`) and signing
identity. It never patches, signs or installs downloaded apps and does not replace
Morphe or touch Morphe's patch-signing keystore. Existing patched installations
keep their existing signing identity because the existing manager does the patching.

APKMirror can require manual variant selection or security checks. Android owns
transfers; the companion restores the recorded transfer when reopened. Android
may pause or fail a transfer according to network, server or system conditions.
Failures are displayed for an explicit new download. Successful original files
remain in the companion's app storage until its data is cleared or it is uninstalled.
Archive checks reject HTML and incomplete ZIP layouts; Morphe performs APK parsing,
bundle merging and patching. The companion does not certify an APK's publisher.

## Build

Requires Java 17+, Android SDK 36 and the bundled Gradle wrapper.

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./scripts/build-companion.ps1
```

The signing script builds and lints the release configuration, then copies the
local deliverable to `artifacts/Morphe-Manager-Downloaders-0.1.0.apk`. It creates a
persistent signing identity only on first use, under
`%USERPROFILE%/.android/keystores/morphe-manager-downloaders/`. Keep `companion.p12`
and `password.txt` together and private; both are needed to build updates accepted
by an existing companion installation. They are outside the repository and are
not Morphe's patch-signing key. You can supply `-SigningRoot` for your own location.

## Android verification

Use an isolated emulator, not a physical device. Start the fixture server with a
built debug APK, install the debug app and its Android test APK, then run:

```powershell
python scripts/fixture-server.py --apk <absolute-debug-apk-path>
./scripts/verify-emulator.ps1 -Serial emulator-5580
```

The fixture server binds host loopback. Only debug builds accept emulator loopback
HTTP URLs; release builds accept APKMirror HTTPS only. Fixtures verify the Android
transfer and URI contracts and do not substitute for a successful real-site check.
The script verifies an ongoing download across separate instrumentation processes.

## Sources and license

Fork of [brosssh/revanced-manager-downloaders](https://github.com/brosssh/revanced-manager-downloaders),
consolidated with [ReVanced/revanced-manager-downloaders](https://github.com/ReVanced/revanced-manager-downloaders)
v1.2.0. Both source histories are preserved. [Consolidation decisions](docs/source-consolidation.md)
record retained fork behavior and the current API/toolchain baseline. GPL-3.0 applies;
see [LICENSE](LICENSE). This companion is an independent project.

The root Gradle project remains the separate legacy ReVanced plugin, including
APKMirror, APKPure and APKCombo providers. Those extra providers do not add site
support to the Morphe companion. The broken upstream Play Store registration
remains disabled. If GitHub Packages credentials cannot resolve the legacy API,
`scripts/build-manager-api.ps1` builds the matching API sources at an immutable
commit; pass its AAR with `-PmanagerApiAar=<path>` when compiling the root project.

Publishing releases and automatically creating PRs are disabled on push. The legacy
release and PR-creation workflows require explicit manual invocation.
