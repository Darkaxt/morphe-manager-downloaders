package app.morphe.manager.downloaders

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object MorpheHandoff {
    const val PACKAGE = "app.morphe.manager"
    fun intent(context: Context, file: File, name: String, format: ArchiveFormat, target: String? = PACKAGE): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file, name)
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (target != null) setPackage(target)
        }
    }
}
