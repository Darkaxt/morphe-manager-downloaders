# Morphe Manager Downloaders: APKMirror companion

Authoritative specification. Implementation is authorized by the current task.
The user's reference to export mode means the previously discussed Expert-mode
APK file handoff; this is the default workflow.

## Requirements

- R1: Consolidate ReVanced/revanced-manager-downloaders main (41b9ab3) and
  brosssh/revanced-manager-downloaders main (e9441f0), retaining their histories.
  Prefer the canonical consolidated API/toolchain; preserve the fork's APKPure,
  APKCombo and original-file APKMirror behavior. Record resolution decisions.
- R2: Create Darkaxt/morphe-manager-downloaders, named Morphe Manager Downloaders.
  Build a separately installable companion with its own package identity.
- R3: Accept APKMirror HTTPS page links through Android VIEW/BROWSABLE handlers
  so LinkSheet can offer it. APKMirror is the companion's only supported site.
- R4: Manage the APKMirror page-to-file workflow in its own WebView, automatically
  follow unambiguous download steps and expose variant/security-check interaction
  when required. Do not select an arbitrary variant or silently change versions.
- R5: Download the original APK/APKM using the browser session's required headers.
  Show download state, support explicit cancellation, retain downloads across
  activity/process recreation and validate archive content before offering it.
  Preserve original bytes; do not merge, patch, sign or install downloaded apps.
- R6: Default completion action shares the validated file directly to installed
  Morphe using ACTION_SEND, a content URI, correct filename/MIME and temporary
  read permission. If Morphe is absent, explain it and allow the system share
  chooser. Do not alter Morphe or its signing keystore.
- R7: Produce a locally signed APK and document installation, LinkSheet selection,
  Expert mode, interaction limits, provenance, verification and signing identity.
  No external release or physical-device installation is authorized.
- R8: Verify focused behavioral tests, Android build/lint, link resolution, archive
  download and URI handoff on an isolated emulator. Test the real APKMirror
  boundary; if externally unavailable record the precise unmet criterion rather
  than treating fixtures as live proof. Commit and push the verified source.

## Stages and acceptance criteria

1. Consolidated baseline — COMPLETE (R1, repository creation portion of R2)
   - Both source histories integrated, meaningful fork changes reconciled with
     the current source layout, no conflict markers; provenance recorded.
   - Consolidated downloader sources compile against the matching manager API.
2. APKMirror vertical slice — BLOCKED (R2–R6)
   - Link handler launches; a real APKMirror page reaches an original download;
     completed archive validates and is readable by a receiving Android app.
   - Default share targets Morphe Expert mode; manual variant/challenge states
     stay usable and are accurately reported.
3. Lifecycle, verification and delivery — BLOCKED (R5, R7, R8)
   - Cancellation/failure/recreation behavior passes focused checks; release APK,
     lint, integration verification, documentation and source delivery complete.

## Reconciliation ledger

Stage 1 passed: both histories reconciled, no unresolved merge entries or conflict
markers, and consolidated `compileDebugKotlin` passed using the unchanged API
sources from `api@1.0.0-dev.4` (b7e94efb). GitHub Packages returns HTTP 401 with
available credentials; `scripts/build-manager-api.ps1` provides a pinned-source
local verification build. No required tracked deferrals.

Stage 2: the emulator fixture verified page traversal, session cookie/User-Agent/
Referer forwarding, original APK download, archive validation and a separate
installed Android app reading the URI with matching SHA-256, filename and MIME.
The default intent targets Morphe. Official Morphe 1.33.0 imported the original
fixture APK on the emulator and displayed its package in Expert-mode selection.
Official LinkSheet 0.0.33 offered the companion and opened a real APKMirror URL in
its WebView. The site's human-verification checkbox was displayed correctly.
External blocker B1: the real APKMirror-page-to-download acceptance criterion
cannot pass while APKMirror serves Cloudflare human verification in the available
browser. User interaction has been requested. Resolution requires access to a real
release page and a successful original-file download. Live selector compatibility
and overall stage closure remain unverified; fixtures do not resolve B1. Stage 2
is parked. Confirmation to click the displayed checkbox has also been requested.

Stage 3 local acceptance criteria passed: cancellation, rejection of invalid files,
activity recreation without duplicate transfers, recovery in a fresh process,
archive/URL contracts, debug/release lint, locally signed APK, readable action
layout, LinkSheet selection and actual Morphe Expert-mode import. Signing identity,
usage and evidence are documented in `docs/verification.md`. Source delivery is
being committed and pushed. B1 also prevents Stage 3's required final real-site
integration verification, so this stage remains BLOCKED even after local source
delivery. The unresolved condition is external and resolves only when the real
page-to-file workflow passes. No required work is classified as a tracked deferral.

Overall outcome is not COMPLETE. R1, R2, R3, R5, R6 and R7 have local evidence;
R4's real download-step selection and R8's final real-site check remain blocked by
B1. No external release or physical-device installation is authorized or performed.

## Consolidation decisions

The GitHub fork was created from brosssh immediately before the request to first
consolidate the original repository. It had no custom implementation at that point;
the canonical history was merged into that same fork network before the companion
was implemented, rather than recreating the GitHub fork.
Canonical main and dev have identical trees. New source layout, API, toolchain and
disabled broken Play Store registration take precedence over the old split apps.
