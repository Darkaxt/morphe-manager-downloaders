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
  On 2026-10-03 the user authorized live testing
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

## Authorized release and phone validation extension: 2026-10-03

The user explicitly authorized publishing v0.1.0 and testing through ObtainX after
reviewing the verified APK/release-note proposal. This supersedes the earlier
no-release boundary for this companion release only.

- R10: Publish the verified signed 0.1.0 APK as a normal GitHub release, verify the
  asset bytes/signature and register this repository in existing phone ObtainX.
  Install through ObtainX on the authorized Samsung SM-F966B user 0. Preserve
  existing ObtainX entries, Morphe installation/signing keys and unrelated apps.
- R11: Simulate Showly's download/import workflow starting in installed Morphe:
  select its required Showly version, open its link through LinkSheet, resolve with
  the companion's private Byparr setting, download/validate the original and verify
  that existing Morphe reads and selects Showly. Stop before patching/installing the
  target. Preserve existing Morphe queue and network settings.
- R12: Record real phone results, failures and exact limits; commit/push supporting
  documentation. Clean only expendable test files, preserving APK and evidence.
- R13: User-authorized rename during phone verification: use **Morphe Downloader**
  for the app label and APK filename. Keep the existing package ID, repository and
  signing identity so the installed companion receives an ordinary update.

4. Release and ObtainX installation - COMPLETE (R10)
   - Release has the verified APK; remote asset equals the local deliverable.
   - Existing ObtainX tracks this repository and installs the expected companion
     package/version on the authorized phone.
5. Showly phone workflow and reconciliation - BLOCKED (R11, R12, R13)
   - Morphe's real Showly link traverses LinkSheet, Byparr and original download.
   - Existing Morphe reads the original Showly file and displays the selected
     app/version. No target patch/install or changes to its signing identity occur.
   - Evidence, limits, cleanup and source documentation are reconciled.
   - The installed update and APK use Morphe Downloader while preserving identity.

Stage 4 passed: GitHub v0.1.0 is a normal published release with the verified APK.
GitHub asset digest and installed phone base.apk SHA-256 both equal
67b04bddb602dbecea298f494994a0dd294f48e7aa1b49e94fb6284b29dfa834.
ObtainX tracks the correct repository/package and shows installed 0.1.0 as up to date.
Its existing InstallerX Resign route installed the original APK without resigning.
InstallerX required notification and all-files permissions during its first-use flow;
these were enabled for that installer only. No other app was updated or replaced.
No stage is ACTIVE. No required tracked deferrals exist. Showly's phone workflow
is not yet verified. The actual phone test exposed an internal defect: Samsung's
DownloadManager successfully wrote `downloadfile.apk`, while validation used the
requested filename. Stage 5 owns correcting validation to use the provider's
completed local URI, regression verification, and a corrective release needed to
retest through ObtainX. The downloaded original is 12,004,991 bytes, SHA-256
3ec55fffbb2614c810e49f9e3441d4f29c0e263e4f8dd6d5fdbec1e4d0ea1708.
This correction preserves R5/R11; it does not change their acceptance criteria.

The corrected 0.1.1 APK passed the stale-path regression, lifecycle checks,
separate-process transfer restoration, JVM checks, release build/lint and unchanged
certificate verification. Published v0.1.1 targets 2b9d28e61; its asset digest equals
the local APK (e5a214ca7949d2fac3f842ec05df7e4db7cb6fc2fb12c79b4b5ea05de9ee7170).
B2 (external): ADB lost the authorized physical phone before the ObtainX update.
R11's real-phone Morphe import and R13's installed new label remain unmet. Reconnecting
and unlocking that phone resolves the environment blocker. Stage 5 is parked;
its workflow and final reconciliation cannot be considered complete until retested.

### User-requested pause checkpoint

Execution paused at the user's explicit request after the phone disconnected.
No further testing, installation, publication, commits or cleanup should run until
the user resumes. Stage 5 remains BLOCKED; Stages 1–4 remain COMPLETE.

- v0.1.1 is published with the renamed `Morphe-Downloader-0.1.1.apk`; source
  commit 2b9d28e61 is pushed. Its signature and remote digest are verified above.
