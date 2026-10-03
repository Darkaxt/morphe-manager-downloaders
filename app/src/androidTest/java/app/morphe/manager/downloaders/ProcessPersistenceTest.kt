package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL

/** Also run these methods in separate instrumentation processes, with a force-stop between. */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ProcessPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun boundary(path: String) {
        val connection = URL("http://10.0.2.2:8765/$path").openConnection() as HttpURLConnection
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        try { assertEquals(200, connection.responseCode); connection.inputStream.close() }
        finally { connection.disconnect() }
    }
    @Test fun aPreparePendingDownload() {
        val store = Downloads(context)
        store.cancel()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        boundary("slow-reset")
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
                store.enqueue("http://10.0.2.2:8765/slow.apk", "Android fixture", null,
                    ArchiveFormat.APK.mime, "http://10.0.2.2:8765/release/")
                store.markOpened()
                boundary("slow-started")
                assertNull(store.error)
                assertFalse(store.ready)
                assertTrue(store.id >= 0)
        }
    }
    @Test fun bRestoreCompletedDownload() {
        boundary("slow-release")
        val prefs = context.getSharedPreferences("download", Context.MODE_PRIVATE)
        val completed = CountDownLatch(1)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "ready" || key == "error") completed.countDown()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        try { ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            val restored = Downloads(context)
            if (!restored.ready) assertTrue(completed.await(20, TimeUnit.SECONDS))
            assertTrue(restored.ready)
            assertNull(restored.error)
            assertEquals(ArchiveFormat.APK, ArchiveFormat.detect(restored.file!!))
            assertEquals("slow.apk", restored.displayName)
            assertTrue(restored.autoOpened)
            assertNotNull(restored.query())
        } } finally { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
