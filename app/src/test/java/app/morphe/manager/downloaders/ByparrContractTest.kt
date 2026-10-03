package app.morphe.manager.downloaders

import org.junit.Assert.*
import org.junit.Test

class ByparrContractTest {
    @Test fun endpointIsExplicitHttpsAndNeverContainsCredentials() {
        assertEquals("https://server.example:8191/v1", ByparrEndpoint.normalize("https://server.example:8191/", false))
        assertEquals("https://server.example/v1", ByparrEndpoint.normalize("https://server.example/v1/", false))
        for (url in listOf("http://server.example:8191", "https://user:password@server.example/",
            "https://server.example/?token=secret", "https://server.example/#fragment", "file:///v1")) {
            assertNull(url, ByparrEndpoint.normalize(url, false))
        }
        assertNotNull(ByparrEndpoint.normalize("http://10.0.2.2:8765/v1", true))
    }

    @Test fun actualDownloadMarkupAndVariantAmbiguityArePreserved() {
        val url = "https://www.apkmirror.com/apk/example/example-1-release/"
        val variants = RemotePage.parse(url, "<title>Example 1</title>" +
            "<div class='table-row'>arm64 <a href='example-1-android-apk-download/'>1</a>" +
            "<a href='example-1-android-apk-download/#disqus_thread'>comments</a></div>" +
            "<div class='table-row'>x86 <a href='example-1-2-android-apk-download/'>1</a></div>")
        assertNull(variants.next)
        assertEquals(2, variants.choices.size)
        assertTrue(variants.choices[0].label.contains("arm64"))
        val landing = RemotePage.parse(url + "example-1-android-apk-download/",
            "<a class='downloadButton' href='download/?key=abc'>Download APK Bundle</a>")
        assertTrue(landing.next!!.endsWith("/download/?key=abc"))
        val attachment = RemotePage.parse(url + "example-1-android-apk-download/download/?key=abc",
            "<a id='download-link' href='/wp-content/themes/APKMirror/download.php?id=1&amp;key=abc'>here</a>")
        assertTrue(attachment.isAttachment(attachment.next!!))
        assertTrue(attachment.next!!.endsWith("?id=1&key=abc"))
    }

    @Test fun challengeAndNotFoundContentCannotPretendToBeSuccess() {
        for (html in listOf("<title>Just a moment...</title><div id='challenge-stage'>Verify</div>",
            "<title>Page Not Found - APKMirror</title>")) {
            assertThrows(java.io.IOException::class.java) {
                RemotePage.parse("https://www.apkmirror.com/apk/example/", html)
            }
        }
        assertNull(RemotePage.parse("https://www.apkmirror.com/apk/example/",
            "<a href='example-1-release/'>Example 1</a>").next)
    }

    @Test fun sessionCookiesStayWithinTheirActualOriginAndPath() {
        val cookie = RemoteCookie("clearance", "value", ".apkmirror.com", "/apk/", true, -1.0)
        assertTrue(cookie.matches("https://www.apkmirror.com/apk/example/"))
        assertFalse(cookie.matches("https://www.apkmirror.com/wp-content/download.php"))
        assertFalse(cookie.matches("https://apkmirror.com.evil.example/apk/"))
        assertFalse(cookie.matches("https://other.r2.cloudflarestorage.com/apk/"))
        assertFalse(cookie.matches("http://www.apkmirror.com/apk/"))
        assertFalse(cookie.copy(expires = 1.0).matches("https://www.apkmirror.com/apk/"))
        assertFalse(cookie.copy(value = "value\r\nInjected: header").matches("https://www.apkmirror.com/apk/"))
    }
}