- The phone still had 0.1.0 at disconnection. Its Byparr endpoint is saved and
  DownloadManager download 1353 retained the original Showly 3.72.0 as
  `downloadfile.apk` in the companion's external Download folder. A local copy
  is preserved in `artifacts/verification/showly/Showly-3.72.0-original.apk`.
- Resume by reconnecting the authorized phone, updating the existing ObtainX
  entry to 0.1.1 without resigning, then verifying original-file Morphe import.
  Stop before target patching/installing; preserve existing Morphe state.
- The isolated emulator was told to stop and the fixture server was stopped.
  Temporary output remains at `D:\Temp\morphe-manager-downloaders-showly-fix`.
  Cleanup transaction `3503b83c6f50e923edb21e5b6acac11d` is registered but has not
  been reviewed/ticketed/applied. Resume cleanup only after preserving required
  evidence and checking generated processes have stopped.
- Documentation reconciliation, phone UI scratch-file cleanup and the final
  documentation commit/push remain outstanding. Checkpoint edits are uncommitted.

## Authorized refinement and additional sources

The user resumed work, reported successful manual download/patch/install after
changing Morphe's installer, and requested the following changes. The screenshot's
missing explicit component was Morphe's configured InstallerX package, not an
archive-integrity failure. Manual phone success is user-reported; no new independent
phone inspection is claimed. The earlier phone remains unavailable over ADB.

- R14: Use Morphe's current launcher artwork with the existing download-arrow badge
  scaled down at bottom right. Preserve package/signing identity and asset attribution.
- R15: Change **Morphe Downloader**, not InstallerX, to a compact rounded native
  dialog resembling InstallerX Resign's installer presentation. Show the APK name
  and a styled progress bar beneath it. Respect light/dark theme, readable spacing,
  touch targets and scrolling on narrow screens. Represent Byparr resolution as
  indeterminate, then actual Android download progress; never invent percentages.
  Retain settings, explicit cancellation, manual variants, errors, restoration and
  original-file sharing. Dismissing must not silently cancel an Android transfer.
- R16: Add APKPure, APKCombo and Uptodown support (all three explicitly confirmed)
  using the saved Byparr endpoint and native original-file download/handoff.
  Register only supported source domains, restrict
  navigation/attachment hosts by source, preserve requested version/variant, and
  validate original APK/bundle bytes. No arbitrary host acceptance or automatic
  retries. Keep the disabled legacy Google Play provider unchanged.
- R17: Verify each added source against a real representative link through the
  configured server, Android download, archive validation and readable receiver URI.
  Record an external blocker if a required boundary cannot be proved; fixtures alone
  do not establish live support. Final focused regression/lifecycle/manifest checks,
  signed artifact, documentation, cleanup and commit/push complete the refinement.
  No new release is authorized merely by these feature requests.

6. Compact presentation and icon - COMPLETE (R14, R15)
   - Exact Morphe vector artwork plus distinguishable small download badge renders.
   - Dialog is visually inspected in light/dark, narrow and wide configurations.
   - Settings/manual selection/error/cancel/restoration/URI behavior still passes.
7. APKPure vertical slice - COMPLETE (APKPure portion of R16, R17)
   - Real APKPure page reaches an original validated file with readable URI.
   - Requested versions and ambiguous variants remain explicit; host policy passes.
8. APKCombo vertical slice - COMPLETE (APKCombo portion of R16, R17)
   - Real APKCombo page reaches an original validated file with readable URI.
   - Requested versions and ambiguous variants remain explicit; host policy passes.
9. Uptodown vertical slice - COMPLETE (Uptodown portion of R16, R17, R20)
   - Real per-build Uptodown page reaches an original validated file with readable URI.
   - Requested versions and ambiguous variants remain explicit; host policy passes.
10. Refinement release reconciliation - COMPLETE (R17, R18 release scope below)
   - Integrated focused checks pass, signed artifact and docs match actual support,
    generated expendable files are cleaned and verified changes committed/pushed.

### Authorized release scope: 0.2.0

