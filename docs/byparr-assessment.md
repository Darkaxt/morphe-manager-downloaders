# Byparr integration

The user authorized Byparr-only page resolution on 2026-10-03. The companion no
longer contains an embedded WebView. Existing Morphe receives original files in
Expert mode and retains its signing identity.

## Verified workflow

A private HTTPS instance running Byparr 3.0.4 resolved the actual YouTube catalogue,
release, variant and download landing pages. The Android 15 emulator requested the
21.39.523 arm64 variant, followed the download.php redirect with returned session
headers, and downloaded the original APKM from APKMirror's signed R2 attachment URL.
Morphe 1.33.0 read and imported that bundle. No custom server deployment, relay,
patching or installation was necessary. See [verification](verification.md) for
hashes and evidence. The private hostname and cookies are excluded from source.

## Client contract

- Persist a private HTTPS endpoint; normalize a server base URL to `/v1`.
- Send POST `/v1` with `cmd: request.get`, the supported source URL and the API protocol's
  `maxTimeout: 60000`. Process page requests sequentially; no automatic retries.
- Check API HTTP status, result status, target status and returned HTML. Reject
  unresolved challenges, empty/unusable pages and APKMirror's not-found content.
- Follow unique download steps; present native choices when ambiguous.
- Resolve attachment redirects with the returned user agent, the selected link's
  Referer policy and cookies
  scoped to their domain, path, expiry and secure flag. APKMirror cookies never
  go to the R2 host. Restrict HTTPS attachments to APKMirror and its observed
  exact R2 account/path. APKPure attachments use its observed download/CDN hosts;
  APKCombo uses its observed `/d`, pureapk and winudf attachment paths. Each source
  has its own host policy. Do not accept arbitrary Cloudflare bucket hosts.
- Enqueue the final original URL with Android DownloadManager. Validate the archive
  before temporary content-URI sharing. Missing settings/failures prompt for an
  endpoint and explicit retry. Cancellation uses the actual request/download state.

The API's POST transport is distinct from upstream `request.post`. The original
saved endpoint now reports the user's deployed `custom-post` fork. The companion's
verified APKMirror/APKPure/APKCombo flows retain the GET contract: site scripts run
inside Byparr's browser and the companion downloads the resolved attachment.
APKCombo requires omitting Referer where its link declares `noreferrer`; preserving
that policy corrected the rejected redirect and passed real Android verification.

Uptodown is pending. Its download button generates an embedded Turnstile token
and submits a JSON POST for a signed URL. GET returned no token; native POST with
an empty token failed. Attempts through the deployed POST API were rejected as
busy before submission, not proven upstream failures. Script/browser-action
support is proposed but not available or integrated. The user directed this task
not to extend Byparr. See the [handoff](uptodown-byparr-handoff.md).

## Limits established by evidence

[Byparr 3.0.4's endpoint](https://github.com/ThePhaseless/Byparr/blob/v3.0.4/src/endpoints.py)
sets solution.status to 200. An invalid YouTube release returned not-found HTML
with a nominal successful result, so status checks alone are insufficient.
[Its content handler](https://github.com/ThePhaseless/Byparr/blob/v3.0.4/src/content.py)
returns HTML, with a PDF-specific binary case; it does not stream arbitrary APKs.
The verified client download therefore remains a separate network boundary.

[Cloudflare documents outgoing-IP consistency requirements](https://developers.cloudflare.com/cloudflare-challenges/concepts/how-challenges-work/).
The tested server/emulator combination passed that boundary, but another server,
network or future challenge may reject session reuse. Such failures are surfaced
for explicit action rather than hidden retries or an unimplemented relay fallback.
A phone must reach its configured server. The Byparr-only APKMirror original
download was live-tested on the physical phone; its initial import exposed a
Samsung file-location defect corrected in 0.1.1. The user subsequently reported
successful patching/installing after correcting Morphe's installer configuration.
Independent updated-phone import verification remains pending. The new APKPure
and APKCombo proof is on the isolated emulator, not the physical phone.

Keep the API private: it can initiate server-side browser requests. The app does
not change Tailscale, expose a server, or deploy the user's POST fork.
