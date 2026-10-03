@file:Suppress("Unused")

package app.revanced.manager.downloaders.apkpure

import android.net.Uri
import app.revanced.manager.downloader.webview.WebViewDownloader
import app.revanced.manager.downloaders.R

// Port of brosssh's provider to the consolidated ReVanced API, 2026-10-03.
val apkPureDownloader = WebViewDownloader(R.string.apkpure) { packageName, version ->
    Uri.Builder().scheme("https").authority("apkpure.net")
        .path(if (version == null) "$packageName/$packageName"
              else "$packageName/$packageName/download/$version")
        .build().toString()
}
