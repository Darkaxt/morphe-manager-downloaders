"""Local Android integration fixture; never a production APKMirror substitute."""
import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse
from threading import Event
from hashlib import sha256


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    payload = Path(args.apk).read_bytes()
    slow_started, slow_release = Event(), Event()
    retry_requests = 0

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            nonlocal retry_requests
            if self.path in ("/queue-full/v1", "/unavailable/v1"):
                self.rfile.read(int(self.headers["Content-Length"]))
                detail = "Browser queue full; no request was queued or submitted" if self.path == "/queue-full/v1" else "Service unavailable"
                data = json.dumps({"detail": detail}).encode()
                self.send_response(503)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
            if self.path == "/retry/v1":
                retry_requests += 1
                if retry_requests == 1:
                    self.html("API unavailable", code=503)
                    return
                self.path = "/v1"
            if self.path == "/error/v1":
                self.html("API unavailable", code=502)
                return
            if self.path != "/v1":
                self.html("Not found", code=404)
                return
            request = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            url = request["url"]
            path = urlparse(url).path
            pages = {
                "/release/": '<a href="/release/one-android-apk-download/">Universal APK</a>',
                "/release/one-android-apk-download/": '<a class="downloadButton" href="/download.php?id=fixture">DOWNLOAD APK</a>',
                "/attachment-choices/": '<a class="downloadButton" href="/download.php?id=first">First APK</a>'
                                        '<a class="downloadButton" href="/download.php?id=second">Second APK</a>',
                "/variants/": '<div class="table-row">arm64 <a href="/variants/one-android-apk-download/">APK</a></div>'
                              '<div class="table-row">x86 <a href="/variants/two-android-apk-download/">APK</a></div>',
                "/challenge/": '<div id="challenge-stage">Human verification required</div>',
            }
            title = "APKMirror fixture" if path in pages else "Page Not Found - APKMirror"
            html = f'<html><head><title>{title}</title></head><body>{pages.get(path, "Not found")}</body></html>'
            data = json.dumps({"status": "ok", "solution": {"status": 200, "url": url,
                "userAgent": "Android Byparr fixture", "contentType": "text/html", "response": html,
                "cookies": [{"name": "session", "value": "morphe_fixture", "domain": "10.0.2.2",
                             "path": "/", "secure": False, "expires": -1}]}}).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def do_GET(self):
            nonlocal retry_requests
            if self.path == "/retry-reset":
                retry_requests = 0
                self.html("Reset")
                return
            path = urlparse(self.path).path
            if path == "/release/":
                self.html('<a href="/release/one-android-apk-download/">Universal APK</a>', cookie=True)
            elif path == "/release/one-android-apk-download/":
                self.html('<a class="downloadButton" href="/download.php?id=fixture">DOWNLOAD APK</a>')
            elif path == "/download.php":
                self.send_response(302)
                self.send_header("Location", "/files/fixture.apk")
                self.end_headers()
            elif path in ("/files/fixture.apk", "/process.apk"):
                if path != "/process.apk" and ("session=morphe_fixture" not in self.headers.get("Cookie", "") or
                    "Android" not in self.headers.get("User-Agent", "") or
                    not self.headers.get("Referer", "")):
                    self.html("<h1>Missing browser-session headers</h1>", code=403)
                    return
                self.send_response(200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Disposition", 'attachment; filename="fixture.apk"')
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)
            elif path == "/variants/":
                self.html('<a href="/variants/one-android-apk-download/">arm64</a>'
                          '<a href="/variants/two-android-apk-download/">x86</a>')
            elif path == "/challenge/":
                self.html('<div id="challenge-stage">Human verification required</div>')
            elif path == "/expected-hash":
                data = sha256(payload).hexdigest().encode()
                self.send_response(200)
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            elif path == "/html.apk":
                data = b"<html>Verification instead of an archive</html>"
                self.send_response(200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            elif path == "/slow.apk":
                start = int(self.headers.get("Range", "bytes=0-").removeprefix("bytes=").split("-")[0])
                self.send_response(206 if start else 200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Length", str(len(payload) - start))
                self.send_header("ETag", '"' + sha256(payload).hexdigest() + '"')
                self.send_header("Accept-Ranges", "bytes")
                if start:
                    self.send_header("Content-Range", f"bytes {start}-{len(payload)-1}/{len(payload)}")
                self.end_headers()
                first = min(len(payload), start + 262144)
                self.wfile.write(payload[start:first])
                self.wfile.flush()
                slow_started.set()
                slow_release.wait()
                try:
                    self.wfile.write(payload[first:])
                except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                    pass  # The cancellation test deliberately closes the transfer.
            elif path == "/slow-started":
                slow_started.wait()
                self.html("Transfer started")
            elif path == "/slow-reset":
                slow_started.clear()
                slow_release.clear()
                self.html("Transfer gate reset")
            elif path == "/slow-release":
                slow_release.set()
                self.html("Transfer released")
            else:
                self.html("<h1>Not found</h1>", code=404)

        def html(self, body, code=200, cookie=False):
            data = ('<!doctype html><html><head><title>APKMirror fixture</title></head>'
                    '<body>' + body + '</body></html>').encode()
            self.send_response(code)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            if cookie:
                self.send_header("Set-Cookie", "session=morphe_fixture; Path=/; SameSite=Lax")
            self.end_headers()
            self.wfile.write(data)

    print(f"Fixture server: http://127.0.0.1:{args.port}/release/", flush=True)
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
