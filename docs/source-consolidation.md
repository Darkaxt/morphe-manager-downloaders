# Source consolidation (2026-10-03)

- Canonical ReVanced main: `41b9ab3` (v1.2.0, 2026-03-14). `dev` has the same tree.
- brosssh main: `e9441f0` (v1.1.0, 2025-12-04).
- Fork: https://github.com/Darkaxt/morphe-manager-downloaders

Merge both histories. Use the canonical single-APK source layout, fragments API
(`revanced-manager-api` 1.0.0-dev.4), SDK 36, AGP 8.13.2, Kotlin 2.3.10, Gradle
8.13, Maven ARSCLib dependency and corrected release-signing configuration.

Carry forward brosssh's actual functional differences:

1. Port APKCombo and APKPure URL providers to the current `WebViewDownloader` API
   and register both in the existing ReVanced downloader class resource.
2. Return APKMirror's original file without merging its splits. The destination
   manager owns merging and validation, avoiding destruction of original inputs.

Remove obsolete split-app build directories and the obsolete ARSCLib submodule
declaration. Fork-only version metadata does not override canonical v1.2.0.
Keep the canonical disabled Play Store registration: upstream documents its login
as broken, and the requested Morphe companion targets APKMirror only.

The separately packaged Morphe companion will live in `app/`; the consolidated
ReVanced downloader package remains a distinct build target. Preserving those
providers does not advertise companion support for other websites.

Verification: `compileDebugKotlin` passed on 2026-10-03 against API sources from
the immutable `api@1.0.0-dev.4` commit `b7e94efb7a2c35496e11174c6d1892685d95b443`.
The published Maven dependency returned HTTP 401 with available credentials, so
the pinned-source adapter builds its unmodified Kotlin/AIDL/resources locally
using SDK 36, core-ktx 1.17.0, lifecycle 2.10.0 and fragment 1.8.9. This checks
source compatibility; it is not verification of ReVanced runtime behavior.
Automatic release-on-push is removed; release invocation is manual only.