R18 (2026-10-03): The user explicitly requested a release with the previous
features while leaving Uptodown pending until the Byparr update is ready. Publish
a normal 0.2.0 companion release containing the Morphe icon/download badge, compact
dialog, actual name/status/progress, APKMirror, APKPure and APKCombo support, and
XAPK structural validation/handoff. Preserve package and signing identity, verify
the signed artifact and published asset, and document Uptodown as pending. Do not
register Uptodown handlers or present it as supported. This explicitly authorizes
partial feature delivery; Stage 9 remains BLOCKED on B4 and is excluded from this
release's completion criteria. The earlier independent phone criteria in Stage 5
remain BLOCKED and are also excluded from this release scope. Neither exclusion
declares those stages complete. Stage 10 owns this partial release; Byparr remains
unchanged. The original endpoint is retained. No phone/Thor installation is required
or claimed for this release; verification uses the isolated task emulator.

Stage 6 passed: the bounded rounded dialog and exact Morphe vector plus small
download badge were visually inspected in light/dark at 1080x2400 and 1968x2184.
Presentation assertions pass; existing error/challenge/manual-choice, cancellation,
recreation and original-byte receiver contracts pass. An endpoint-save test race
now waits for the actual preference-change event before asserting its result.

Stage 8 passed after the user's requested Helium capture exposed the incorrect
Referer header. Fresh real Android download/validation, package/version inspection
and receiver byte equality pass for Showly 3.72.0. The earlier B3 hypothesis is
resolved. Stage 9 owns Uptodown and is parked on B4 below. Stage 10 delivers the
later authorized R18 release scope; no implementation stage remains ACTIVE.
Stage 5 remains parked on its earlier independent phone
verification boundary. No required work is assigned an unnamed future deferral.

Stage 7 passed: real APKPure Showly 3.72.0 (build 843) was resolved through Byparr,
downloaded on Android, validated, and read unchanged through a receiving app's URI.
Package/version and APK signature were independently inspected. APKPure's container
MD5 differs from APKMirror's, but all ZIP entry payloads and signing certificates
match; the APKPure-specific hash is used in the live check. Tests reject changed
versions, advertising APKs and attachment hosts belonging to another source.
APK/XAPK archive formats are supported; XAPK uses Morphe's declared x-xapk MIME.
The user's subsequent title/status clarification is recorded under R15: after
metadata arrives, show `Downloading {app name}` and actual resolution, transfer,
validation and ready states. No fabricated percentage during page resolution.

B3 (original hypothesis, superseded by Helium capture): APKCombo's real Showly 3.72.0 page resolves and its
browser POST steps return a versioned variant, but `/d` redirects through
`download.pureapk.com` to APKPure's `/url?e=2` HTML error page, not an attachment.
Fresh native POSTs to the actual `/dl` and `/checkin` endpoints reproduce the
failure. Resolving the generated URL through Byparr also fails at its protocol
boundary. Android reproduces the rejected attachment redirect; no APK was accepted.
R17's real original download/validation/readable URI cannot pass until that upstream
attachment service produces a downloadable original through the supported transport.
The user requested Helium traffic capture. The capture proves `/d` ->
`download.pureapk.com` -> `data.winudf.com` succeeds when the anchor's `noreferrer`
policy is preserved. Sending Referer reproduces the HTML error with the identical
URL; omitting Referer returns the APK with both Helium and Byparr user agents.
This is an internal request-policy defect, not evidence of an unavailable upstream.
The native redirect resolver and Android DownloadManager now preserve the chosen
anchor's referrer policy. Fresh Android download and receiver verification then
passed, closing Stage 8. Helium's local CDN blocking was not disabled.

B4 (external integration, 2026-10-03): R16/R17's Uptodown resolution through the
configured Byparr service cannot currently pass. The real per-build page has no
static attachment URL. Clicking its download button obtains an embedded Turnstile
token, then submits a JSON POST to its download-url endpoint. A browser capture
observed a successful response containing the signed attachment key; following
that key with native HTTP produced the original APK. Byparr GET returned the page
without a populated token. Native POST with an empty token returned HTTP 400,
errorCode -51. Attempts through the deployed custom-post API were rejected with
HTTP 429 before submission while its browser slot was occupied; these are not
evidence of a failed upstream POST implementation. POST transport and same-context
preflight alone do not perform the page's button-driven token flow.

