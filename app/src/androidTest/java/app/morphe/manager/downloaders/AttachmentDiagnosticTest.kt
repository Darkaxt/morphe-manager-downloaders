package app.morphe.manager.downloaders

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/** Opt-in observation of the installed companion's saved session; never edits it. */
@RunWith(AndroidJUnit4::class)
class AttachmentDiagnosticTest {
    @Test fun observePhoneAttachment() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("attachmentDiagnostic") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val endpoint = context.getSharedPreferences("byparr", Context.MODE_PRIVATE).getString("endpoint", null)!!
        var url = args.getString("downloadUrl") ?: context.getSharedPreferences("browser", Context.MODE_PRIVATE).getString("url", null)!!
        val output = StringBuilder()
        fun record(line: String) { output.appendLine(line); Log.i("AttachmentDiagnostic", line) }
        val client = ByparrClient(false)
        try {
            val pages = mutableSetOf<String>()
            while (pages.add(url)) {
                val page = client.fetch(endpoint, url)
                record("page=${URI(page.content.url).host}${URI(page.content.url).path} nextHost=${page.content.next?.let { URI(it).host }} cookies=${page.cookies.map { "${it.name}@${it.domain}${it.path}" }}")
                val next = page.content.next ?: error("No unique download step")
                if (!page.content.isAttachment(next)) { url = next; continue }
                var attachmentUrl = next
                val attachments = mutableSetOf<String>()
                while (attachments.add(attachmentUrl)) {
                    check(DownloadPolicy(false).downloadUrl(attachmentUrl, page.content.source)) { "Unsupported attachment origin" }
                    val conn = URI(attachmentUrl).toURL().openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.setRequestProperty("User-Agent", page.userAgent)
                    conn.setRequestProperty("Referer", page.content.url)
                    page.cookieHeader(attachmentUrl).takeIf { it.isNotEmpty() }?.let { conn.setRequestProperty("Cookie", it) }
                    try {
                        val code = conn.responseCode
                        record("attachment=${URI(attachmentUrl).host}${URI(attachmentUrl).path} status=$code type=${conn.contentType} server=${conn.getHeaderField("Server")} cfMitigated=${conn.getHeaderField("cf-mitigated")}")
                        if (code in listOf(301,302,303,307,308)) {
                            attachmentUrl = URI(attachmentUrl).resolve(conn.getHeaderField("Location")).toString()
                            continue
                        }
                        if (code >= 400) record("errorBody=" + conn.errorStream?.bufferedReader()?.use {
                            val sample = CharArray(1200)
                            val length = it.read(sample)
                            if (length < 0) "" else String(sample, 0, length)
                        })
                        break
                    } finally { conn.disconnect() }
                }
                break
            }
        } finally {
            File(context.getExternalFilesDir(null), "attachment-diagnostic.txt").writeText(output.toString())
        }
    }
}
