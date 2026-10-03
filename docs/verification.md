# Verification record: 2026-10-03

## Real Byparr and APKMirror boundary

The Byparr-only companion passed the actual private HTTPS server boundary on an
isolated Android 15 emulator (`emulator-5580`). Byparr 3.0.4 resolved:

`https://www.apkmirror.com/apk/google-inc/youtube/youtube-21-39-523-release/youtube-21-39-523-android-apk-download/`

The client POSTed `request.get` to `/v1`, parsed the variant and download landing
HTML, followed the native attachment redirect and downloaded the signed R2 URL
using Android DownloadManager. This is an actual original-file download, not only
an HTML/cookie response or fixture.

- Original APKM: 72,237,474 bytes.
- APKM SHA-256: `b915320883868ce20abb80fc02df398eb581543551858835a3ed832e937bdaad`.
- Contained base.apk MD5: `9a9a84346eba90a7a856d00525d11d31`, equal to APKMirror's
  listed bundle-entry hash. This compares file bytes, not publisher certificate hashes.
- Official Morphe 1.33.0 received the original content URI, displayed its split-bundle
  warning and parsed version 21.39.523/build 1561299910. After acknowledging its
  warnings, YouTube's Expert-mode patch selection was independently inspected.
  **No Patch button was pressed and no downloaded app was installed.**
- This version was used to prove download/import, not compatibility with the
  installed patch set. Morphe flagged it as unsupported and recommended 21.16.256.
  The companion preserves the requested version and leaves patch compatibility to Morphe.

Raw signed URLs, session cookies and private endpoint settings are excluded from
public source. Sanitized hashes, emulator logs, screenshots and UI hierarchies are
retained locally in `artifacts/verification/`. `byparr-live.txt` and
`byparr-morphe-selected.png` record the original and actual receiver state.

## Focused current-source verification