The user explicitly instructed this task not to extend Byparr, and requested a
handoff for that repository's task owner. No backend files or deployment were
changed. The original saved service endpoint remains selected. Resolving B4
requires the owner to provide and live-verify a supported same-browser operation
that returns the requested build's signed attachment URL. Uptodown's Android
download/validation/receiver verification and final all-source reconciliation
remain unsatisfied until that boundary is available. Stage 9 is BLOCKED; Stage 10
owns the explicitly authorized partial release under R18. See
`docs/uptodown-byparr-handoff.md` for reproducible evidence and
the requested capability; it proposes behavior, not an existing API command.

Stage 10 release preparation passed: current companion JVM contracts, Android
presentation/lifecycle/manual-attachment/URI checks, separate-process restoration,
release build/lint and original signing-certificate verification apply to the
0.2.0 implementation. A reproduced completed-window recreation defect is corrected
and both automatic/manual downloads remain ready with the same transfer ID.
The final signed APK upgrades from 0.1.1 while preserving a saved endpoint. Installed
handlers include APKMirror/APKPure/APKCombo and exclude Uptodown/unrelated hosts.
The task emulator, fixture server and capture tab are stopped/closed. Both exact
task-generated Temp roots are removed after retaining required APKs/evidence;
cleanup completed through reviewed tickets, including a fresh review after the
idle task Gradle daemon released lint metadata handles. Byparr and user devices
were left unchanged. Source commit `7b875c106b40679288282002021f0ce75786e8e7`
was pushed and independently matched on GitHub. Normal release v0.2.0 targets that
commit and is the latest release. Its only asset, `Morphe-Downloader-0.2.0.apk`,
was downloaded and compared byte-for-byte with the verified 2,092,502-byte local
APK; GitHub's SHA-256 digest also matches
`60ec0eeafcdd3de91aa1368187fc5f2c6f5431b833f8e8576eda8cce918e7750`.
All R18 release criteria pass and Stage 10 is COMPLETE. R18 excludes unresolved
Stages 5 and 9 from this release; no claim of all-source completion is made.

## R19: Ready archive identity (2026-10-03)

Replace the ready filename title with archive metadata, following the inspected
InstallerX dialog: app label as the main title, then `Package: {package ID}` and
`Version: {version name} ({version code})` as smaller centered details. Read the
downloaded original APK, including the base APK inside supported APKM/XAPK files,
off the UI thread. Persist the details with the completed download, and populate
them for previously saved ready downloads. If optional metadata cannot be read,
retain a readable filename fallback without rejecting a validated archive.
Preserve original bytes, handoff filenames/MIME/URI access, loading progress and
provider behavior. No Byparr changes or new external release are authorized here.
The user's additional button request belongs to this same presentation scope:
label the Byparr setup actions `Cancel` and `Save`, preserving their behavior.

Stage 11 — ready metadata presentation: COMPLETE. Covers R19. Acceptance criteria:
real APK label/package/version/build appear with InstallerX's hierarchy; supported
bundle base metadata is read and temporary extraction removed; ready state survives
recreation and legacy metadata backfill; unreadable metadata has a usable fallback;
original download bytes and receiver filename/URI contract remain unchanged.
Byparr setup displays `Cancel`/`Save` and retains endpoint saving/retry behavior.
Verify Android archive parsing and ready UI, the existing download/receiver slice,
focused lifecycle/presentation contracts and a build, then commit the verified
scope. Stages 5 and 9 remain BLOCKED on their existing independent boundaries.

