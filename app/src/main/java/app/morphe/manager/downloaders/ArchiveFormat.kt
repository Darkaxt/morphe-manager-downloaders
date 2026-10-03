package app.morphe.manager.downloaders

import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

enum class ArchiveFormat(val extension: String, val mime: String) {
    APK("apk", "application/vnd.android.package-archive"),
    APKM("apkm", "application/x-apkm"),
    XAPK("xapk", "application/x-xapk");

    companion object {
        @Throws(IOException::class)
        fun detect(file: File): ArchiveFormat = ZipFile(file).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            if (names.any { it.startsWith('/') || '\\' in it || ".." in it.split('/') }) {
                throw IOException("The archive contains an unsafe file path.")
            }
            when {
                zip.getEntry("AndroidManifest.xml")?.let { !it.isDirectory && it.size > 0 } == true -> APK
                zip.getEntry("base.apk")?.let { !it.isDirectory && it.size > 0 } == true && "info.json" in names -> APKM
                zip.getEntry("manifest.json")?.let { !it.isDirectory && it.size > 0 } == true &&
                    names.any { it.endsWith(".apk") && zip.getEntry(it).size > 0 } -> XAPK
                else -> throw IOException("The download is not a complete APK, APKM or XAPK archive. It may be a verification page.")
            }
        }
    }
}
