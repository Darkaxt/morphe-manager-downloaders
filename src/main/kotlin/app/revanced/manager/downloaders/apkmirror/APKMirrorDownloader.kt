@file:Suppress("Unused")

package app.revanced.manager.downloaders.apkmirror

import android.net.Uri
import app.revanced.manager.downloader.webview.WebViewDownloader
import app.revanced.manager.downloaders.R

// Preserve brosssh's original-file behavior; the receiving manager owns merging.
val ApkMirrorDownloader = WebViewDownloader(R.string.apkmirror) { packageName, version ->
    Uri.Builder()
        .scheme("https")
        .authority("www.apkmirror.com")
        .appendQueryParameter("post_type", "app_release")
        .appendQueryParameter("searchtype", "apk")
        .appendQueryParameter("s", version?.let { "$packageName $it" } ?: packageName)
        .appendQueryParameter("bundles%5B%5D" /* bundles[] */, "apk_files")
        .toString()
}