- Debug APK/test APK and locally signed release APK build successfully.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease` and
  `:app:lintRelease` pass. No lint baseline hides errors. Warnings include durable
  preference commits, optional dependency upgrades and localization guidance.
- JVM checks cover endpoint normalization/restrictions, exact attachment-host policy,
  escaped-query preservation, ambiguous variants, challenge/not-found content,
  cookie domain/path/secure/expiry rules and APK/APKM structural validation.
- Native Android checks cover a missing endpoint prompt, persisted endpoint after
  recreation, failed API response followed by explicit endpoint edit/retry,
  manual variant choices, unresolved challenge errors and absence of a WebView.
- The mock Byparr HTTP boundary leads through real Android DownloadManager to an
  original fixture APK, requiring cookie/User-Agent/Referer headers. A separate
  installed receiving APK reads the content URI with equal SHA-256, name and MIME.
- Pending/completed transfer recreation, cancellation, invalid HTML archive rejection
  and retained successful originals pass. Separate instrumentation processes with a
  force-stop between them recover a pending download and validate it after reopening.
- Official LinkSheet 0.0.33 previously offered the companion for an APKMirror link;
  its manifest handlers/package identity are unchanged. VIEW/BROWSABLE resolution
  lists the companion alongside Chrome without claiming general browser links.
- Native action layout and real Morphe selection screenshots were visually reviewed.

## Provenance and local artifact

Canonical `41b9ab3` and brosssh `e9441f0` remain ancestors through merge `03baaf8`.
The root legacy downloader compiles against unchanged API sources pinned at
`api@1.0.0-dev.4` (`b7e94efb`). It remains a separate legacy plugin; only the
standalone companion uses the new Byparr-only interface.

Local signed deliverable: `artifacts/Morphe-Manager-Downloaders-0.1.0.apk`.

- APK SHA-256: `67b04bddb602dbecea298f494994a0dd294f48e7aa1b49e94fb6284b29dfa834`.
- Signing certificate SHA-256:
  `4b99c36a398c62dd2eb818ed8ae6a56fff6081fc9c77990fe1998335c5d08c11`.
- apksigner verification passes with v2 signing; minimum Android 8.
- Persistent companion signing key/password remain outside Git. Morphe and its
  patch-signing identity are unchanged. The verified 0.1.0 artifact was subsequently
  published and installed through ObtainX on the authorized phone.

## Physical phone and corrective build

GitHub v0.1.0 and the phone-installed base.apk both match the recorded release
SHA-256. ObtainX tracks this repository. Its existing InstallerX Resign route
installed the original signature (Install, not Resign); first-use notification
and all-files permissions were required for that installer.

On Samsung SM-F966B / Android 16, existing Morphe 1.33.0-dev.17 selected Showly
3.72.0 and opened its actual APKMirror variant through LinkSheet nightly. The row
requires a double-tap with its default configuration. The configured private
Byparr server resolved the page and Android downloaded the correct original:

- Package `com.michaldrabik.showly2`, version 3.72.0/build 843.
- 12,004,991 bytes; SHA-256 `3ec55fffbb2614c810e49f9e3441d4f29c0e263e4f8dd6d5fdbec1e4d0ea1708`.
- MD5 `ebbea07e896f21d3c0a559c4223f9c62`, matching APKMirror's displayed file hash.

The initial import failed: Samsung's DownloadManager wrote `downloadfile.apk`
instead of the requested filename. The regression reproduces the stale requested
path while a real DownloadManager transfer succeeds. It fails on 0.1.0 and passes
with completed `COLUMN_LOCAL_URI` validation in 0.1.1, including equal bytes at a
receiving content URI. Endpoint, failure/cancellation/recreation checks pass on the
isolated emulator. JVM checks, signed release build, release lint and v2 signature
verification pass. The signing certificate is unchanged.

Corrective deliverable: `artifacts/Morphe-Downloader-0.1.1.apk`, 2,083,318 bytes;
SHA-256 `e5a214ca7949d2fac3f842ec05df7e4db7cb6fc2fb12c79b4b5ea05de9ee7170`.
The independent physical-phone import retest of this build is still pending.
Stage 5 is BLOCKED by the disconnected phone. The user subsequently resumed work
and reported successful patching/installing after changing Morphe's installer.
This is user-reported success, not new independent phone verification. See the
historical checkpoint and later refinement scope in the specification.

## Testing limits

The complete new Byparr path is verified on the emulator. On the physical phone,
the original download passed but import exposed the file-location defect above.
The former embedded-browser build was installed for authorized testing on the
Samsung SM-F966B; Cloudflare verification looped there as well. External Chrome/
Firefox success was user-reported, not independently captured. The AYN Thor was
not used. No changes to phone network settings, Morphe keys, or patched apps were made.

A phone needs access to the configured HTTPS server. Cloudflare may bind clearance
to an outgoing IP; the tested server/emulator passed native attachment retrieval,
but this is not a universal guarantee for every server/network or later site change.
Failures remain explicit and offer endpoint editing/retry. No relay or upstream
POST workflow is claimed. See [Byparr integration](byparr-assessment.md).

## Reproduction

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
python scripts/fixture-server.py --apk <absolute-debug-apk-path>
./scripts/verify-emulator.ps1 -Serial emulator-5580
./scripts/build-companion.ps1
```

For the optional live test, install the debug and test APKs on the isolated emulator
and invoke `ByparrLiveTest` with instrumentation arguments `byparrEndpoint` and
`downloadUrl`; supply `expectedBaseMd5` for an APKM base, or `expectedMd5`,
`expectedPackage` and `expectedVersion` for an APK. Keep the endpoint
private. Run `MorpheHandoffSmokeTest` before fixture tests replace the active file,
then independently inspect Morphe's import screen. A successful Activity launch
alone does not prove parsing/import. Stop before patching/installing target apps.

## 0.2.0 refinement release verification

The user authorized publishing the completed presentation/APKPure/APKCombo changes
while keeping Uptodown pending until the Byparr browser-action update is ready.
Stage 9's Uptodown boundary and Stage 5's independent phone retest are excluded
from this release scope, not declared complete.

- Morphe's exact launcher vector and a small original download badge render in
  the compact native dialog. Light/dark and narrow/wide presentation was visually
  inspected. Settings retain a 48dp accessible target; content can scroll.
