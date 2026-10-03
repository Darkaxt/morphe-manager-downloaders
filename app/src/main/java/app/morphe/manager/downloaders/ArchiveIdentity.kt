package app.morphe.manager.downloaders

import android.content.Context
import android.os.Build
import java.io.File
import java.util.zip.ZipFile

/** Optional display details from the original APK's Android manifest and resources. */
data class ArchiveIdentity(val label: String, val packageName: String, val versionName: String?, val versionCode: Long) {
    companion object {
        fun read(context: Context, file: File, format: ArchiveFormat): ArchiveIdentity? = try {
            if (format == ArchiveFormat.APK) readApk(context, file)
            else ZipFile(file).use { zip ->
                // PackageManager rejects standalone split APKs. Prefer conventional base names,
                // then inspect XAPK candidates until the actual base manifest is found.
                val candidates = zip.entries().asSequence().filter {
                    !it.isDirectory && if (format == ArchiveFormat.APKM) it.name == "base.apk"
                    else it.name.endsWith(".apk", ignoreCase = true)
                }.sortedBy { if (it.name.substringAfterLast('/') == "base.apk") 0 else 1 }
                candidates.firstNotNullOfOrNull { entry ->
                    // Never extract to an entry-supplied path. Only one temporary APK at a time.
                    val base = File.createTempFile("archive-identity-", ".apk", context.cacheDir)
                    try {
                        zip.getInputStream(entry).use { input -> base.outputStream().use { input.copyTo(it) } }
                        readApk(context, base)
                    } finally { base.delete() }
                }
            }
        } catch (_: Exception) { null }

        @Suppress("DEPRECATION")
        private fun readApk(context: Context, file: File): ArchiveIdentity? {
            val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
            val app = info.applicationInfo ?: return null
            app.sourceDir = file.absolutePath
            app.publicSourceDir = file.absolutePath
            val label = try { app.loadLabel(context.packageManager).toString().trim() }
                catch (_: Exception) { "" }
            return ArchiveIdentity(label.ifBlank { info.packageName }, info.packageName,
                info.versionName?.trim()?.takeIf { it.isNotEmpty() },
                if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
        }
    }
}
