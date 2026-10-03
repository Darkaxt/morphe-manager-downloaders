package app.morphe.manager.downloaders

import android.content.*
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.net.URL
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File

@RunWith(AndroidJUnit4::class)
class VerticalSliceTest {
    @Test fun downloadsOriginalWithSessionAndSharesReadableUri() = verifyOriginal("/release/", false)

    @Test fun selectedAttachmentDownloadsWithItsResolvedSessionAndSharesReadableUri() =
        verifyOriginal("/attachment-choices/", true)

    private fun verifyOriginal(path: String, selectAttachment: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = Downloads(context)
        store.cancel()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "http://10.0.2.2:8765/v1").commit()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        val prefs = context.getSharedPreferences("download", Context.MODE_PRIVATE)
        val downloaded = CountDownLatch(1)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // Keep this URI-contract test in the companion; the receiving test APK is opened explicitly.
            if (key == "id" && store.id >= 0) {
                store.markOpened()
                // A provider may rewrite the requested destination (observed on Samsung).
                // Validation must use the completed download's reported URI, not this hint.
                prefs.edit().putString("path", File(context.getExternalFilesDir(null), "requested-but-not-written.apk").absolutePath).commit()
            }
            if (key == "ready" || key == "error") downloaded.countDown()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        val page = Intent(Intent.ACTION_VIEW, Uri.parse("http://10.0.2.2:8765$path"), context, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(page).use { scenario ->
            try {
                if (selectAttachment) {
                    val second = UiDevice.getInstance(instrumentation).findObject(UiSelector().text("Second APK"))
                    assertTrue("Attachment choices were not presented", second.waitForExists(15000))
                    assertEquals("Ambiguous attachments must not download before selection", -1L, store.id)
                    second.click()
                }
                assertTrue("Download did not reach completion", downloaded.await(45, TimeUnit.SECONDS))
                assertNull(store.error)
                assertTrue(store.ready)
                context.getSystemService(android.app.DownloadManager::class.java)
                    .query(android.app.DownloadManager.Query().setFilterById(store.id)).use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        val actual = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LOCAL_URI)))
                        assertEquals(File(actual.path!!).canonicalFile, store.file!!.canonicalFile)
                    }
                assertEquals(ArchiveFormat.APK, store.format)
                assertEquals("fixture.apk", store.displayName)
                val completedId = store.id
                val device = UiDevice.getInstance(instrumentation)
                assertTrue("Completed download did not reach its ready presentation",
                    device.wait(Until.hasObject(By.textContains("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")), 15000))
                assertTrue(device.hasObject(By.text("Morphe Downloader")))
                assertTrue(device.hasObject(By.textContains("Package: app.morphe.manager.downloaders")))
                scenario.recreate()
                assertTrue("Recreation reopened the incoming link instead of restoring the ready result",
                    device.wait(Until.hasObject(By.textContains("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")), 15000))
                assertEquals(completedId, Downloads(context).id)
                assertTrue(Downloads(context).ready)
                val original = store.file!!.readBytes()
                assertTrue(original.size > 1000)
                val expectedHash = MessageDigest.getInstance("SHA-256").digest(original).joinToString("") { "%02x".format(it) }
                val sourceHash = URL("http://10.0.2.2:8765/expected-hash").openConnection().apply {
                    connectTimeout = 15000; readTimeout = 15000
                }.getInputStream().bufferedReader().use { it.readText() }
                assertEquals("Original download bytes changed", sourceHash, expectedHash)
                scenario.onActivity { activity ->
                    val view = activity.window.decorView
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    File(activity.getExternalFilesDir(null), "verification-ready.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
                val acknowledgment = CountDownLatch(1)
                var received: Intent? = null
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) { received = intent; acknowledgment.countDown() }
                }
                ContextCompat.registerReceiver(context, receiver, IntentFilter(HandoffReceiverActivity.RESULT), ContextCompat.RECEIVER_EXPORTED)
                try {
                    val outgoing = MorpheHandoff.intent(context, store.file!!, store.displayName, store.format)
                    assertEquals(MorpheHandoff.PACKAGE, outgoing.`package`)
                    outgoing.setComponent(ComponentName(instrumentation.context.packageName, HandoffReceiverActivity::class.java.name))
                        .setPackage(instrumentation.context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(outgoing)
                    assertTrue("Receiving app did not acknowledge reading the URI", acknowledgment.await(15, TimeUnit.SECONDS))
                    assertNull(received!!.getStringExtra("error"))
                    assertEquals(expectedHash, received!!.getStringExtra("sha256"))
                    assertEquals(original.size, received!!.getIntExtra("size", -1))
                    assertEquals("fixture.apk", received!!.getStringExtra("name"))
                    assertEquals(ArchiveFormat.APK.mime, received!!.getStringExtra("mime"))
                } finally { context.unregisterReceiver(receiver) }
                val retained = store.file!!
                assertEquals("Recreation must not restart the completed download", completedId, store.id)
                store.cancel()
                assertTrue("Successful originals must survive clearing the active download", retained.isFile)
            } finally { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }
}
