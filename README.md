# Morphe Downloader

Standalone download companion for Morphe Manager. Select it in LinkSheet when
Morphe opens an APKMirror, APKPure, APKCombo or Uptodown link. It follows unambiguous download
steps, downloads an original APK, APKM or XAPK and shares it back using a temporary content
URI grant. Enable Morphe's **Expert mode** first; this is the default workflow.

Page resolution uses a configurable private **Byparr server**. The companion has
a compact native dialog, Morphe artwork with a download badge, and no embedded
browser. It shows actual status and measured transfer progress. Once ready, the
dialog shows the archive's app label, package ID and version/build like InstallerX.
Live Android download/validation/readable-URI checks passed for APKMirror,
APKPure, APKCombo and Uptodown; an APKMirror APKM was also imported into existing Morphe
on the isolated Android emulator.
See [verification](docs/verification.md) and the [specification](docs/specification.md).

**Uptodown requires Byparr's browser scripting update** (`custom-post-scripting`
or a newer scripting/queue version).
The companion supplies the verified recipe and lets the website generate its own
token. Older Byparr servers remain usable with the other sources. See
[Uptodown integration](docs/uptodown.md).

## Use

1. Install [the latest release](https://github.com/Darkaxt/morphe-manager-downloaders/releases/latest)
   alongside your existing Morphe installation. ObtainX can track this repository.
2. Enable Expert mode in Morphe's settings.
3. Open Morphe's APKMirror, APKPure, APKCombo or Uptodown link and select **Morphe Downloader**
   in LinkSheet's app chooser (double-tap its row with LinkSheet's default behavior).
   Its handlers are limited to those supported source domains;
   it does not claim every web link or replace your general-purpose browser.
4. When prompted, enter your private HTTPS Byparr API URL, for example
   `https://your-server:8191/v1`. The setting persists. Your phone must be able to
   reach the server (for example through your existing private network).
5. Choose the required release/variant if more than one exists. Unambiguous
   download steps continue automatically.
6. After validation, the original file opens in Morphe. **Open in Morphe** repeats
   the handoff; **Share file** offers another receiving app. Morphe may display its
   own bundle/version warnings before importing a file.

There is no public default server. The cog opens **Byparr server** at any time;
its setup dialog offers **Cancel** and **Save**. Failures stay in the download
dialog with **Retry** and **Open in Browser**. Saving retries a pending page
explicitly. Check the displayed failure as well as the URL: an unresolved
server challenge, missing release or client download rejection cannot necessarily
be fixed by changing the endpoint.

With Byparr's transactional queue update, page requests wait their turn behind
other consumers without an opt-in setting or automatic retry. Cancel disconnects
the pending request. Queue waiting does not use the server's browser execution
budget. A full backlog is reported separately so you can retry explicitly;
queuing does not resolve a website's independent Cloudflare rejection.

With Byparr's live feedback update (`custom-post-scripting-queue-feedback`), the
dialog shows your current position among waiting requests and how many are queued
ahead. The active browser owner is excluded from that count. When admitted, the
status changes to resolving the download; transfer progress then shows measured
bytes in readable B/KB/MB/GB units. Each browser operation has its own UUID and
feedback stops when it finishes or is cancelled. Unavailable or malformed feedback
does not complete, cancel or resubmit the original operation. Older servers retain
the ordinary status display.

The companion has its own package (`app.morphe.manager.downloaders`) and signing
identity. It never patches, signs or installs downloaded apps and does not replace
Morphe or touch Morphe's patch-signing keystore. Existing patched installations
keep their existing signing identity because the existing manager does the patching.

Download sites can require manual version/variant selection. Byparr handles page security checks;
an unresolved challenge is reported as a failure. Android owns
transfers; the companion restores the recorded transfer when reopened. Android
may pause or fail a transfer according to network, server or system conditions.
Failures are displayed for an explicit new download. Successful original files
remain in the companion's app storage until its data is cleared or it is uninstalled.
Archive checks reject HTML and incomplete ZIP layouts; Morphe performs APK parsing,
bundle merging and patching. The companion does not certify an APK's publisher.

## Build

Requires JDK 21, Android SDK 36 and the bundled Gradle wrapper. Set `JAVA_HOME`
to your existing JDK 21 installation. Gradle, compilation and tests use that
installation; automatic JDK discovery/downloads are disabled for this project.
Java and Kotlin retain JVM target 17 for Android compatibility.

```powershell
python "$env:USERPROFILE/.codex/skills/gradle-build-gate/scripts/invoke_gradle_build_gate.py" run --project $PWD --kotlin-strategy in-process -- :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./scripts/build-companion.ps1
```

The signing script builds and lints the release configuration, then copies the
local deliverable to `artifacts/Morphe-Downloader-<version>.apk`. On Windows it
requires the installed shared Gradle build gate (or `-BuildGate <helper-path>`),
which serializes participating builds and applies the resource profile. It creates a
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
`downloadUrl` (a real supported-source URL), and optionally `expectedFormat`,
`expectedPackage`, `expectedVersion`, `expectedMd5` (an APK hash) or
`expectedBaseMd5` (an APKM base.apk hash). It tests the actual server, native attachment redirect and
Android download boundary. `MorpheHandoffSmokeTest` opens the resulting original;
inspect Morphe's import screen independently before claiming successful import.

The API transport is POST `/v1` with `cmd: request.get`. Upstream `request.post`
is not needed by the verified sources and is not invoked. Cookies and
user agent are forwarded together, with cookies restricted to their domain/path.
APKCombo's `noreferrer` attachment policy is preserved.
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
APKMirror, APKPure and APKCombo providers. Its providers are separate from the
standalone companion's implementations. The broken upstream Play Store registration
remains disabled. If GitHub Packages credentials cannot resolve the legacy API,
`scripts/build-manager-api.ps1` builds the matching API sources at an immutable
commit; pass its AAR with `-PmanagerApiAar=<path>` when compiling the root project.

Publishing releases and automatically creating PRs are disabled on push. The legacy
release and PR-creation workflows require explicit manual invocation.
