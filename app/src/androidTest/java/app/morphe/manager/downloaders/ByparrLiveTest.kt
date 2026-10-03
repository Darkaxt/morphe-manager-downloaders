package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/** Opt-in real server/download check. Private endpoint is supplied only by instrumentation arguments. */
@RunWith(AndroidJUnit4::class)
class ByparrLiveTest {
    @Test fun downloadsRealOriginalThroughConfiguredServer() {
        val args = InstrumentationRegistry.getArguments()
        val endpoint = args.getString("byparrEndpoint")
        val url = args.getString("apkMirrorUrl")
        assumeTrue(endpoint != null && url != null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = Downloads(context)
        store.cancel()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit().putString("endpoint", endpoint).commit()
        val pagePrefs = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        pagePrefs.edit().clear().commit()
        val downloadPrefs = context.getSharedPreferences("download", Context.MODE_PRIVATE)
        val completed = CountDownLatch(1)
        val downloadListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "id" && store.id >= 0) store.markOpened()
            if (store.ready || store.error != null) completed.countDown()
        }
        val pageListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "pageError" && pagePrefs.getString(key, null) != null) completed.countDown()
        }
        downloadPrefs.registerOnSharedPreferenceChangeListener(downloadListener)
        pagePrefs.registerOnSharedPreferenceChangeListener(pageListener)
        try {
            ActivityScenario.launch<MainActivity>(Intent(Intent.ACTION_VIEW, Uri.parse(url), context, MainActivity::class.java)).use {
                // Diagnostic limit belongs to this opt-in test, never application control flow.
                assertTrue("Real download did not complete: ${store.query()}", completed.await(240, TimeUnit.SECONDS))
                assertNull(pagePrefs.getString("pageError", null))
                assertNull(store.error)
                assertTrue(store.ready)
                assertEquals(ArchiveFormat.APKM, store.format)
                val file = store.file!!
                fun digest(bytes: ByteArray, algorithm: String) = MessageDigest.getInstance(algorithm)
                    .digest(bytes).joinToString("") { "%02x".format(it) }
                val sha256 = digest(file.readBytes(), "SHA-256")
                val baseMd5 = ZipFile(file).use { zip ->
                    digest(zip.getInputStream(zip.getEntry("base.apk")).use { stream -> stream.readBytes() }, "MD5")
                }
                args.getString("expectedBaseMd5")?.let { expected -> assertEquals(expected, baseMd5) }
                File(context.getExternalFilesDir(null), "byparr-live.txt").writeText(
                    "url=$url\nname=${store.displayName}\nformat=${store.format}\nbytes=${file.length()}\nsha256=$sha256\nbaseApkMd5=$baseMd5\n")
            }
        } finally {
            downloadPrefs.unregisterOnSharedPreferenceChangeListener(downloadListener)
            pagePrefs.unregisterOnSharedPreferenceChangeListener(pageListener)
        }
    }
}
