# Uptodown browser-action handoff for Byparr

Status: the backend gap described below is resolved by Byparr commit `5671f24`.
Morphe Downloader's integration has passed the real Android download/receiver
workflow. See [current integration](uptodown.md); the following notes preserve the
earlier investigation and owner handoff, not the current supported API.

## Request and boundary

Morphe Downloader needs to resolve an original APK from a specific Uptodown build
using the existing Byparr service. The user directed this task to leave Byparr
unchanged and hand the problem to its task owner. Use the original deployed
endpoint on port 8191; the historical isolated port is irrelevant to this work.

This is a browser token-generation gap, not evidence that the new POST transport
is broken. The companion will download, validate and share the original file
itself once it receives a usable attachment URL.

## Reproduction observed on 2026-10-03

Representative page:

https://showly-2-0.en.uptodown.com/android/download/1220892131-x

This is Showly 3.72.0, application ID 888750, file ID 1220892131. The live page's
download button has these identifiers but no static `data-url` attachment link.

Capturing the successful flow in the user's Helium browser showed:

1. Clicking `#detail-download-button` runs the site's embedded Turnstile flow.
2. The page submits the resulting token to the same-origin endpoint:

   ```http
   POST /ajax/app/888750/file/1220892131/download-url
   Content-Type: application/json

   {"token":"<fresh token generated in this browser>","onlyXapk":"1"}
   ```

3. The endpoint returns HTTP 200 with:

   ```json
   {"success":1,"data":{"downloadURL":"<signed relative attachment key>"}}
   ```

4. The page opens `https://dw.uptodown.com/dwn/<signed key>`, which redirects to
   `dw.uptodown.net`. A native GET of the captured link returned the original APK:
   12,004,991 bytes, SHA-256
   `f0fac7fc5474168ca982fd220361f3bbb76d4f4f3ed9e89cf03d2465d2b77074`.

The captured request's `onlyXapk` value is a page-provided option, not reliable
evidence of the returned archive format. This particular response was an APK.
The companion must inspect the bytes as it does for the other sources.

## What has and has not passed

- The original endpoint reports `custom-post` at `/ready`.
- Its GET successfully returns the requested page, but the Turnstile response
  input has no token. Page navigation alone did not run the button's full flow.
- Native POST with an empty token returns HTTP 400:
  `{"success":0,"errorCode":-51,"errorMsg":"Bad Request"}`.
- Attempts to submit the Uptodown POST through the deployed custom-post API
  returned HTTP 429, `Browser busy; no request was queued or submitted`, while
  another validation occupied its browser. No upstream POST result was obtained
  in those attempts; they do not establish a POST implementation failure.
- The current API offers GET/POST navigation and same-origin `preflightUrl`, but
  no operation to click this page's download button and capture its AJAX result.
  The inspected challenge entry point detects Cloudflare interstitials; the
  embedded widget/button flow is a separate integration boundary.
- The captured attachment works with native HTTP. Uptodown resolution through
  Byparr and the companion's full Android/receiver workflow remain unverified.

Private captures contain tokens, cookies and signed links. They are retained in
gitignored local verification artifacts and must not be included in public logs,
fixtures, commits or this handoff.

## Requested capability, for the owner to design

Provide a supported operation that, in one browser context, opens the supplied
Uptodown build page, activates its actual download button, waits for the actual
download-url response, and returns the signed attachment URL with any required
user-agent/cookie context. Derive application/file IDs and options from that page;
preserve the explicitly requested file ID and fail if the site changes it.

The owner should choose the API shape. No command name or parameter in this
document is claimed to exist. Returning the signed URL is sufficient; Byparr
does not need to relay the APK bytes or expose unrestricted browser scripting.

Preserve existing admission, context cleanup, POST replay permission and target
status semantics. Test sequentially without overlapping deployment validation.
If the server browser cannot complete the live widget flow, return that actual
failure; do not substitute a captured or empty token, another build, automatic
retries or an arbitrary delay.

Acceptance evidence should include a fresh real per-build operation that returns
a usable signed link, followed by an original-file GET. Distinguish controlled
fixtures from live Turnstile success. Once this boundary is available, this task
will integrate the documented API and verify Android download, archive/package/
version checks and unchanged bytes through the receiving URI.
