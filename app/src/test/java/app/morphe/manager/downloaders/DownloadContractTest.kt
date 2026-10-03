package app.morphe.manager.downloaders

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadContractTest {
    @Test fun acceptsOnlyApkMirrorSitesAndUpgradesHttp() {
        val policy = ApkMirrorPolicy(false)
        assertEquals("https://www.apkmirror.com/apk/example/", policy.pageUrl("http://www.apkmirror.com/apk/example/"))
        assertEquals("https://www.apkmirror.com/a%2Fb?key=x%2By%26z#v%2F1",
            policy.pageUrl("http://www.apkmirror.com:80/a%2Fb?key=x%2By%26z#v%2F1"))
        assertNotNull(policy.pageUrl("https://apkmirror.com/"))
        assertNotNull(policy.pageUrl("https://www.apkmirror.com/?s=com.google.android.youtube"))
        for (url in listOf("https://apkmirror.com.evil.test/", "https://evilapkmirror.com/",
            "https://user:pass@apkmirror.com/", "javascript:alert(1)", "file:///data/local/file.apk",
            "https://10.0.2.2/file", "http://10.0.2.2/page", "https://apkmirror.com:444/")) {
            assertNull(url, policy.pageUrl(url))
        }
        assertFalse(policy.downloadUrl("http://www.apkmirror.com/file.apk"))
        assertTrue(policy.downloadUrl("https://downloadr2.apkmirror.com/file.apk"))
    }
    @Test fun loopbackFixturesAreDebugOnly() {
        assertNull(ApkMirrorPolicy(false).pageUrl("http://10.0.2.2:8765/release"))
        assertNotNull(ApkMirrorPolicy(true).pageUrl("http://10.0.2.2:8765/release"))
        assertFalse(ApkMirrorPolicy(true).downloadUrl("http://192.168.1.1/file.apk"))
    }
    @Test fun detectsOriginalArchivesAndRejectsHtmlAndIncompleteBundles() {
        fun archive(vararg names: String): File {
            val file = File.createTempFile("morphe-contract", ".bin")
            ZipOutputStream(file.outputStream()).use { zip ->
                names.forEach { zip.putNextEntry(ZipEntry(it)); zip.write(byteArrayOf(1, 2, 3)); zip.closeEntry() }
            }
            return file
        }
        val apk = archive("AndroidManifest.xml", "classes.dex")
        val apkm = archive("info.json", "base.apk", "split_config.arm64_v8a.apk")
        val broken = archive("split_config.arm64_v8a.apk")
        val html = File.createTempFile("morphe-html", ".apk").apply { writeText("<html>Verification needed</html>") }
        try {
            assertEquals("apk", ArchiveFormat.detect(apk).extension)
            assertEquals("apkm", ArchiveFormat.detect(apkm).extension)
            for (file in listOf(broken, html)) {
                assertThrows(java.io.IOException::class.java) { ArchiveFormat.detect(file) }
            }
        } finally { listOf(apk, apkm, broken, html).forEach { it.delete() } }
    }
}
