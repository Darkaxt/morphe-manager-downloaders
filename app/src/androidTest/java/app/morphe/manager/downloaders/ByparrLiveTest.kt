package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat

/** Opt-in real server/download check. Private endpoint is supplied only by instrumentation arguments. */
@RunWith(AndroidJUnit4::class)
class ByparrLiveTest {
    @Test fun downloadsRealOriginalThroughConfiguredServer() {
        val args = InstrumentationRegistry.getArguments()
        val endpoint = args.getString("byparrEndpoint")
        val url = args.getString("downloadUrl")
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
                args.getString("expectedFormat")?.let { assertEquals(ArchiveFormat.valueOf(it), store.format) }
                val file = store.file!!
                args.getString("expectedPackage")?.let { expected ->
                    val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                    assertNotNull("Downloaded APK metadata must be readable", info)
                    assertEquals(expected, info!!.packageName)
                    args.getString("expectedVersion")?.let { assertEquals(it, info.versionName) }
                }
                fun digest(bytes: ByteArray, algorithm: String) = MessageDigest.getInstance(algorithm)
                    .digest(bytes).joinToString("") { "%02x".format(it) }
                val sha256 = digest(file.readBytes(), "SHA-256")
                val baseMd5 = if (store.format == ArchiveFormat.APK) digest(file.readBytes(), "MD5") else
                    ZipFile(file).use { zip -> digest(zip.getInputStream(zip.getEntry("base.apk")).use { stream -> stream.readBytes() }, "MD5") }
                args.getString("expectedBaseMd5")?.let { expected -> assertEquals(expected, baseMd5) }
                args.getString("expectedMd5")?.let { expected -> assertEquals(expected, baseMd5) }
                val acknowledged = CountDownLatch(1)
                var received: Intent? = null
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) { received = intent; acknowledged.countDown() }
                }
                ContextCompat.registerReceiver(context, receiver, IntentFilter(HandoffReceiverActivity.RESULT), ContextCompat.RECEIVER_EXPORTED)
                try {
                    val testPackage = InstrumentationRegistry.getInstrumentation().context.packageName
                    context.startActivity(MorpheHandoff.intent(context, file, store.displayName, store.format)
                        .setComponent(ComponentName(testPackage, HandoffReceiverActivity::class.java.name))
                        .setPackage(testPackage).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    assertTrue("Original URI was not read by receiver", acknowledged.await(15, TimeUnit.SECONDS))
                    assertNull(received!!.getStringExtra("error"))
                    assertEquals(sha256, received!!.getStringExtra("sha256"))
                    assertEquals(store.displayName, received!!.getStringExtra("name"))
                } finally { context.unregisterReceiver(receiver) }
                File(context.getExternalFilesDir(null), "byparr-live.txt").writeText(
                    "url=$url\nname=${store.displayName}\nformat=${store.format}\nbytes=${file.length()}\nsha256=$sha256\nbaseApkMd5=$baseMd5\n")
            }
        } finally {
            downloadPrefs.unregisterOnSharedPreferenceChangeListener(downloadListener)
            pagePrefs.unregisterOnSharedPreferenceChangeListener(pageListener)
        }
    }
}
