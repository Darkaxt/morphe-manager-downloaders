# Verification record: 2026-10-03

## Confirmed

- Canonical `41b9ab3` and brosssh `e9441f0` are ancestors of the consolidated
  history. Merge `03baaf8` preserves both parents. The legacy root downloader
  Kotlin sources compile against API `api@1.0.0-dev.4` sources at `b7e94efb`.
- Companion debug and release configurations build. Debug and release lint pass
  without errors. Remaining warnings include optional dependency upgrades,
  localization, API-compatibility attributes and deliberate durable preference
  commits. No lint baseline hides errors.
- Focused JVM contracts verify release URL restrictions, spoofed-host rejection,
  debug-only loopback access and APK/APKM ZIP structure rejection/recognition.
- Android 15 isolated emulator `emulator-5580` lists the companion alongside Chrome
  for an APKMirror VIEW/BROWSABLE intent. No physical device was installed to.
- Official LinkSheet **0.0.33** displayed the companion alongside Chrome for the
  APKMirror URL. Selecting the companion launched that real URL in its WebView.
  APKMirror then displayed Cloudflare's human-verification checkbox; the companion
  accurately displayed its manual-verification state. No generic browser handler
  or domain ownership association was needed for this LinkSheet chooser.
- The HTTP fixture verifies release -> variant -> redirected attachment navigation,
  required cookie/User-Agent/Referer forwarding and Android DownloadManager transfer.
  The downloaded SHA-256 equals the fixture's source APK SHA-256. A separate installed
  test APK reads the content URI with matching bytes, display filename and MIME.
- Completed and pending transfers survive activity recreation. Clearing a completed
  transfer retains its file. Restoration does not automatically start a duplicate
  download. Cancellation removes the active system download. HTML advertised as an
  APK is rejected, and the error remains visible after recreation.
- Separate instrumentation invocations, with a force-stop between them, recover a
  pending transfer and validate the completed original after reopening the app.
  The gated fixture implements HTTP ETag/range semantics; the application adds no
  retry, sleep or timeout recovery mechanism.
- Synthetic multiple-variant and security-check pages remain manual. These checks
  exercise the shipped JavaScript in Android WebView, not a substitute parser.
- Official Morphe **1.33.0**, downloaded from its GitHub release, received the
  companion's file on the emulator with Expert mode enabled. Its patch selection
  screen displayed `app.morphe.manager.downloaders` and Expert mode, demonstrating
  actual URI access and APK parsing. No patching or installation was performed.
- The ready-screen layout was visually inspected. The action buttons and filename
  are readable and fit the emulator's screen.
- Local signed artifact: `artifacts/Morphe-Manager-Downloaders-0.1.0.apk`.
  APK signature verification passes (v2; minimum Android 8).
  SHA-256: `0770442ee65479fa67fde34398d4782b3a89318facfddf1fde3986705155df32`.
  Signing certificate SHA-256:
  `4b99c36a398c62dd2eb818ed8ae6a56fff6081fc9c77990fe1998335c5d08c11`.

Raw emulator results, screenshots, lint/unit XML, APK metadata and signature output
are retained locally in `artifacts/verification/`. Generated build/API/emulator
outputs are expendable after these evidence files and the signed APK are preserved.
The companion key/password pair remains outside the repository under the build
script's persistent signing directory. No GitHub release has been published.

## Remaining integration blocker

**B1 — real APKMirror page-to-original-file workflow.** APKMirror serves Cloudflare
human verification in Chrome and the companion's Android WebView; direct HTTP access returns 403.
User interaction was requested. No CAPTCHA was automatically clicked or bypassed.
The real site's current release/variant/download selectors and original file
retrieval have not passed live verification. Synthetic pages do not establish this.

The Android transfer and Morphe import path work with a real built APK from the
controlled server. Live APKMirror download compatibility, LinkSheet selection on the
user's physical device and a real APKM import have not been claimed. Only APKMirror domains
are declared by the companion. [LinkSheet's documentation](https://github.com/LinkSheet/LinkSheet)
describes its native app and browser chooser; the companion is a domain-specific
app handler in that chooser.

## Reproduction

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
python scripts/fixture-server.py --apk <absolute-debug-apk-path>
./scripts/verify-emulator.ps1 -Serial emulator-5580
./scripts/build-companion.ps1
```

After process-restore, install official Morphe on the isolated emulator and enable
Expert mode there. Run the optional `MorpheHandoffSmokeTest` instrumentation class,
then independently inspect Morphe's selected-APK screen; the test's launch result
alone does not prove import. For final live verification, launch a real APKMirror
link through the companion, complete any human check and confirm a downloaded
original reaches Morphe. This remains required for overall completion.
