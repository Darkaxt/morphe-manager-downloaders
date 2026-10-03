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
2. APKMirror vertical slice — ACTIVE (R2–R6)
   - Link handler launches; a real APKMirror page reaches an original download;
     completed archive validates and is readable by a receiving Android app.
   - Default share targets Morphe Expert mode; manual variant/challenge states
     stay usable and are accurately reported.
3. Lifecycle, verification and delivery — NOT STARTED (R5, R7, R8)
   - Cancellation/failure/recreation behavior passes focused checks; release APK,
     lint, integration verification, documentation and source delivery complete.

## Reconciliation ledger

Stage 1 passed: both histories reconciled, no unresolved merge entries or conflict
markers, and consolidated `compileDebugKotlin` passed using the unchanged API
sources from `api@1.0.0-dev.4` (b7e94efb). GitHub Packages returns HTTP 401 with
available credentials; `scripts/build-manager-api.ps1` provides a pinned-source
local verification build. No required tracked deferrals.

Stage 2 live verification currently encounters Cloudflare's human-verification
checkbox in Chrome. User interaction has been requested; implementation and
isolated Android verification remain actionable.

## Consolidation decisions

The GitHub fork was created from brosssh immediately before the request to first
consolidate the original repository. It has no custom implementation yet; merge
the canonical history into that same fork network rather than recreating it.
Canonical main and dev have identical trees. New source layout, API, toolchain and
disabled broken Play Store registration take precedence over the old split apps.
