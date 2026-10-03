# APKMirror attachment resolution investigation

Status: automatic correction remains unverified; this recipe is not shipped.
Observed on 2026-10-04 with the deployed generic scripting API, without changing
the Byparr deployment or its other consumers.

## Demonstrated failing boundary

The phone's YouTube 21.16.256 request fails in `ByparrClient.attachment`, before
the Android system download starts. Ordinary Byparr GET resolves the selected
variant and its download landing page with target HTTP 200. A native request to
the resulting `download.php`, using the returned browser user agent, scoped
cookies and landing referrer, returns HTTP 403, `cf-mitigated: challenge`, and
Cloudflare's challenge HTML. This is a rejected redirect request, not evidence
of a corrupt APK or an Android process crash.

The same selected page in the user's Helium browser produced this real chain:

1. Selected YouTube 21.16.256 plain APK variant, Android 9+, universal/nodpi.
2. That variant's `/download/` landing page, preserving `forcebaseapk=true`.
3. `/wp-content/themes/APKMirror/download.php?id=13517428&...`.
4. HTTP 302 to the already approved APKMirror storage bucket:
   `eb5e7388c3df147b74dd2379b7cf8323.r2.cloudflarestorage.com`, under
   `/downloadprod/wp-content/uploads/`, with the selected APK filename.

Page metadata: package `com.google.android.youtube`, version 21.16.256,
build 1561068412, 184,012,881 bytes. The browser's redirect is verified; a
companion download through Byparr for this file is not yet verified.

## Generic browser API findings

- Capturing the redirect with `watchResponse` returned HTTP 502, "Browser
  response capture failed". The checked backend's `collect_response` always
  reads `response.body()`, including redirects whose body is unavailable.
  Response captures should preserve the actual status and headers even when a
  redirect has no retrievable body. Do not fabricate attachment HTTP 200.
- A no-op script (`() => ({ready:true})`) on the selected variant returned
  API/target HTTP 200 and the expected JSON result. Scripting is not universally
  broken on APKMirror.
- Capturing `download.php` as a request and aborting the storage request avoids
  the redirect-body problem in principle. Live attempts from both the variant
  and landing page timed out at the API's 120-second boundary. Server logs show
  the initial Cloudflare challenge and checkbox attempts; no successful
  attachment capture was returned. Trials with normal media settings and
  `blockMedia:false` did not establish success. Do not infer a particular
  fingerprinting cause from these results.
- The shared service also returned immediate HTTP 429 while another consumer
  occupied its single browser slot. Those requests were not queued or submitted.
  Readiness HTTP 200 does not mean the browser slot is free. Other consumers were
  left running.

## Required proof before integrating a correction

Keep navigation/redirect resolution in the server browser. Return the verified
signed storage URL before transferring the APK there. A generic redirect
response capture with actual status/Location, or a verified aborted attachment
request, can provide this boundary. Preserve exact build/base-APK selection,
user agent, destination-scoped cookies, referrer and the existing host policy.

First demonstrate a complete live resolution on the original endpoint. Then
verify the companion's Android download, archive metadata and unchanged readable
receiving URI on the same connected phone. The phone was disconnected with the
user's agreement; this final device proof remains pending. No Byparr source,
deployment, consumer or phone network settings were changed in this investigation.
