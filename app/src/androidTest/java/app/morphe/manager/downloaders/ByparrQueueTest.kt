package app.morphe.manager.downloaders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/** The live cases require an externally held operation on the original endpoint. */
@RunWith(AndroidJUnit4::class)
class ByparrQueueTest {
    private val args get() = InstrumentationRegistry.getArguments()

    @Test fun fullBacklogHasAnAccurateMessageAndOther503sStayDistinct() {
        val client = ByparrClient(true)
        fun message(path: String) = try {
            client.fetch("http://10.0.2.2:8765/$path/v1", "http://10.0.2.2:8765/variants/")
            fail("Expected HTTP 503"); ""
        } catch (error: IOException) { error.message }
        assertEquals("Byparr's browser queue is full (HTTP 503). No page request was submitted. Use Retry to try again.", message("queue-full"))
        assertEquals("Byparr API failed (HTTP 503).", message("unavailable"))
    }

    private fun state(endpoint: String): JSONObject {
        val connection = URI(endpoint.removeSuffix("/v1") + "/ready").toURL().openConnection() as HttpURLConnection
        // Diagnostic HTTP budget, not the application's queue or execution budget.
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        try {
            assertEquals(200, connection.responseCode)
            val reply = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            assertEquals("custom-post-scripting-queue", reply.getString("version"))
            return reply.getJSONObject("browser")
        } finally { connection.disconnect() }
    }

    @Test fun queuedAndroidCancellationDisconnectsBeforeAdmission() {
        val endpoint = args.getString("byparrEndpoint")
        val url = args.getString("queueUrl")
        assumeTrue(endpoint != null && url != null && args.getString("queueCase") == "cancel")
        val baseline = state(endpoint!!)
        assertTrue("Test owner must already hold the browser", baseline.getBoolean("active"))
        val queued = baseline.getInt("queued")
        val client = ByparrClient(false)
        val executor = Executors.newSingleThreadExecutor()
        val request = executor.submit<ByparrClient.Page> { client.fetch(endpoint, url!!) }
        try {
            while (state(endpoint).getInt("queued") <= queued) {
                assertFalse("Request completed before joining the held queue", request.isDone)
            }
            client.cancel()
            try {
                request.get(15, TimeUnit.SECONDS)
                fail("Cancelled request returned a page")
            } catch (error: ExecutionException) { assertTrue(error.cause is IOException) }
            while (state(endpoint).getInt("queued") > queued) {
                assertTrue("Owner must stay held during cancellation proof", state(endpoint).getBoolean("active"))
            }
            assertTrue(state(endpoint).getBoolean("active"))
            println("PASS Android queued request removed while owner remained held; no admission or replay")
        } finally { client.cancel(); executor.shutdown() }
    }

    @Test fun queuedAndroidRequestResolvesAfterOwnerRelease() {
        val endpoint = args.getString("byparrEndpoint")
        val url = args.getString("queueUrl")
        assumeTrue(endpoint != null && url != null && args.getString("queueCase") == "resolve")
        assertTrue(state(endpoint!!).getBoolean("active"))
        val client = ByparrClient(false)
        try {
            // The external harness verifies queue membership before releasing its owner.
            val page = client.fetch(endpoint, url!!)
            assertEquals(DownloadSource.APK_MIRROR, page.content.source)
            assertTrue(page.content.appName.contains("YouTube", true))
            assertTrue(page.content.next != null || page.content.choices.isNotEmpty())
            println("PASS queued Android request resolved the actual YouTube page once")
        } finally { client.cancel() }
    }
}