Stage 11 reconciliation: a real Showly original displays its resource label,
package ID and independently confirmed version/build, visually inspected in
light/dark. Controlled APKM/XAPK containers containing a real base APK preserve
their hashes and leave no extracted APK in cache. Older ready records populate
metadata and restore it on recreation; unreadable metadata retains a ready file
and filename fallback. Automatic/manual download and separate-process transfer
restoration, original-byte readable URI and filename/MIME contracts pass. Focused
setup checks pass with `Cancel`/`Save`, including endpoint persistence and retry.
Current debug/test builds, JVM checks and final lint pass (zero lint errors).
All R19 presentation criteria pass; no new blocker or deferral remains in this
scope. This source refinement is unreleased and does not close Stages 5 or 9.

## R20: Uptodown scripting integration and release (2026-10-03)

The user supplied the deployed Byparr scripting handoff and explicitly requested
implementation and a new release. Integrate the caller-owned recipe from Byparr
commit `5671f2474493d6fb2ce5ab5d2cdd4e7fbc47db2f`; do not modify or redeploy Byparr.
Use the user's existing original endpoint. Preserve explicit Uptodown file IDs,
derive build links from page metadata for app/download landing pages, and expose
ambiguous release choices rather than substituting a build. Before native transfer,
validate API HTTP 200/status ok, the matching successful website POST response,
the aborted GET capture, and its exact signed URL correspondence. Use only the
captured accept/referer, matching user agent, and destination-scoped cookies.
Permit only Uptodown's verified attachment hosts/paths through redirects. Inspect
the original archive independently; do not infer APK/XAPK from the website option.
No token extraction/replay, automatic retry, arbitrary delay, or fabricated status.

Stage 9 is COMPLETE (R16/R17 Uptodown and R20). The deployed readiness
reports custom-post-scripting; B4's absent backend operation is resolved. Its
criteria are companion integration, rejection contracts, incoming link handling,
and a fresh real Showly build -> Android original download -> archive/package/
version/build/hash verification -> unchanged readable receiving URI. Controlled
tests do not replace that live boundary. Preserve the other three sources.

Stage 9 closure: the exact caller recipe is packaged byte-identically to the
pinned Byparr source. Controlled capture checks reject failed/different website
responses, missing scripting, non-aborted/non-GET requests, different signed
URLs, mismatched user agents and invalid/cross-origin forwarded headers. Source
policy/parser contracts preserve requested IDs and restrict attachment origins.
The live original endpoint -> Showly file 1220892131 -> Android download -> archive
validation -> metadata and unchanged receiver URI passed. The APK is 12,004,991
bytes, package com.michaldrabik.showly2, version 3.72.0/build 843, SHA-256
f0fac7fc5474168ca982fd220361f3bbb76d4f4f3ed9e89cf03d2465d2b77074.
The new installed page handler resolves Uptodown app/build URLs and excludes its
attachment URL. B4 is resolved. No Stage 9 blocker or tracked deferral remains.

Stage 12 — release 0.3.0: COMPLETE (R19/R20). Depends on Stage 9 COMPLETE.
Include Uptodown plus the verified metadata dialog and Cancel/Save labels. Verify
focused integrated contracts, signed release build/lint, original certificate,
0.2.0 in-place upgrade with endpoint persistence, accurate documentation/release
notes, cleanup, commit/push, normal GitHub release and independently downloaded
asset byte equality. The user's new release request authorizes publication.
Stage 5 remains BLOCKED on its independent physical-phone workflow; the release
uses isolated emulator verification, consistent with the established release
boundary, and does not claim physical-phone or patched-app installation proof.

Stage 12 closure (2026-10-04): focused companion JVM and Android capture,
metadata, lifecycle, presentation, URI and process-restoration checks pass.
The final signed release passed a fresh real Uptodown Showly workflow and a
0.2.0 in-place upgrade preserving the UI-saved endpoint. Release build/lint and
unchanged certificate verification pass. Documentation and release notes match
the verified behavior; exact temporary roots were removed through reviewed
cleanup tickets after stopping task processes and preserving APK/evidence.
Source commit `d10ba91513c31a851a0d5fa7caa62aa8e3d57099` was pushed and matched
independently on origin. Normal/latest v0.3.0 targets that source; its only APK
asset was independently downloaded and matched the verified local artifact
byte-for-byte and GitHub's SHA-256 digest. All R20 release criteria and R19's
inclusion pass. No required blocker or tracked deferral remains in this scope.

