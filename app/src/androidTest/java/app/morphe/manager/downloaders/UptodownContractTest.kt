package app.morphe.manager.downloaders

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UptodownContractTest {
    private val url = "https://showly-2-0.en.uptodown.com/android/download/1220892131-x"
    private val html = "<h1>Showly</h1><iframe src='https://challenges.cloudflare.com/widget'></iframe>" +
        "<button id='detail-download-button' data-app-id='888750' data-file-id='1220892131' data-download-version='1220892131'></button>"
    private fun reply(): JSONObject = JSONObject().put("solution", JSONObject().put("response", html))
        .put("scriptResult", JSONObject().put("terminalCapture", "attachment")
            .put("value", JSONObject().put("kind", "request").put("method", "GET").put("aborted", true)
                .put("url", "https://dw.uptodown.com/dwn/fixture-key")
                .put("requestHeaders", JSONObject().put("user-agent", "fixture browser")
                    .put("accept", "*/*").put("referer", "https://showly-2-0.en.uptodown.com/")
                    .put("cookie", "do-not-forward=secret").put("authorization", "do-not-forward")))
            .put("captures", JSONObject().put("resolution", JSONObject().put("kind", "response").put("method", "POST")
                .put("url", "https://showly-2-0.en.uptodown.com/ajax/app/888750/file/1220892131/download-url")
                .put("status", 200).put("body", JSONObject().put("success", 1)
                    .put("data", JSONObject().put("downloadURL", "fixture-key"))))))
    private fun resolve(value: JSONObject) = Uptodown.resolved(value, RemotePage.parse(url, html), url, "fixture browser")

    @Test fun verifiedAbortedRequestUsesOnlyItsScopedBrowserHeaders() {
        val (page, headers) = resolve(reply())
        assertEquals("https://dw.uptodown.com/dwn/fixture-key", page.next)
        assertTrue(page.isAttachment(page.next!!))
        assertEquals(setOf("accept", "referer"), headers.keys)
        assertTrue("The packaged recipe must be available on Android", Uptodown.script().contains("await byparr.finishWith"))
        val cookie = RemoteCookie("clearance", "value", ".uptodown.com", "/dwn/", true, -1.0)
        assertTrue(cookie.matches(page.next!!))
        assertFalse(cookie.matches("https://dw.uptodown.net/dwn/fixture-key"))
        assertFalse(cookie.matches("https://dw.uptodown.com/other/fixture-key"))
    }
    @Test fun rejectedOrUnrelatedCapturesCannotBecomeDownloads() {
        val changes: List<(JSONObject) -> Unit> = listOf(
            { it.remove("scriptResult") },
            { it.getJSONObject("scriptResult").put("terminalCapture", "other") },
            { it.getJSONObject("scriptResult").getJSONObject("value").put("kind", "response") },
            { it.getJSONObject("scriptResult").getJSONObject("value").put("method", "POST") },
            { it.getJSONObject("scriptResult").getJSONObject("value").put("aborted", false) },
            { it.getJSONObject("scriptResult").getJSONObject("value").put("aborted", "true") },
            { it.getJSONObject("scriptResult").getJSONObject("value").put("url", "https://dw.uptodown.com/dwn/other-key") },
            { it.getJSONObject("scriptResult").getJSONObject("captures").getJSONObject("resolution").put("status", 400) },
            { it.getJSONObject("scriptResult").getJSONObject("captures").getJSONObject("resolution").put("status", "200") },
            { it.getJSONObject("scriptResult").getJSONObject("captures").getJSONObject("resolution").put("url", "https://showly-2-0.en.uptodown.com/ajax/app/888750/file/999/download-url") },
            { it.getJSONObject("scriptResult").getJSONObject("captures").getJSONObject("resolution").getJSONObject("body").put("success", 0) },
            { it.getJSONObject("scriptResult").getJSONObject("captures").getJSONObject("resolution").getJSONObject("body").getJSONObject("data").put("downloadURL", "") },
            { it.getJSONObject("scriptResult").getJSONObject("value").getJSONObject("requestHeaders").put("user-agent", "different browser") },
            { it.getJSONObject("scriptResult").getJSONObject("value").getJSONObject("requestHeaders").put("accept", "*/*\r\nInjected: value") },
            { it.getJSONObject("scriptResult").getJSONObject("value").getJSONObject("requestHeaders").put("referer", "https://unrelated.example/") }
        )
        changes.forEachIndexed { index, change ->
            val value = reply().also(change)
            try { resolve(value); fail("Invalid capture $index was accepted") }
            catch (_: IOException) { /* Explicit rejection is the contract. */ }
        }
    }
}
