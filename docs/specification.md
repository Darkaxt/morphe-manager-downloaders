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
- R4: Scope change authorized 2026-10-03: manage APKMirror pages through Byparr only;
  remove the companion's internal WebView. Follow unambiguous download steps and
  expose manual variant choices. Reject unresolved challenges and wrong/not-found
  page content. Do not select an arbitrary variant or silently change versions.
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
  No external release is authorized. On 2026-10-03 the user authorized live testing
  on their connected phone (Samsung SM-F966B, serial RFCY80551LT), including installing
  the companion and reaching existing Morphe's import screen. Do not use the Thor,
  replace Morphe, patch/install the downloaded target, or alter its signing identity.
- R8: Verify focused behavioral tests, Android build/lint, link resolution, archive
  download and URI handoff on an isolated emulator. Test the real APKMirror
  boundary; if externally unavailable record the precise unmet criterion rather
  than treating fixtures as live proof. Commit and push the verified source.
- R9: User-authorized Byparr-only support (2026-10-03). Add a persisted,
  configurable private HTTPS API endpoint and retain the original-file Morphe handoff.
  Prompt for the server URL when no default/saved endpoint exists or the download
  fails. Saving/retrying is explicit; do not add automatic retry loops.
  Validate API/content results, restrict target navigation to APKMirror and process
  one browser request at a time. Prove an original-file download across the actual
  configured server boundary; do not claim a cookie/HTML response as file proof.

## Stages and acceptance criteria

1. Consolidated baseline — COMPLETE (R1, repository creation portion of R2)
   - Both source histories integrated, meaningful fork changes reconciled with
     the current source layout, no conflict markers; provenance recorded.
   - Consolidated downloader sources compile against the matching manager API.
2. APKMirror vertical slice — COMPLETE (R2–R6, R9)
   - Link handler launches; a real APKMirror page reaches an original download;
     completed archive validates and is readable by a receiving Android app.
   - Default share targets Morphe Expert mode; manual variant/error states
     stay usable and are accurately reported.
   - Byparr setting persists, fails clearly when its API/content is invalid,
     and participates in a real original-file download without replacing Morphe.
3. Lifecycle, verification and delivery — COMPLETE (R5, R7, R8, R9)
   - Cancellation/failure/recreation behavior passes focused checks; release APK,
     lint, integration verification, documentation and source delivery complete.

## Reconciliation ledger

Stage 1 passed: both histories reconciled, no unresolved merge entries or conflict
markers, and consolidated `compileDebugKotlin` passed using the unchanged API
sources from `api@1.0.0-dev.4` (b7e94efb). GitHub Packages returns HTTP 401 with
available credentials; `scripts/build-manager-api.ps1` provides a pinned-source
local verification build. No required tracked deferrals.

Stage 2 passed on 2026-10-03: the Byparr-only native companion resolved the actual
YouTube 21.39.523 variant, followed its download landing page, resolved APKMirror's
attachment redirect and downloaded the original 72,237,474-byte APKM using Android
DownloadManager. Its base.apk MD5 matches APKMirror's published bundle entry
(9a9a84346eba90a7a856d00525d11d31). Official Morphe 1.33.0 read the content URI,
recognized the split bundle and version, and reached YouTube's Expert-mode patch
selection. No patching or target installation occurred. Fixture verification covers
manual variant selection, session headers, byte-preserving URI sharing and endpoint
persistence/explicit retry. APKMirror LinkSheet handlers are unchanged and verified
on the isolated emulator. All Stage 2 acceptance criteria pass.

B1 (Cloudflare loops in the former embedded WebView) is resolved by the expressly
user-authorized Byparr-only design change. No companion WebView remains. The real
server boundary and actual original-file workflow now pass, rather than substituting
fixture results or a catalogue response. The API uses POST /v1 with request.get;
the user's forthcoming upstream POST support is not required by this tested flow.

Stage 3 passed: current-source JVM and Android lifecycle/URI checks, separate-process
restoration, debug/release lint, locally signed release build and signature verification
all pass. Documentation records setup, limits, source provenance and signing identity.
The complete Byparr integration was committed and pushed as fbc3d761; GitHub main
was independently checked against that commit. Temporary verification outputs were
removed after preserving the signed deliverable and sanitized integration evidence.
No blockers or required tracked deferrals remain.

Final reconciliation: R1 is verified by preserved ancestry and the unchanged root
API compilation; R2/R3 by the standalone identity, existing LinkSheet chooser proof
and unchanged VIEW/BROWSABLE handlers; R4/R9 by actual Byparr page-to-file proof and
native setting/selection/failure tests; R5 by Android transfer, original-file hashes,
archive validation and lifecycle checks; R6 by real Morphe Expert-mode import and
readable URI sharing; R7 by the signed APK and documentation; R8 by focused checks,
real external-boundary verification and source delivery. All required criteria are
satisfied and all stages are COMPLETE. No external release was published.

Live proof applies to the isolated Android 15 emulator and configured private HTTPS
server. The previous phone WebView test looped; the new Byparr-only build has not
been tested on that phone. This distinction does not weaken the required emulator
acceptance criteria or claim physical-phone verification.

## Consolidation decisions

The GitHub fork was created from brosssh immediately before the request to first
consolidate the original repository. It had no custom implementation at that point;
the canonical history was merged into that same fork network before the companion
was implemented, rather than recreating the GitHub fork.
Canonical main and dev have identical trees. New source layout, API, toolchain and
disabled broken Play Store registration take precedence over the old split apps.
