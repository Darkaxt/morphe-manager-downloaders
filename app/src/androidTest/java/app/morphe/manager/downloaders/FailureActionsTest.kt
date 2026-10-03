package app.morphe.manager.downloaders

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.net.URL

@RunWith(AndroidJUnit4::class)
class FailureActionsTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    @Test fun failedPageStaysInDialogAndRetryUsesTheSavedServer() {
        Downloads(context).cancel()
        val prefs = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val server = context.getSharedPreferences("byparr", Context.MODE_PRIVATE)
        server.edit().putString("endpoint", "http://10.0.2.2:8765/retry/v1").commit()
        URL("http://10.0.2.2:8765/retry-reset").openStream().close()
        val url = "http://10.0.2.2:8765/variants/"
        ActivityScenario.launch<MainActivity>(Intent(Intent.ACTION_VIEW, Uri.parse(url), context, MainActivity::class.java)).use { scenario ->
            assertNotNull("Failed request must offer Retry", device.wait(Until.findObject(By.text("Retry")), 15000))
            assertTrue(device.hasObject(By.text("Open in Browser")))
            assertNull("Failure must not open server configuration", device.findObject(By.clazz("android.widget.EditText")))
            assertTrue(device.hasObject(By.textContains("503")))
            scenario.recreate()
            assertNotNull(device.wait(Until.findObject(By.text("Retry")), 15000))
            assertNull(device.findObject(By.clazz("android.widget.EditText")))
            device.findObject(By.text("Retry")).click()
            assertTrue("Retry must resolve the same requested page", device.wait(Until.hasObject(By.textContains("arm64 APK")), 15000))
            assertFalse(device.hasObject(By.text("Retry")))
            assertEquals("http://10.0.2.2:8765/retry/v1", server.getString("endpoint", null))
            assertEquals(url, prefs.getString("url", null))
        }
    }

    @Test fun downloadFailureOffersExternalSourcePageWithoutRecursingIntoCompanion() {
        val store = Downloads(context); store.cancel()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit().putString("endpoint", "https://saved.example/v1").commit()
        val url = "https://www.apkmirror.com/apk/example/selected-1-android-apk-download/"
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear()
            .putString("url", url + "download/?key=expired&forcebaseapk=true").commit()
        store.fail("Download failed (Android reason 403).")
        var chooser: Intent? = null
        val launched = CountDownLatch(1)
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_CHOOSER) return null
                chooser = intent; launched.countDown()
                return Instrumentation.ActivityResult(0, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
                assertNotNull(device.wait(Until.findObject(By.text("Retry")), 15000))
                assertNull(device.findObject(By.clazz("android.widget.EditText")))
                scenario.recreate()
                assertNotNull(device.wait(Until.findObject(By.text("Open in Browser")), 15000))
                scenario.onActivity { activity ->
                    val view = activity.window.decorView
                    val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                    view.draw(android.graphics.Canvas(image))
                    java.io.File(context.getExternalFilesDir(null), "failure-actions.png").outputStream().use {
                        image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                    image.recycle()
                }
                device.findObject(By.text("Open in Browser")).click()
                assertTrue(launched.await(15, TimeUnit.SECONDS))
                val view = chooser!!.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
                assertEquals(Intent.ACTION_VIEW, view.action)
                assertEquals(url, view.dataString)
                val excluded = chooser!!.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS)!!
                assertTrue(excluded.contains(ComponentName(context, MainActivity::class.java)))
                assertEquals("https://saved.example/v1", context.getSharedPreferences("byparr", Context.MODE_PRIVATE).getString("endpoint", null))
            }
        } finally { instrumentation.removeMonitor(monitor); store.cancel() }
    }

    @Test fun failedNewPageDoesNotOfferThePreviousCompletedApk() {
        val store = Downloads(context); store.cancel()
        val oldFile = java.io.File(context.filesDir, "previous-ready-fixture.apk").apply { writeText("UI fixture") }
        context.getSharedPreferences("download", Context.MODE_PRIVATE).edit()
            .putBoolean("ready", true).putString("path", oldFile.absolutePath).putBoolean("opened", true).commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit().putString("endpoint", "https://saved.example/v1").commit()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear()
            .putString("url", "https://www.apkmirror.com/apk/example/selected-1-android-apk-download/")
            .putString("pageError", "Attachment request failed (HTTP 403).").commit()
        try {
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
                assertNotNull(device.wait(Until.findObject(By.text("Retry")), 15000))
                assertFalse("The old APK must not be offered for a failed new request", device.hasObject(By.text("Open in Morphe")))
                assertFalse(device.hasObject(By.text("Share file")))
            }
        } finally { store.cancel(); oldFile.delete() }
    }
}
