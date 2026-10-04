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

## Unreleased ready metadata and setup labels (2026-10-03)

The ready dialog follows the inspected InstallerX source and captured phone
dialog: app label as the prominent centered title, then package ID and version
name/build as smaller centered details. Details come from the original APK's
Android manifest/resources, not its filename or the download website's title.
Android PackageManager reads plain APKs and a temporarily extracted bundle base
APK off the UI thread. Temporary base files are removed; metadata is persisted
atomically with readiness. Older ready records are populated once on reopening.
Optional metadata failure retains the filename and the structurally validated file.

Fresh verification on the isolated API 35 emulator includes:

- An initial ready metadata regression fails against the old filename display.
  Saved plain APK, APKM base, XAPK base with an invalid split candidate, recreation,
  metadata fallback and unchanged archive hashes then pass. Bundle inputs are
  controlled containers containing a real APK, not live provider bundle downloads.
- The retained original Showly 3.72.0 APK shows `Showly`,
  `Package: com.michaldrabik.showly2`, and `Version: 3.72.0 (843)`.
  Independent aapt inspection confirms the values. Its bytes remain unchanged.
  The compact ready layout is visually inspected in both light and dark themes.
- Automatic/manual download, completed-window recreation, actual DownloadManager
  local URI, unchanged receiver hash/filename/MIME, bounded dialog/accessibility,
  failure/challenge/cancellation and separate-process transfer restoration pass.
- Byparr setup displays `Cancel` and `Save`. The focused endpoint save/recreation
  and failed-page edit/retry contracts pass with the shortened labels.
- Current debug/test APK builds and companion JVM contracts pass. Final debug
  lint has zero errors; its existing preference/localization/dependency/style
  warnings remain. No lint baseline or production timeout/retry was added.

Evidence and ready dialog screenshots are retained locally under
`artifacts/verification/metadata/`. This source refinement is not a new published
release; the verified 0.2.0 asset is preserved. Byparr and physical devices were
unchanged. Existing independent phone and Uptodown criteria remain blocked as
recorded in the specification; this presentation change does not close them.
The task emulator, fixture server and idle task Gradle daemon were stopped after
verification. The exact registered temporary build/emulator root was removed
through a reviewed cleanup ticket after retaining the required evidence.

## 0.3.0 Uptodown and metadata release verification (2026-10-04)

R20 authorizes integrating the deployed generic scripting API and publishing a
new release including R19. The recipe is byte-identical to Byparr commit
`5671f2474493d6fb2ce5ab5d2cdd4e7fbc47db2f`; no backend source or deployment changed.

- The original endpoint reports `custom-post-scripting`. The exact Showly build
  page resolves through the packaged recipe, with the website generating its
  fresh token and POST. The captured successful response and aborted attachment
  request are validated before native Android transfer.
- Policy/parser contracts preserve requested file IDs, restrict attachment hosts
  and reject unrelated builds. Android capture contracts reject missing scripting,
  failed/different POST responses, non-GET/non-aborted requests, mismatched signed
  URLs/user agents and invalid headers. A reproduced JSON coercion regression
  now rejects string values in place of boolean abort/integer status fields.
- The final signed 0.3.0 APK passed a fresh live Uptodown -> Android DownloadManager
  -> archive validation -> metadata -> separate receiver readable-URI workflow.
  The original is 12,004,991 bytes, package `com.michaldrabik.showly2`, version
  3.72.0/build 843, SHA-256
  `f0fac7fc5474168ca982fd220361f3bbb76d4f4f3ed9e89cf03d2465d2b77074`.
  Independent aapt inspection confirms the metadata. The ready dialog visibly
  displays Showly, its package ID and `3.72.0 (843)`.
- Focused ready metadata checks cover plain APK, controlled APKM/XAPK base
  extraction, legacy state, recreation, fallback, unchanged bytes and cache cleanup.
  Automatic/manual transfer, URI filename/MIME/byte identity, presentation,
  failure/challenge/cancellation and separate-process restoration pass. These
  controlled checks preserve the existing three-source workflow contracts.
- An actual signed 0.2.0 -> 0.3.0 in-place update preserved the endpoint entered
  through the setup UI. Upgrade instrumentation is explicitly opt-in; default
  execution skips both version-specific checks. Installed Uptodown page handlers
  include the exact build URL and exclude the attachment URL.
- Final release build and lint pass: zero lint errors, 79 warnings, no baseline.
  APK v2 signature verification passes with the existing certificate. Package
  `app.morphe.manager.downloaders`, version 0.3.0/code 4, minimum SDK 26, target 36.