## R21: Connected-phone APKMirror failure (2026-10-04)

The user connected the phone for diagnosis and correction of the current failure.
Trace the exact YouTube 21.16.256 APKMirror request on Samsung SM-F966B before
changing production behavior. Preserve the saved Byparr endpoint, existing Morphe,
patching key, patched applications and unrelated phone/network settings. Test only
the companion workflow; do not patch or install the downloaded YouTube original.
Correct the demonstrated cause, preserving exact variant selection, original
bytes, supported-host/header/cookie policy and explicit cancellation. Reuse the
deployed generic Byparr interface if required; do not alter its deployment.

Stage 13 — APKMirror phone correction: BLOCKED (R21). Acceptance: reproduce and
attribute the failed hop; regression checks exercise the demonstrated cause;
the same phone/page reaches validated original readiness with correct metadata
and readable URI bytes; focused existing-source/lifecycle contracts and signed
build checks pass; settings/signing identity remain intact; document, clean
temporary outputs and commit the verified correction. The subsequent R24 delivery
instruction authorizes release of the verified R22 scope while R21 stays blocked.

R22 (same stage, authorized during diagnosis): request failures stay in the
standard compact download dialog, showing the actual error plus `Retry` and
`Open in Browser`. Retry explicitly resolves a fresh session for the selected
page/build. Browser fallback opens that source page externally, excluding this
companion from its chooser. The cog remains the entry to Byparr settings; retain
the initial configuration prompt only when no endpoint exists. Apply the actions
to page, attachment and system-download errors, persist error presentation across
recreation, and never retry automatically. Verify the actions, no unsolicited
settings prompt, selected URL, cancellation and endpoint preservation.

Stage 13 reconciliation (2026-10-04): R22 is implemented and verified. Failures
remain inline across recreation, Retry uses the selected source page with a fresh
session, expired APKMirror landing keys are discarded, and the external chooser
excludes the companion. The cog and first-use setup still work. A failed new
request cannot offer the previous completed APK. Focused source, lifecycle,
metadata and failure-action checks pass. Signed local 0.3.1 (code 5), release
build/lint, original certificate and 0.3.0 -> 0.3.1 saved-endpoint upgrade pass.
The local artifact contains the verified R22 changes; it does not claim an R21
automatic-resolution correction or a new external release.

B5 (external): R21's real browser resolution/download criterion cannot currently
pass. Native `download.php` returned HTTP 403 with Cloudflare challenge headers;
Helium verified its normal 302 storage redirect. Deployed Byparr browser-capture
attempts timed out during its Cloudflare check; a no-op script and ordinary page
GETs succeeded. Redirect response capture also returned HTTP 502 when reading a
redirect body. The exact observations and required proof are recorded in
`apkmirror-byparr-handoff.md`. Resolution requires a successful live capture of
the exact APK's signed attachment using the original endpoint, followed by the
companion correction and regression/integration checks. The user has reserved
Byparr extension/deployment work for its owner. R21 remains unsatisfied.

B6 (external): R21's same-phone original-readiness/URI acceptance criterion
cannot be verified while Samsung SM-F966B is disconnected. Resolution requires
that phone to be reconnected after B5 is resolved and a verified correction is
available. Its installed companion, Morphe, keys, apps and network settings were
preserved. The temporary diagnostic test package/file may still be on the phone
and require cleanup on reconnection. No required tracked deferral was created;
Stage 13 and the automatic APKMirror correction remain incomplete.

R23 (all remaining work, 2026-10-04): subsequent Gradle commands must use the
host-wide Windows mutex `Local\Darka.AndroidGradleBuildGate`, held by their
supervising process from before checks for other active/ambiguous builds until
the command exits. Wait for completion without interrupting other builds or
stealing the lock. Start with two workers, no parallel execution, explicit
3 GiB Gradle/Kotlin heaps and preserved required JVM arguments. Bound forked
test/native concurrency and coordinate memory-heavy verification. Evaluate
in-process Kotlin compilation against this project's actual plugin versions and
required behavior. Raise a budget only after diagnosing an actual memory
failure. Report the actual profile and gate acquisition for the next required
focused build/test; do not rerun unaffected verification merely to exercise it.

