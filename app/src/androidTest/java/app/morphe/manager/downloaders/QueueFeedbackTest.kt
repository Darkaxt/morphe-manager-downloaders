package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ProgressBar
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class QueueFeedbackTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = "http://10.0.2.2:8765"
    private fun control(path: String): String {
        val conn = URI(fixture + path).toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 15000; conn.readTimeout = 15000 // Test diagnostics only.
        try { assertEquals(200, conn.responseCode); return conn.inputStream.bufferedReader().use { it.readText() } }
        finally { conn.disconnect() }
    }

    @Test fun validatesIdentityAndQueueSemantics() {
        fun reply(state: String = "queued", position: Any = 2, total: Int = 3, id: String = "current") =
            JSONObject().put("requestId", id).put("state", state).put("position", position)
                .put("total", total).put("queueLimit", 16)
        assertEquals(ByparrClient.QueueStatus(2, 3), ByparrClient.QueueStatus.parse(reply(), "current"))
        assertTrue(ByparrClient.QueueStatus(2, 3).message.contains("1 request queued ahead"))
        assertTrue(ByparrClient.QueueStatus(1, 3).message.contains("0 requests queued ahead"))
        assertEquals(ByparrClient.QueueStatus(0, 3), ByparrClient.QueueStatus.parse(reply("active", 0), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply(id = "old"), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply(position = "2"), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply(position = 2.5), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply(position = 0), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply(position = 4), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply(total = 17), "current"))
        assertNull(ByparrClient.QueueStatus.parse(reply("completed", 0), "current"))
    }

    @Test fun dialogTracksQueueThenActiveAndRealByteProgress() {
        Downloads(context).cancel()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "$fixture/feedback/v1").commit()
        control("/feedback-reset"); control("/slow-reset")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            ActivityScenario.launch<MainActivity>(Intent(Intent.ACTION_VIEW, Uri.parse("$fixture/release/"), context, MainActivity::class.java)).use { scenario ->
                assertTrue(device.wait(Until.hasObject(By.textContains("Queue position 2 of 2")), 15000))
                scenario.onActivity { activity ->
                    fun bars(view: android.view.View): List<ProgressBar> =
                        if (view is ProgressBar) listOf(view) else if (view is android.view.ViewGroup)
                            (0 until view.childCount).flatMap { bars(view.getChildAt(it)) } else emptyList()
                    assertTrue(bars(activity.window.decorView).single().isIndeterminate)
                }
                control("/feedback-phase/first")
                assertTrue(device.wait(Until.hasObject(By.textContains("Queue position 1 of 1")), 15000))
                device.takeScreenshot(File(context.getExternalFilesDir(null), "queue-dialog.png"))
                control("/feedback-phase/active")
                assertTrue(device.wait(Until.hasObject(By.text("Byparr is resolving the download…")), 15000))
                control("/feedback-release")
                control("/slow-started")
                assertTrue(device.wait(Until.hasObject(By.textContains("Downloading…")), 15000))
                assertFalse(device.hasObject(By.textContains("Queue position")))
                device.takeScreenshot(File(context.getExternalFilesDir(null), "byte-progress.png"))
                device.findObject(By.text(context.getString(R.string.cancel))).click()
            }
        } finally { control("/feedback-release"); control("/slow-release"); Downloads(context).cancel() }
    }

    @Test fun cancellingQueuedDialogAndRecreatingDoesNotRestoreOldFeedback() {
        Downloads(context).cancel()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "$fixture/feedback/v1").commit()
        control("/feedback-reset")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            ActivityScenario.launch<MainActivity>(Intent(Intent.ACTION_VIEW, Uri.parse("$fixture/release/"), context, MainActivity::class.java)).use { scenario ->
                assertTrue(device.wait(Until.hasObject(By.textContains("Queue position 2 of 2")), 15000))
                device.findObject(By.text("Cancel page request")).click()
                assertTrue(device.wait(Until.hasObject(By.text("Page request cancelled.")), 15000))
                control("/feedback-phase/active"); control("/feedback-release")
                scenario.recreate()
                assertFalse(device.hasObject(By.textContains("Queue position")))
                assertFalse(device.hasObject(By.text("Byparr is resolving the download…")))
                assertFalse(context.getSharedPreferences("browser", Context.MODE_PRIVATE).getBoolean("awaitingDownload", true))
            }
        } finally { control("/feedback-release") }
    }

    @Test fun absentFeedbackDoesNotCompleteOrReplayBrowserOperation() {
        control("/feedback-reset"); control("/feedback-phase/missing")
        val client = ByparrClient(true)
        val pool = Executors.newSingleThreadExecutor()
        val request = pool.submit<ByparrClient.Page> { client.fetch("$fixture/feedback/v1", "$fixture/release/") { fail("404 must not invent feedback") } }
        try {
            control("/feedback-sampled")
            assertFalse("A missing feedback entry must not finish /v1", request.isDone)
            control("/feedback-release")
            assertEquals("Queue fixture", request.get(15, TimeUnit.SECONDS).content.appName)
            assertFalse(client.receivingQueueFeedback)
            assertTrue(control("/feedback-posts").contains("<body>1</body>"))
        } finally { client.cancel(); control("/feedback-release"); pool.shutdown() }
    }

    @Test fun cancellingHeldRequestStopsFeedbackWithoutReplay() {
        control("/feedback-reset")
        val observed = CountDownLatch(1)
        val client = ByparrClient(true)
        val pool = Executors.newSingleThreadExecutor()
        val request = pool.submit<ByparrClient.Page> {
            client.fetch("$fixture/feedback/v1", "$fixture/release/") { observed.countDown() }
        }
        try {
            assertTrue(observed.await(15, TimeUnit.SECONDS))
            client.cancel()
            try { request.get(15, TimeUnit.SECONDS); fail("Cancelled request returned a page") }
            catch (error: ExecutionException) { assertTrue(error.cause is IOException) }
            assertFalse(client.receivingQueueFeedback)
            assertTrue(control("/feedback-posts").contains("<body>1</body>"))
        } finally { client.cancel(); control("/feedback-release"); pool.shutdown() }
    }

    /** External harness holds a real browser owner and earlier waiter, then cancels that waiter. */
    @Test fun realServerPositionMovesAndCancellationStopsFeedback() {
        val args = InstrumentationRegistry.getArguments()
        val endpoint = args.getString("feedbackEndpoint")
        val url = args.getString("feedbackUrl")
        assumeTrue(endpoint != null && url != null)
        val second = CountDownLatch(1); val first = CountDownLatch(1)
        var initialPosition = 0
        var finalPosition = 0
        val client = ByparrClient(true)
        val pool = Executors.newSingleThreadExecutor()
        val request = pool.submit<ByparrClient.Page> {
            client.fetch(endpoint!!, url!!) { status ->
                File(context.getExternalFilesDir(null), "feedback-position-${status.position}.txt").writeText(status.message)
                if (status.position > 0 && initialPosition == 0) {
                    initialPosition = status.position
                    // Observe the private caller UUID for this live API test only;
                    // do not add a production diagnostic interface or expose it in the UI.
                    val monitorField = client.javaClass.getDeclaredField("queueMonitor").apply { isAccessible = true }
                    val monitor = monitorField.get(client)!!
                    val idField = monitor.javaClass.getDeclaredField("requestId").apply { isAccessible = true }
                    File(context.getExternalFilesDir(null), "feedback-request-id.txt").writeText(idField.get(monitor) as String)
                    control("/real-observed/2"); second.countDown()
                } else if (status.position in 1 until initialPosition) {
                    finalPosition = status.position
                    control("/real-observed/1"); first.countDown()
                }
            }
        }
        try {
            assertTrue("Actual initial position not observed", second.await(60, TimeUnit.SECONDS))
            assertTrue("Actual position movement not observed", first.await(60, TimeUnit.SECONDS))
            assertFalse(request.isDone)
            client.cancel()
            try { request.get(15, TimeUnit.SECONDS); fail("Cancelled request returned a page") }
            catch (error: ExecutionException) { assertTrue(error.cause is IOException) }
            assertFalse(client.receivingQueueFeedback)
            File(context.getExternalFilesDir(null), "feedback-cancelled.txt").writeText("Cancelled after actual $initialPosition→$finalPosition movement; feedback stopped; no result or replay")
        } finally { client.cancel(); pool.shutdown() }
    }
}