Signed deliverable: `artifacts/Morphe-Downloader-0.3.0.apk`, 2,101,819 bytes.
SHA-256:
`40180b3297d9e2b1197522002dab5875c11c09cdde90af5d5776a452fcf199f6`.
Certificate SHA-256:
`4b99c36a398c62dd2eb818ed8ae6a56fff6081fc9c77990fe1998335c5d08c11`.
The published 0.2.0 local artifact remains unchanged. Evidence is retained under
`artifacts/verification/uptodown/`; private captures/endpoints are not published.
This proves the isolated Android original-download/handoff boundary. It does not
claim physical-phone Morphe patching or installation; independent Stage 5 remains
blocked outside this release's scope. Stage 9's backend blocker B4 is resolved.
The task emulator, fixture server and idle task Gradle daemon are stopped. Both
exact registered temporary roots were removed through reviewed cleanup tickets;
the signed deliverable and required evidence remain. Physical devices and user
browser sessions were unchanged.

[v0.3.0](https://github.com/Darkaxt/morphe-manager-downloaders/releases/tag/v0.3.0)
is published as the normal/latest release targeting verified source commit
`d10ba91513c31a851a0d5fa7caa62aa8e3d57099`. Its only asset is the signed APK above.
An independent download matched the local tested deliverable byte-for-byte;
GitHub's asset size and SHA-256 digest also match. Stage 12 is COMPLETE.

## Local 0.3.1 error-dialog update (2026-10-04)

Failures remain in the compact download form with Retry and Open in Browser.
Failure-action checks verify no automatic configuration prompt, recreation,
saved-server preservation, explicit successful retry, migration from an expired
APKMirror landing URL, and chooser exclusion of the companion. A reproduced
regression where a failed new request offered an older completed APK was fixed
and verified. Lifecycle checks retain first-use setup, cog editing, cancellation
and rejection of HTML/unresolved challenges. Ready metadata checks and existing
source-policy/parser/cookie/endpoint contracts pass.

Release build/lint and APK signature verification pass. The signed local APK
upgrades 0.3.0 in place while preserving the UI-saved endpoint. The signed APK's
external chooser and inline error presentation passed an emulator check and
visual inspection. Logs and the dialog image are retained under
`artifacts/verification/apkmirror-phone/`.

- APK: `artifacts/Morphe-Downloader-0.3.1.apk`, version code 5; 2,103,319 bytes.
- SHA-256: `b7d5d1ad62399f52ff83bbe20ce06f645b3d9f0c47386b195e32d6d32aa73ca9`.
- Certificate SHA-256: `4b99c36a398c62dd2eb818ed8ae6a56fff6081fc9c77990fe1998335c5d08c11`.

At the initial checkpoint this was a local artifact; it was subsequently
published under R24, as recorded below. The APKMirror automatic
correction and physical-phone readiness/receiving-URI proof remain blocked, as
recorded in specification B5/B6 and the APKMirror Byparr handoff. No downloaded
YouTube original was patched or installed. Byparr and its other consumers were
not changed. After preserving the diagnostic evidence and local APK, reviewed
cleanup removed the exact task-owned temporary root, including private captures,
emulator data and build intermediates. The task emulator, fixture server and
completed task-owned Gradle daemon were stopped; other builds were left running.

The user added a host-wide Gradle constraint after these builds finished. No
Gradle command was launched afterward, so the new mutex/profile has not yet been
exercised or claimed as verified. Earlier builds used the existing 2 GiB Gradle
and Kotlin daemon heaps with default worker settings; no memory-related build
failure occurred. A subsequent required build must acquire
`Local\Darka.AndroidGradleBuildGate` before host activity checks, hold it until
command exit, use two workers/no parallel execution, and start with explicit
3 GiB Gradle/Kotlin heaps. Kotlin 2.3.10/AGP 8.13.2 currently used daemon
compilation. In-process compilation could remove the separate compiler JVM but
would share the Gradle heap with AGP; it has not been validated under the new
budget and should be evaluated in that next required focused build.

## Published 0.3.1 dialog update (2026-10-04)

[v0.3.1](https://github.com/Darkaxt/morphe-manager-downloaders/releases/tag/v0.3.1)
is the normal/latest release targeting source
`dc476932fe7ccf295dbc95c1049a3046b94d07af`. It includes the verified dialog
recovery changes and accurate unresolved-APKMirror notes. The single uploaded APK
matches the size/digest above; an independently downloaded copy matched every
byte of the tested local artifact. Temporary release verification files were
removed through a reviewed cleanup ticket. No additional Gradle build occurred.
Stage 14 is COMPLETE; Stage 13's automatic correction/phone proof stays BLOCKED.

## Transactional Byparr admission / 0.3.2 (2026-10-04)

The original endpoint reports `custom-post-scripting-queue`; its running image
matches the qualified Byparr image `38f540160714261a43a82a094c47440d9a4f02c24aed0b3967d150e0c3b758de`.
Reviewed server admission evidence confirms FIFO, bounded backlog, disconnect
removal and unchanged production configuration. This task did not change Byparr
or its other consumers.

Actual Android transport checks held one controlled browser operation on that
endpoint. The Android client's queued request was cancelled and removed while
the owner remained held, proving no browser admission for the cancelled request.
A second queued request resolved the exact YouTube 21.16.256 APKMirror page after
owner release. The test's two owner pages were submitted once each; its temporary
loopback fixture process was stopped. This proves page resolution and cancellation,
not an APKMirror attachment Cloudflare correction.

The full-backlog regression reproduced the generic 503 message before the change.
It now distinguishes only the exact JSON queue-full response; another 503 remains
generic. Focused Android failure/lifecycle and Uptodown capture contracts pass.
The first UI run was obstructed by a fresh-emulator Google Messages ANR dialog;
after clearing that unrelated test-fixture dialog, only the affected UI checks
were repeated and passed. No production change was made for that fixture issue.

The signed 0.3.1 -> 0.3.2 upgrade preserved its UI-saved endpoint. The signed
release then passed a fresh original-endpoint -> Uptodown Showly build 1220892131
-> Android download -> metadata/hash -> unchanged readable receiving URI workflow.
Package `com.michaldrabik.showly2`, version 3.72.0/build 843, 12,004,991 bytes,
SHA-256 `f0fac7fc5474168ca982fd220361f3bbb76d4f4f3ed9e89cf03d2465d2b77074`.
An earlier test used the wrong app hostname and was correctly rejected as an
unsuccessful target response; the corrected canonical build URL is retained in
the signed live evidence. No downloaded original was patched or installed.

Focused companion JVM contracts, signed release build and release lint pass
(0 errors, 82 existing warnings). All Gradle invocations used the installed
`Local\Darka.AndroidGradleBuildGate` supervisor, which actually acquired the gate;
the final command waited behind another task's build. Effective profile: two
workers, no parallel projects, 3 GiB Gradle heap, Kotlin 2.3.10 in-process with
AGP 8.13.2 sharing that heap, one 512 MiB test fork. The compiler strategy and
actual test launch were inspected in the build log; no native compilation tasks
had sources. The emulator was paused during the final build. No memory-related
failure or budget increase occurred. One early invocation failed because a
PowerShell path argument was unquoted, then passed with correct quoting. Completed
task-owned idle daemons/cached compiler workers were cleaned only after inspecting
identity, completed lifecycle and the absence of build-client connections.

Signed deliverable: `artifacts/Morphe-Downloader-0.3.2.apk`, version code 6,
2,103,647 bytes. SHA-256
`104024efdd7bd3be818bf273cb0e6266b6b8fcab9da69071747fbade334b4047`.
Unchanged certificate SHA-256:
`4b99c36a398c62dd2eb818ed8ae6a56fff6081fc9c77990fe1998335c5d08c11`.
Evidence is retained in `artifacts/verification/byparr-queue/` (gitignored).
Stage 13's B5/B6 remain unresolved; the new queue adoption is a separate verified
release scope.

Delivery: source commit `56ef75ce74789ca26bd612b70e5895cc7a903b8b` was pushed to
`origin/main`; GitHub release `v0.3.2` is normal, non-draft and latest, targeting
that source. Its independently downloaded APK matches the local tested signed
deliverable byte-for-byte. The GitHub asset is uploaded, 2,103,647 bytes, with the
same SHA-256 recorded above. The release uses meaningful notes from
`docs/release-notes/v0.3.2.md` and retains the independent APKMirror limitation.

Reviewed transactional cleanup removed all 2,368 members of
`D:/Temp/morphe-byparr-queue` (115,992,101 logical bytes), the failed-command report
and its empty task-created `D:/plugins` parent. Cleanup reported no errors or
residuals; both root paths were independently confirmed absent. The isolated
emulator, host fixture and remote loopback fixture stopped, and the task-owned
idle Gradle processes were cleaned after completion evidence. The signed APK and
required evidence remain outside the removed roots. No physical-device state was
changed. Stage 15 is COMPLETE; Stage 13's B5/B6 remain BLOCKED.
