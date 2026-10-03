# Morphe Downloader

Standalone APKMirror companion for Morphe Manager. Select it in LinkSheet when
Morphe opens an APKMirror link. It follows unambiguous download steps, downloads
an original APK or APKM and shares it back to Morphe using a temporary content
URI grant. Enable Morphe's **Expert mode** first; this is the default workflow.

APKMirror page resolution uses a configurable private **Byparr server**. The
companion has a native interface and no embedded browser. A real APKMirror APKM
was downloaded and imported into existing Morphe on the isolated Android emulator.
See [verification](docs/verification.md) and the [specification](docs/specification.md).

## Use

1. Install [the latest release](https://github.com/Darkaxt/morphe-manager-downloaders/releases/latest)
   alongside your existing Morphe installation. ObtainX can track this repository.
2. Enable Expert mode in Morphe's settings.
3. Open Morphe's APKMirror download link and select **Morphe Downloader**
   in LinkSheet's app chooser (double-tap its row with LinkSheet's default behavior).
   Its handlers are limited to APKMirror domains;
   it does not claim every web link or replace your general-purpose browser.
4. When prompted, enter your private HTTPS Byparr API URL, for example
   `https://your-server:8191/v1`. The setting persists. Your phone must be able to
   reach the server (for example through your existing private network).
5. Choose the required release/variant if more than one exists. Unambiguous
   download steps continue automatically.
6. After validation, the original file opens in Morphe. **Open in Morphe** repeats
   the handoff; **Share file** offers another receiving app. Morphe may display its
   own bundle/version warnings before importing a file.

There is no public default server. **Byparr server** edits the setting at any time;
missing settings and failed page/download requests offer **Save and retry**.
Retrying is explicit. Check the displayed failure as well as the URL: an unresolved
server challenge, missing release or client download rejection cannot necessarily
be fixed by changing the endpoint.

The companion has its own package (`app.morphe.manager.downloaders`) and signing
identity. It never patches, signs or installs downloaded apps and does not replace
Morphe or touch Morphe's patch-signing keystore. Existing patched installations
keep their existing signing identity because the existing manager does the patching.

APKMirror can require manual variant selection. Byparr handles page security checks;
an unresolved challenge is reported as a failure. Android owns
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
local deliverable to `artifacts/Morphe-Downloader-0.1.1.apk`. It creates a
persistent signing identity only on first use, under
`%USERPROFILE%/.android/keystores/morphe-manager-downloaders/`. Keep `companion.p12`
and `password.txt` together and private; both are needed to build updates accepted
by an existing companion installation. They are outside the repository and are
not Morphe's patch-signing key. You can supply `-SigningRoot` for your own location.

## Android verification

Use an isolated emulator for the automated verification script. Start the fixture server with a
built debug APK, install the debug app and its Android test APK, then run:

```powershell
python scripts/fixture-server.py --apk <absolute-debug-apk-path>
./scripts/verify-emulator.ps1 -Serial emulator-5580
```

The fixture server binds host loopback. Only debug builds accept emulator loopback
HTTP URLs; release builds require HTTPS for server settings and download requests. Fixtures verify the Android
transfer and URI contracts and do not substitute for a successful real-site check.
The script verifies an ongoing download across separate instrumentation processes.

The optional `ByparrLiveTest` takes instrumentation arguments `byparrEndpoint`,
`apkMirrorUrl` (a real APKM variant URL), and optionally `expectedBaseMd5` (the
listed base.apk hash). It tests the actual server, native attachment redirect and
Android download boundary. `MorpheHandoffSmokeTest` opens the resulting original;
inspect Morphe's import screen independently before claiming successful import.

The API transport is POST `/v1` with `cmd: request.get`. Upstream `request.post`
is not needed by the verified APKMirror flow and is not yet invoked. Cookies and
user agent are forwarded together, with cookies restricted to their domain/path.
Byparr resolves HTML; Android downloads the attachment. Session clearance may be
bound to the server's outgoing IP, so success with one server/network does not
establish universal portability. [Integration details](docs/byparr-assessment.md).

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
