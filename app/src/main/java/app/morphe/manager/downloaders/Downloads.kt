package app.morphe.manager.downloaders

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import java.io.File
import java.io.IOException
import java.util.UUID

/** One persisted system download. Android owns transfers even while this app is closed. */
class Downloads(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("download", Context.MODE_PRIVATE)
    private val manager = app.getSystemService(DownloadManager::class.java)
    val id: Long get() = prefs.getLong("id", -1)
    val ready: Boolean get() = prefs.getBoolean("ready", false)
    val error: String? get() = prefs.getString("error", null)
    val displayName: String get() = prefs.getString("name", "download.apk")!!
    val format: ArchiveFormat get() = ArchiveFormat.valueOf(prefs.getString("format", "APK")!!)
    val file: File? get() = prefs.getString("path", null)?.let(::File)
    val autoOpened: Boolean get() = prefs.getBoolean("opened", false)
    fun markOpened() { prefs.edit().putBoolean("opened", true).commit() }

    fun enqueue(url: String, userAgent: String, disposition: String?, mime: String?, referer: String,
                cookieHeader: String? = null, sendReferer: Boolean = true) = synchronized(stateLock) {
        check(id < 0 || ready || error != null) { "Finish or cancel the current download first." }
        val guessed = URLUtil.guessFileName(url, disposition, mime)
        val name = guessed.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._() -]"), "_").take(180)
            .ifBlank { "download.apk" }
        val target = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${UUID.randomUUID()}-$name")
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(name).setDescription("${app.getString(R.string.app_name)} · ${DownloadPolicy(false).source(referer)?.label.orEmpty()}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(target))
            .addRequestHeader("User-Agent", userAgent)
        if (sendReferer) request.addRequestHeader("Referer", referer)
        cookieHeader?.takeIf { it.isNotBlank() }?.let {
            request.addRequestHeader("Cookie", it)
        }
        val downloadId = manager.enqueue(request)
        prefs.edit().clear().putLong("id", downloadId).putString("path", target.absolutePath)
            .putString("name", name).commit()
    }

    data class State(val status: Int, val downloaded: Long, val total: Long, val reason: Int, val localUri: String?)
    fun query(downloadId: Long = id): State? {
        if (downloadId < 0) return null
        manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
            return State(number(DownloadManager.COLUMN_STATUS).toInt(),
                number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES), number(DownloadManager.COLUMN_REASON).toInt(),
                cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)))
        }
    }

    /** Called off the UI thread after the system reports a completed transfer. */
    fun validate() {
        val snapshot = synchronized(stateLock) {
            if (id < 0 || ready || error != null) return
            id to displayName
        }
        try {
            val completed = query(snapshot.first)
            if (completed?.status != DownloadManager.STATUS_SUCCESSFUL)
                throw IOException("The system download is not complete.")
            val uri = completed.localUri?.let(Uri::parse)
                ?: throw IOException("The system download has no local file.")
            if (uri.scheme != "file" || uri.path == null)
                throw IOException("The system download has an unsupported local URI.")
            // The provider may rewrite the requested filename. Its completed URI is authoritative.
            val source = File(uri.path!!).canonicalFile
            val directory = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.canonicalFile
            if (directory == null || source.parentFile != directory)
                throw IOException("The system download is outside the companion's download folder.")
            val detected = ArchiveFormat.detect(source)
            val properName = snapshot.second.substringBeforeLast('.', snapshot.second) + "." + detected.extension
            synchronized(stateLock) {
                if (id == snapshot.first && error == null && !ready) {
                    prefs.edit().putBoolean("ready", true).putString("path", source.absolutePath).putString("format", detected.name)
                        .putString("name", properName).commit()
                }
            }
        } catch (e: Exception) {
            synchronized(stateLock) {
                if (id == snapshot.first && !ready) fail(e.message ?: "The file could not be validated.")
            }
        }
    }
    fun fail(message: String) = synchronized(stateLock) {
        prefs.edit().putBoolean("ready", false).putString("error", message).commit()
        // Only failed transfers are expendable. Successful originals are retained.
        if (id >= 0) manager.remove(id)
    }
    fun cancel() = synchronized(stateLock) {
        if (id >= 0 && !ready) manager.remove(id)
        prefs.edit().clear().commit()
    }
    companion object { private val stateLock = Any() }
}
