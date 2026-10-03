package app.morphe.manager.downloaders

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AdditionalSourcesTest {
    @Test fun apkComboKeepsExplicitVersionsAndVariants() {
        val html = "<title>Download Showly APK</title><h1>Showly APK - Latest Version</h1>" +
            "<a class='variant' rel='nofollow noreferrer' href='https://apkcombo.com/d?u=encoded'>Showly 3.72.0 (843) APK arm64</a>" +
            "<a class='variant' href='https://apkcombo.com/d?u=other'>Showly 3.72.0 (843) APK x86</a>"
        val page = RemotePage.parse("https://apkcombo.com/showly/com.michaldrabik.showly2/download/phone-3.72.0-apk", html)
        assertNull(page.next)
        assertEquals(2, page.choices.size)
        assertTrue(page.isAttachment(page.choices.first().url))
        assertFalse(page.choices.first().sendReferer)
        assertTrue(page.choices.last().sendReferer)
        assertThrows(IOException::class.java) {
            RemotePage.parse("https://apkcombo.com/showly/com.michaldrabik.showly2/download/phone-3.70.0-apk", html)
        }
    }
    @Test fun attachmentHostsAreRestrictedToTheirOwnSource() {
        val policy = DownloadPolicy(false)
        assertNotNull(policy.pageUrl("https://apkpure.net/showly/com.michaldrabik.showly2"))
        assertTrue(policy.downloadUrl("https://data.winudf.com/APK/id", DownloadSource.APK_PURE))
        assertFalse(policy.downloadUrl("https://data.winudf.com/APK/id", DownloadSource.APK_MIRROR))
        assertFalse(policy.downloadUrl("https://data.winudf.com.evil.example/APK/id", DownloadSource.APK_PURE))
        assertFalse(policy.downloadUrl("https://data.winudf.com/other/id", DownloadSource.APK_PURE))
        assertFalse(policy.downloadUrl("https://www.apkmirror.com/download.php?id=1", DownloadSource.APK_PURE))
        assertTrue(policy.downloadUrl("https://apkcombo.com/d?u=file", DownloadSource.APK_COMBO))
        assertTrue(policy.downloadUrl("https://download.pureapk.com/b/APK/file", DownloadSource.APK_COMBO))
        assertFalse(policy.downloadUrl("https://download.pureapk.com/b/APK/file", DownloadSource.APK_PURE))
        assertFalse(policy.downloadUrl("https://apkcombo.com/other?u=file", DownloadSource.APK_COMBO))
        assertFalse(policy.downloadUrl("https://apkcombo.com.evil.test/d?u=file", DownloadSource.APK_COMBO))
    }
    @Test fun apkPureUsesTheRequestedBuildAndIgnoresStoreAppAdverts() {
        val page = RemotePage.parse("https://apkpure.com/showly/com.michaldrabik.showly2/download/3.72.0",
            "<title>Download Showly 3.72.0 Android APK File</title>" +
            "<a class='download-start-btn' href='https://d.apkpure.com/b/APK/com.michaldrabik.showly2?versionCode=843'>Download APK</a>" +
            "<a id='download_link' href='https://d.apkpure.com/b/APK/com.michaldrabik.showly2?versionCode=843'>Click here</a>" +
            "<a class='dl-direct-download-btn' href='https://d.apkpure.com/custom/com.apkpure.aegon.apk'>Store app</a>")
        assertEquals("https://d.apkpure.com/b/APK/com.michaldrabik.showly2?versionCode=843", page.next)
        assertTrue(page.isAttachment(page.next!!))
        assertThrows(IOException::class.java) {
            RemotePage.parse("https://apkpure.com/showly/com.michaldrabik.showly2/download/3.70.0",
                "<title>Download Showly 3.72.0 Android APK File</title><a id='download_link' href='https://d.apkpure.com/b/APK/com.michaldrabik.showly2?versionCode=843'>Download</a>")
        }
    }
}