- The title becomes `Downloading {app name}` after page metadata arrives. Bottom
  text represents resolution, measured transfer, validation and completion. It
  does not invent progress during Byparr resolution. Manual variants use source
  labels, not fixture labels: the real APKCombo Showly entry includes version
  `3.72.0 (843)`, APK, `11 MB`, Android requirement and DPI range.
- Real APKPure and APKCombo Showly 3.72.0 workflows passed on Android 15:
  configured Byparr page -> native attachment redirect -> Android DownloadManager
  -> archive validation/package/version checks -> separate receiver reading the
  unchanged original through its granted URI. APKCombo's link declared
  `noreferrer`; preserving that policy fixed its rejected attachment redirect.
- The two providers returned the same 12,004,991-byte original:
  SHA-256 `f0fac7fc5474168ca982fd220361f3bbb76d4f4f3ed9e89cf03d2465d2b77074`,
  MD5 `e1bb5ea5b4e0d866c3159034076f3c90`, package
  `com.michaldrabik.showly2`, version 3.72.0/build 843. Signature verification
  passed. APKMirror's container hash differs, but ZIP entry payloads and signer
  certificates match; it was not treated as a provider-independent binary hash.
- APK/XAPK structural checks, requested-version rejection, advert exclusion,
  source-specific host policy and cookie contracts pass the current JVM checks.
  XAPK uses Morphe's declared `application/x-xapk` MIME. Live added-source proof
  used APKs; it does not claim a new live XAPK import into Morphe.
- Fresh Android checks pass for explicit attachment choice with its resolved
  session, unchanged receiver bytes, missing/saved endpoint, explicit error retry,
  unresolved challenge, HTML rejection, cancellation and recreation. Separate
  instrumentation processes with a force-stop restore a recorded transfer.
- A stronger completed-window regression reproduced reprocessing the original
  VIEW intent on recreation. Creation now restores persisted state when Android
  supplies saved instance state. Both automatic and manually selected downloads
  remain ready with the same download ID after recreation; the check waits for
  the actual ready presentation rather than observing preferences too early.
- The release APK has the existing signing certificate, version 0.2.0/code 3,
  Android 8 minimum and target SDK 36. Release build/lint and v2 APK signature
  verification pass; lint has 0 errors and existing localization/dependency/style
  warnings. No lint baseline was added.
- An actual signed 0.1.1 -> 0.2.0 in-place upgrade passed on the task emulator,
  preserving a saved HTTPS endpoint. The installed release handles APKMirror,
  APKPure com/net and APKCombo; it does not handle Uptodown or arbitrary web links.
- No Byparr source/deployment, physical phone, AYN Thor, Morphe patch-signing key
  or patched application was changed for this release. Uptodown remains pending
  as documented in the [handoff](uptodown-byparr-handoff.md).

Local signed deliverable: `artifacts/Morphe-Downloader-0.2.0.apk`, 2,092,502 bytes.
APK SHA-256:
`60ec0eeafcdd3de91aa1368187fc5f2c6f5431b833f8e8576eda8cce918e7750`.
Certificate SHA-256 remains
`4b99c36a398c62dd2eb818ed8ae6a56fff6081fc9c77990fe1998335c5d08c11`.
Local evidence is retained under `artifacts/verification/refinement/`, including
signed build/lint, Android contracts, separate-process restoration, signature,
upgrade/handler checks and live provider logs. Private traffic captures and signed
links remain gitignored and are not published.

[v0.2.0](https://github.com/Darkaxt/morphe-manager-downloaders/releases/tag/v0.2.0)
is published as a normal/latest release targeting source commit
`7b875c106b40679288282002021f0ce75786e8e7`. The published APK was independently
downloaded and matched the local deliverable byte-for-byte; GitHub's asset digest
also matches the SHA-256 above. Exact task-generated Temp roots were removed
through reviewed cleanup tickets after preserving deliverables/evidence and
stopping the task emulator, fixture server and idle task Gradle daemon. The owned
capture tab was closed; other user browser tabs and build processes were preserved.