R24 (2026-10-04): the user's correction confirms GitHub delivery is the expected
endpoint for the verified 0.3.1 dialog update. Publish that completed R22 scope
with accurate release notes explicitly identifying the unresolved APKMirror
automatic correction and absent new physical-phone proof. Keep R21 BLOCKED.

Stage 14 — publish verified 0.3.1 dialog update: COMPLETE (R24/R22). Acceptance:
reconcile unchanged production code/artifact against the reviewed build, lint,
signature, UI and upgrade evidence; preserve the existing certificate/package;
commit/push source and release notes; publish a normal/latest GitHub v0.3.1 with
the verified APK; independently download its asset and compare bytes, size and
digest; clean release-verification temporary files and record actual completion.
No Gradle run is needed for unchanged application code and the verified artifact.

Stage 14 closure (2026-10-04): reviewed unchanged production-code and artifact
evidence passes the release boundary. Source/release notes were committed and
pushed; normal/latest GitHub v0.3.1 targets
`dc476932fe7ccf295dbc95c1049a3046b94d07af`. Its single APK asset is 2,103,319
bytes, SHA-256 `b7d5d1ad62399f52ff83bbe20ce06f645b3d9f0c47386b195e32d6d32aa73ca9`.
GitHub reports uploaded/non-draft/non-prerelease status and the matching digest.
An independent release download was byte-equal to the tested local APK. Reviewed
cleanup removed only that temporary verification download; the original local
deliverable and evidence remain. No Gradle command was run, so no build-gate or
new-profile verification is claimed. Stage 13 remains BLOCKED on B5/B6; release
notes explicitly retain those limits. Stage 14 has no blocker or tracked deferral.

## R25: Adopt deployed transactional admission and release (2026-10-04)

The user authorized monitoring the Byparr task, adopting its verified feature,
and publishing a new release. The original endpoint now runs the qualified
`custom-post-scripting-queue` image from Byparr commit `cd07c46`; its API retains
GET/POST/script replies, queues all requests without a flag, excludes waiting
from `maxTimeout`, and returns pre-submission 503 when its 16-waiter backlog is
full. Do not modify the server or replay admitted operations.

Stage 15 — queued admission and 0.3.2 delivery: COMPLETE (R25/R23). Acceptance:
verify Android requests can wait behind a real held operation on the original
endpoint and resolve once; Android cancellation disconnects/removes a queued
request before browser submission; preserve execution budgets and absence of a
client queue deadline; explain the specific full-backlog response accurately
without relabeling other 503 errors or automatically retrying. Keep existing
inline Retry/Open in Browser and settings/signing identity. Verify focused
contracts, signed build/lint, in-place upgrade and real Uptodown original APK /
metadata / unchanged receiving URI on an isolated emulator. Record effective
Gradle gate/resource evidence, commit/push and publish normal/latest v0.3.2;
independently compare uploaded bytes and clean expendable outputs. The established
emulator release boundary applies; Stage 13's independent B5/B6 remain BLOCKED.
Queue adoption does not satisfy the APKMirror native attachment correction.

Plan: demonstrate the deployed queue and cancellation contract, apply only the
confirmed full-backlog error adaptation, verify the integrated Android original
workflow and release identity, then deliver and reconcile this scope. No tracked
deferral is created; do not claim overall Stage 13 completion.

Stage 15 pre-delivery reconciliation: the actual original endpoint and its running
image match the Byparr queue contract. Android cancellation removes a waiting
request while its controlled owner stays held; a second queued Android request
resolves the real YouTube page after release. Existing transport is compatible
without a flag, new client deadline or replay. The exact full-backlog message
regression and other-503 distinction pass, as do affected inline error/settings/
lifecycle and Uptodown capture contracts. Signed build/lint, JVM contracts,
unchanged certificate and 0.3.1 -> 0.3.2 saved-endpoint upgrade pass. The signed
release freshly downloads the exact Showly 3.72.0/build 843 original through the
updated server and verifies metadata/hash and unchanged readable receiving URI.
The effective gated resource profile is recorded in `verification.md`. Required
delivery/cleanup criteria remain until the verified GitHub upload, independent
asset comparison and reviewed temporary cleanup complete. B5/B6 remain confined
to the still-BLOCKED Stage 13; no Stage 15 blocker or tracked deferral exists.

