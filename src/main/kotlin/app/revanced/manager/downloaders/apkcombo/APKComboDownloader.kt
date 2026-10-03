@file:Suppress("Unused")

package app.revanced.manager.downloaders.apkcombo

import android.net.Uri
import app.revanced.manager.downloader.webview.WebViewDownloader
import app.revanced.manager.downloaders.R

// Port of brosssh's provider to the consolidated ReVanced API, 2026-10-03.
val apkComboDownloader = WebViewDownloader(R.string.apkcombo) { packageName, version ->
    Uri.Builder().scheme("https").authority("apkcombo.com")
        .path(if (version == null) "${packageName.substringAfterLast('.')}/$packageName/"
              else "${packageName.substringAfterLast('.')}/$packageName/download/phone-$version-apk")
        .build().toString()
}
