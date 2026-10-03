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
  patch-signing identity are unchanged. No GitHub release is published.

## Testing limits

The complete new Byparr path is verified on the emulator, not the physical phone.
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
`apkMirrorUrl`; supply `expectedBaseMd5` when the page lists it. Keep the endpoint
private. Run `MorpheHandoffSmokeTest` before fixture tests replace the active file,
then independently inspect Morphe's import screen. A successful Activity launch
alone does not prove parsing/import. Stop before patching/installing target apps.
