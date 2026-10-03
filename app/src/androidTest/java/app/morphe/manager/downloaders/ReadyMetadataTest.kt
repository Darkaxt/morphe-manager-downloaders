package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ReadyMetadataTest {
    @Test fun legacyReadyApkLoadsIdentityAndRestoresIt() = verifyReady(ArchiveFormat.APK)
    @Test fun apkmUsesBaseApkIdentityWithoutChangingBundle() = verifyReady(ArchiveFormat.APKM)
    @Test fun xapkUsesBaseApkIdentityWithoutChangingBundle() = verifyReady(ArchiveFormat.XAPK)
    @Test fun unreadableMetadataKeepsValidatedFileAndFilenameFallback() = verifyReady(ArchiveFormat.APK, readable = false)

    // Explicit live-artifact check, run with -e showlyApk <task-owned file>.
    @Test fun showlyOriginalShowsItsOwnIdentity() {
        val path = InstrumentationRegistry.getArguments().getString("showlyApk")
        org.junit.Assume.assumeNotNull(path)
        verifyReady(ArchiveFormat.APK, original = File(path!!), label = "Showly",
            packageId = "com.michaldrabik.showly2", version = "3.72.0", build = 843)
    }

    private fun verifyReady(format: ArchiveFormat, readable: Boolean = true,
                            original: File? = null, label: String = "Morphe Downloader",
                            packageId: String = "app.morphe.manager.downloaders",
                            version: String = BuildConfig.VERSION_NAME, build: Int = BuildConfig.VERSION_CODE) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("download", Context.MODE_PRIVATE)
        Downloads(context).cancel()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "http://10.0.2.2:8765/v1").commit()
        val target = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "metadata-test.${format.extension}")
        val source = original ?: File(context.applicationInfo.sourceDir)
        if (format == ArchiveFormat.APK && readable) source.copyTo(target, overwrite = true)
        else ZipOutputStream(target.outputStream()).use { zip ->
            fun entry(name: String, content: ByteArray) {
                zip.putNextEntry(ZipEntry(name)); zip.write(content); zip.closeEntry()
            }
            when (format) {
                ArchiveFormat.APK -> entry("AndroidManifest.xml", byteArrayOf(1, 2, 3))
                ArchiveFormat.APKM -> {
                    entry("info.json", "{}".toByteArray()); entry("base.apk", source.readBytes())
                }
                ArchiveFormat.XAPK -> {
                    entry("manifest.json", "{\"package_name\":\"$packageId\"}".toByteArray())
                    entry("config.en.apk", byteArrayOf(1, 2, 3))
                    entry("$packageId.apk", source.readBytes())
                }
            }
        }
        fun digest() = MessageDigest.getInstance("SHA-256").digest(target.readBytes()).toList()
        val before = digest()
        val cacheBefore = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.absolutePath }.toSet()
        assertEquals(format, ArchiveFormat.detect(target))
        // Recreate a 0.2.0 completed record: it has no APK metadata preferences.
        prefs.edit().clear().putLong("id", 1234).putBoolean("ready", true).putBoolean("opened", true)
            .putString("path", target.absolutePath).putString("format", format.name)
            .putString("name", target.name).commit()
        try {
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
                val device = UiDevice.getInstance(instrumentation)
                val details = "Version: $version ($build)"
                if (readable) {
                    assertTrue("Archive identity was not presented", device.wait(Until.hasObject(By.textContains(details)), 15000))
                    assertTrue(device.hasObject(By.textContains("Package: $packageId")))
                    scenario.onActivity { activity ->
                        assertEquals(label, activity.findViewById<android.widget.TextView>(10004).text.toString())
                    }
                    scenario.recreate()
                    assertTrue(device.wait(Until.hasObject(By.textContains(details)), 15000))
                    if (original != null) scenario.onActivity { activity ->
                        val view = activity.window.decorView
                        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                        view.draw(android.graphics.Canvas(bitmap))
                        File(context.getExternalFilesDir(null), "metadata-showly.png").outputStream().use {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmap.recycle()
                    }
                } else {
                    assertTrue(device.wait(Until.hasObject(By.text(target.name)), 15000))
                    assertFalse(device.hasObject(By.textStartsWith("Package:")))
                }
                assertTrue(Downloads(context).ready)
                assertNull(Downloads(context).error)
                assertEquals(1234L, Downloads(context).id)
                assertEquals(target.name, Downloads(context).displayName)
                assertEquals(before, digest())
                assertTrue("Extracted base APK was left in cache", context.cacheDir.walkTopDown()
                    .filter { it.isFile && it.extension == "apk" }.all { it.absolutePath in cacheBefore })
            }
        } finally {
            Downloads(context).cancel()
            target.delete()
        }
    }
}
