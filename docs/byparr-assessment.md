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
- Send POST `/v1` with `cmd: request.get`, the APKMirror URL and the API protocol's
  `maxTimeout: 60000`. Process page requests sequentially; no automatic retries.
- Check API HTTP status, result status, target status and returned HTML. Reject
  unresolved challenges, empty/unusable pages and APKMirror's not-found content.
- Follow unique download steps; present native choices when ambiguous.
- Resolve attachment redirects with the returned user agent, Referer and cookies
  scoped to their domain, path, expiry and secure flag. APKMirror cookies never
  go to the R2 host. Restrict HTTPS attachments to APKMirror and its observed
  exact R2 account/path; do not accept arbitrary Cloudflare bucket hosts.
- Enqueue the final original URL with Android DownloadManager. Validate the archive
  before temporary content-URI sharing. Missing settings/failures prompt for an
  endpoint and explicit retry. Cancellation uses the actual request/download state.

The API's POST transport is distinct from upstream `request.post`. The user's
forthcoming POST-capable fork can retain this GET contract; this APKMirror flow
currently needs no upstream POST. No speculative POST workflow was introduced.

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
A phone must reach its configured server. The Byparr-only build was not live-tested
on the physical phone; the previous embedded-browser build looped there, while
external-browser success was reported by the user.

Keep the API private: it can initiate server-side browser requests. The app does
not change Tailscale, expose a server, or deploy the user's POST fork.
