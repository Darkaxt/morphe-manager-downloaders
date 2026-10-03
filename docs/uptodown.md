# Uptodown through Byparr browser scripting

Uptodown support uses the existing saved private Byparr `/v1` endpoint. It requires
the `custom-post-scripting` update from [Darkaxt/Byparr](https://github.com/Darkaxt/Byparr).
Morphe Downloader includes the caller-owned [recipe](https://github.com/Darkaxt/Byparr/blob/5671f2474493d6fb2ce5ab5d2cdd4e7fbc47db2f/examples/uptodown.js)
from commit `5671f2474493d6fb2ce5ab5d2cdd4e7fbc47db2f`, under the same GPLv3 license.
Its packaged UTF-8 contents are byte-identical to that source (SHA-256
`123deb09a2ff8aef9e49059acd8782b5a6f27868b02244c9184d9b8ccffc1735`).

App/download pages resolve their actual button's numeric file ID to an exact
`/android/download/{fileId}-x` page. An explicit build must match both button file
attributes. Available explicit release links are presented for selection when
there is no unique download. The exact build request includes `script`,
`scriptArgs.fileId`, `blockMedia: false` and the API's own `maxTimeout: 120000`.
The recipe waits for the website load event, registers captures, selects the
terminal attachment capture and clicks with native Playwright input. Uptodown
generates and submits its own fresh token; the companion never extracts or replays it.

Before native transfer the client verifies API HTTP 200/status ok, the matching
website POST URL/status 200/success=1, and the aborted GET request's exact
correspondence to the signed `downloadURL`. Missing or inconsistent scripting
output is an error, not a download candidate. Embedded Turnstile content alone
does not imply an unresolved interstitial; the captured successful operation
establishes the result. Actual challenge pages and failed captures remain errors.

Only captured `accept` and same-origin `referer` headers are reused. Captured and
solution user agents must agree. Cookies are selected by domain, path, secure
transport and expiration for each native resolution request and the final system
download destination. Captured Cookie/Authorization headers are not forwarded.
Attachment redirects are confined to HTTPS `dw.uptodown.com/dwn/` and
`dw.uptodown.net/dwn/`. Native downloading supplies the actual attachment status;
the aborted capture has none. The bytes determine APK/APKM/XAPK format, regardless
of the site's `onlyXapk` option.

HTTP 429 means Byparr rejected admission before submission. There is no automatic
retry, token replay, arbitrary wait or public default service. A failure offers the
existing Cancel/Save setup dialog. Use the current endpoint and updated backend;
server/session acceptance can vary on subsequent requests.

The companion's live Showly 3.72.0 build `1220892131` test passed on the isolated
Android emulator: 12,004,991 bytes, package `com.michaldrabik.showly2`, build 843,
SHA-256 `f0fac7fc5474168ca982fd220361f3bbb76d4f4f3ed9e89cf03d2465d2b77074`.
Archive metadata and the original hash were checked, and a separate receiving app
read unchanged bytes through the granted URI. See [verification](verification.md).
