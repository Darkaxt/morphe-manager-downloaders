package app.morphe.manager.downloaders

import java.net.URI

enum class DownloadSource(val label: String) { APK_MIRROR("APKMirror"), APK_PURE("APKPure"), APK_COMBO("APKCombo") }

class DownloadPolicy(private val debugFixtures: Boolean) {
    private fun parse(value: String): URI? = runCatching { URI(value) }.getOrNull()
    private fun mirror(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return (host == "apkmirror.com" || host.endsWith(".apkmirror.com")) &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 443, 80)
    }
    private fun fixture(uri: URI): Boolean = debugFixtures && uri.scheme == "http" &&
        uri.host in listOf("10.0.2.2", "127.0.0.1", "localhost") && uri.rawUserInfo == null

    fun source(value: String): DownloadSource? {
        val uri = parse(value) ?: return null
        val host = uri.host?.lowercase() ?: return null
        return when {
            fixture(uri) || host == "apkmirror.com" || host.endsWith(".apkmirror.com") -> DownloadSource.APK_MIRROR
            host == "apkpure.com" || host.endsWith(".apkpure.com") ||
                host == "apkpure.net" || host.endsWith(".apkpure.net") -> DownloadSource.APK_PURE
            host == "apkcombo.com" || host.endsWith(".apkcombo.com") -> DownloadSource.APK_COMBO
            else -> null
        }
    }

    fun pageUrl(value: String): String? {
        val uri = parse(value) ?: return null
        if (fixture(uri)) return value
        if (source(value) == null || uri.rawUserInfo != null || uri.port !in listOf(-1, 443, 80)) return null
        return when (uri.scheme?.lowercase()) {
            "https" -> if (uri.port != 80) value else null
            // Decoded URI components would change escaped separators and signed query values.
            "http" -> "https://${uri.host}${uri.rawPath.orEmpty()}" +
                uri.rawQuery?.let { "?$it" }.orEmpty() + uri.rawFragment?.let { "#$it" }.orEmpty()
            else -> null
        }
    }
    fun downloadUrl(value: String, source: DownloadSource? = null): Boolean {
        val uri = parse(value) ?: return false
        val attachmentBucket = uri.host == "eb5e7388c3df147b74dd2379b7cf8323.r2.cloudflarestorage.com" &&
            uri.rawPath.orEmpty().startsWith("/downloadprod/wp-content/uploads/") &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 443)
        val pureAttachment = uri.host in listOf("d.apkpure.com", "d.apkpure.net") &&
            (uri.path.startsWith("/b/APK/") || uri.path.startsWith("/b/XAPK/")) ||
            uri.host == "data.winudf.com" && (uri.path.startsWith("/APK/") || uri.path.startsWith("/XAPK/"))
        val comboAttachment = uri.host == "apkcombo.com" && uri.path == "/d" ||
            uri.host == "download.pureapk.com" && (uri.path.startsWith("/b/APK/") || uri.path.startsWith("/b/XAPK/")) ||
            uri.host == "data.winudf.com" && (uri.path.startsWith("/APK/") || uri.path.startsWith("/XAPK/"))
        if (fixture(uri)) return source == null || source == DownloadSource.APK_MIRROR
        if (uri.scheme != "https" || uri.rawUserInfo != null || uri.port !in listOf(-1, 443)) return false
        return when (source) {
            DownloadSource.APK_MIRROR -> mirror(uri) || attachmentBucket
            DownloadSource.APK_PURE -> pureAttachment
            DownloadSource.APK_COMBO -> comboAttachment
            null -> mirror(uri) || attachmentBucket || pureAttachment || comboAttachment
        }
    }
}