Stage 15 delivery reconciliation: source commit
`56ef75ce74789ca26bd612b70e5895cc7a903b8b` was pushed and published as the normal,
latest GitHub release `v0.3.2`. The independently downloaded asset matches the
tested signed deliverable byte-for-byte, including its SHA-256 and 2,103,647-byte
size. Reviewed transactional cleanup removed the task's temporary build/emulator
root and failed-command report directories without residual files. Task-owned
verification processes stopped; the signed deliverable and required evidence
remain. Every Stage 15 acceptance criterion is verified, with no blocker or
tracked deferral. Stage 13 remains BLOCKED on the independent B5/B6 requirements.

## R26: JDK 21 migration and single-version host policy (2026-10-04)

The user authorized migrating Morphe and enforcing one JDK major version on this
Windows host. The user clarified that this policy covers development JDKs only;
preserve app-owned runtimes, including Bisq's Java 11 and JDownloader's Java 17.
Use the existing machine-wide Zulu JDK 21 for Gradle, compilation
and tests. Both Android modules retain Java/Kotlin JVM target 17 and the existing
SDK/package/signing/application behavior. CI selects JDK 21. Project-local
toolchain discovery uses JAVA_HOME without automatic downloads or discovery of
other installations. Do not install another JDK, modify unrelated repositories,
interrupt another build, or overwrite the published signed APK. This build-only
migration does not authorize a new app release or change Stage 13's B5/B6.

Stage 16 — migrate and verify JDK 21: COMPLETE (R26). Acceptance: both modules use
the JDK 21 toolchain with explicit matching JVM target 17; CI and build guidance
agree; fresh focused companion JVM tests, debug/release compilation, Android test
compilation and release lint pass using only the existing Zulu 21 installation.
Inspect effective toolchain, class-file target and resource settings from a gated
build. Preserve the existing release APK and signing identity.

Stage 17 — remove superseded host JDKs and reconcile: COMPLETE (R26).
Acceptance: inventory actual JDK installations and caches, distinguish embedded
JREs from JDKs, check remaining project requirements and active process identities,
remove superseded JDK 17/25 development toolchains only after migration verification
and safe coordination, and confirm only JDK 21 remains in the inspected inventory.
Retain same-major JDK 21 copies where they serve existing projects. Clean task
build outputs with reviewed tickets, record evidence, reconcile R26 and commit.
No tracked deferrals. If another requirement or live consumer prevents safe
removal, identify its exact owner and resolution condition rather than deleting
an in-use dependency or claiming the single-version policy satisfied.

Stage 16 reconciliation: the companion's JVM tests, debug/release and Android
test compilation, signed release verification and release lint pass. Toolchain
reports for both modules resolve only the existing Zulu 21 and confirm matching
Java/Kotlin target 17 with provisioning/discovery disabled. The pinned-source API
adapter now uses JDK 21 and the host build gate; its actual build passes, followed
by successful legacy-module compilation and D8 conversion against that adapter.
The published 0.3.2 APK retains its original hash. Effective gated settings are
recorded in verification.md. No Stage 16 blocker or tracked deferral remains.

Stage 17 reconciliation: reviewed deletion under the host-wide mutex removed the
17/25 development toolchains, archives and locks after confirming no active build
or old-JDK process. Their exact roots are absent. The accessible compiler inventory
now contains only development JDK 21; machine/process JAVA_HOME select Zulu 21.
User-exempt app runtimes and same-major copies serving other projects remain.
The other-account cache named JDK 21 was unreadable and is documented without an
unsupported inspection claim. The task's 4,609 temporary build/source members were
reviewed and deleted with no errors; its temporary root is absent, while required
verification evidence and the original signed release remain. R26 is satisfied
and verified with no blocker or tracked deferral. Stage 13's B5/B6 stay BLOCKED.
