package app.morphe.manager.downloaders

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.json.JSONTokener
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
    private fun launch(): ActivityScenario<MainActivity> {
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        return ActivityScenario.launch(Intent(context, MainActivity::class.java))
    }
    private fun boundary(path: String) {
        val connection = URL("http://10.0.2.2:8765/$path").openConnection() as HttpURLConnection
        // Diagnostic limit at the test HTTP boundary; never used by application control flow.
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        try { assertEquals(200, connection.responseCode); connection.inputStream.close() }
        finally { connection.disconnect() }
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

    @Test fun ambiguousVariantsAndVerificationRemainManual() {
        launch().use { scenario ->
            val cases = listOf(
                "<div id='challenge-stage'>Verify</div>" to true,
                "<a href='/release/arm64-android-apk-download/'>arm64</a>" +
                    "<a href='/release/x86-android-apk-download/'>x86</a>" to false
            )
            for ((html, challenge) in cases) {
                val inspected = CountDownLatch(1)
                var result: JSONObject? = null
                lateinit var web: WebView
                scenario.onActivity { activity ->
                    web = WebView(activity)
                    web.settings.javaScriptEnabled = true
                    web.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            val script = activity.assets.open("apkmirror.js").bufferedReader().use { it.readText() }
                            view.evaluateJavascript(script) {
                                result = JSONObject(JSONTokener(it).nextValue() as String)
                                inspected.countDown()
                            }
                        }
                    }
                    web.loadDataWithBaseURL("https://www.apkmirror.com/release/", html, "text/html", "UTF-8", null)
                }
                try {
                    assertTrue("Page inspection did not finish", inspected.await(15, TimeUnit.SECONDS))
                    assertEquals(challenge, result!!.getBoolean("challenge"))
                    assertTrue(result!!.isNull("next"))
                    if (!challenge) assertEquals(2, result!!.getJSONArray("variants").length())
                } finally { scenario.onActivity { web.destroy() } }
            }
        }
    }
}
