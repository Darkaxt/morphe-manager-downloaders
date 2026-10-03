package app.morphe.manager.downloaders

import android.app.DownloadManager
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
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LifecycleTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun launch(): ActivityScenario<MainActivity> {
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "http://10.0.2.2:8765/v1").commit()
        return ActivityScenario.launch(Intent(context, MainActivity::class.java))
    }
    private fun boundary(path: String) {
        val connection = URL("http://10.0.2.2:8765/$path").openConnection() as HttpURLConnection
        // Diagnostic limit at the test HTTP boundary; never used by application control flow.
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        try { assertEquals(200, connection.responseCode); connection.inputStream.close() }
        finally { connection.disconnect() }
    }

    @Test fun missingEndpointPromptsAndSavedEndpointSurvivesRecreation() {
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit().clear().commit()
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 15000)
            assertNotNull("Missing endpoint did not prompt", input)
            input.text = "https://private-server.example:8191"
            device.findObject(By.text("SAVE AND RETRY")).click()
            val prefs = context.getSharedPreferences("byparr", Context.MODE_PRIVATE)
            assertEquals("https://private-server.example:8191/v1", prefs.getString("endpoint", null))
            scenario.recreate()
            assertEquals("https://private-server.example:8191/v1", prefs.getString("endpoint", null))
            assertNull(device.findObject(By.clazz("android.widget.EditText")))
        }
    }

    @Test fun apiFailureOffersAnExplicitEndpointEditAndRetry() {
        Downloads(context).cancel()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "http://10.0.2.2:8765/error/v1").commit()
        val page = Intent(Intent.ACTION_VIEW, Uri.parse("http://10.0.2.2:8765/variants/"), context, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(page).use { scenario ->
            val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 15000)
            assertNotNull("API failure did not offer endpoint input", input)
            assertTrue(context.getSharedPreferences("browser", Context.MODE_PRIVATE)
                .getString("pageError", "").orEmpty().contains("502"))
            input.text = "http://10.0.2.2:8765/v1"
            device.findObject(By.text("SAVE AND RETRY")).click()
            assertTrue(device.wait(Until.hasObject(By.textContains("arm64 APK")), 15000))
            assertTrue(device.hasObject(By.textContains("x86 APK")))
            assertEquals(-1L, Downloads(context).id)
            scenario.recreate()
            assertTrue(device.wait(Until.hasObject(By.textContains("arm64 APK")), 15000))
            assertEquals("http://10.0.2.2:8765/v1",
                context.getSharedPreferences("byparr", Context.MODE_PRIVATE).getString("endpoint", null))
        }
    }

    @Test fun pendingDownloadSurvivesRecreationAndCanBeCancelled() {
        val store = Downloads(context)
        store.cancel()
        launch().use { scenario ->
            try {
                boundary("slow-reset")
                store.enqueue("http://10.0.2.2:8765/slow.apk", "Android fixture", null,
                    ArchiveFormat.APK.mime, "http://10.0.2.2:8765/release/")
                val downloadId = store.id
                boundary("slow-started")
                scenario.recreate()
                assertEquals(downloadId, Downloads(context).id)
                assertFalse(store.ready)
                assertNotNull(store.query())
                store.cancel()
                assertEquals(-1L, Downloads(context).id)
                assertFalse(store.ready)
                context.getSystemService(DownloadManager::class.java)
                    .query(DownloadManager.Query().setFilterById(downloadId)).use { assertFalse(it.moveToFirst()) }
            } finally { store.cancel(); boundary("slow-release") }
        }
    }

    @Test fun htmlMasqueradingAsApkIsRejectedAndFailurePersists() {
        val store = Downloads(context)
        store.cancel()
        val prefs = context.getSharedPreferences("download", Context.MODE_PRIVATE)
        val failed = CountDownLatch(1)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "error") failed.countDown()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        try {
            launch().use { scenario ->
                store.enqueue("http://10.0.2.2:8765/html.apk", "Android fixture", null,
                    ArchiveFormat.APK.mime, "http://10.0.2.2:8765/release/")
                assertTrue("Archive rejection did not finish", failed.await(20, TimeUnit.SECONDS))
                assertNotNull(store.error)
                assertFalse(store.ready)
                scenario.recreate()
                assertEquals(store.error, Downloads(context).error)
                assertFalse(Downloads(context).ready)
            }
        } finally { prefs.unregisterOnSharedPreferenceChangeListener(listener); store.cancel() }
    }

    @Test fun unresolvedChallengeIsAnErrorInsteadOfAWebViewLoop() {
        Downloads(context).cancel()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "http://10.0.2.2:8765/v1").commit()
        val page = Intent(Intent.ACTION_VIEW, Uri.parse("http://10.0.2.2:8765/challenge/"), context, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(page).use {
            assertNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 15000))
            assertTrue(context.getSharedPreferences("browser", Context.MODE_PRIVATE)
                .getString("pageError", "").orEmpty().contains("unresolved security check"))
            assertEquals(-1L, Downloads(context).id)
            assertFalse(device.hasObject(By.clazz("android.webkit.WebView")))
        }
    }
}
