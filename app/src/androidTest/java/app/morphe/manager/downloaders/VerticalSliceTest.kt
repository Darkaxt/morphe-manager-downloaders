package app.morphe.manager.downloaders

import android.content.*
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    @Test fun downloadsOriginalWithSessionAndSharesReadableUri() {
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
        val page = Intent(Intent.ACTION_VIEW, Uri.parse("http://10.0.2.2:8765/release/"), context, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(page).use { scenario ->
            try {
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
                scenario.recreate()
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
