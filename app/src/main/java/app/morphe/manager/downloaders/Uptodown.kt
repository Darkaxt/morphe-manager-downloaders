package app.morphe.manager.downloaders

import org.json.JSONObject
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI

/** Caller-owned integration for Byparr's generic browser scripting API. */
object Uptodown {
    private val buildPath = Regex("^/android/download/([0-9]+)(?:-x)?/?$")
    fun fileId(url: String): String? = buildPath.matchEntire(URI(url).path)?.groupValues?.get(1)
    fun scriptedId(url: String): String? = fileId(url)?.takeIf { URI(url).path.trimEnd('/').endsWith("-x") }
    fun script(): String = Uptodown::class.java.getResourceAsStream("/uptodown.js")
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: throw IOException("The Uptodown browser recipe is missing.")

    fun page(url: String, requestedUrl: String, doc: Document): RemotePage {
        val button = doc.selectFirst("#detail-download-button")
        val requested = fileId(requestedUrl)
        val name = doc.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.title().substringBefore(" for Android").substringBefore(" for ")
        if (button != null) {
            val id = button.attr("data-file-id")
            if (!id.matches(Regex("[0-9]+")) || !button.attr("data-app-id").matches(Regex("[0-9]+")) ||
                button.attr("data-download-version") != id || requested != null && requested != id)
                throw IOException("Uptodown returned different or invalid build identifiers.")
            val next = URI(url).resolve("/android/download/$id-x").toString()
            return RemotePage(url, doc.title(), next, listOf(RemotePage.Choice(next, name)), DownloadSource.UPTODOWN, name)
        }
        if (requested != null) throw IOException("Uptodown did not return the requested build's download button.")
        val choices = doc.select("a[href]").mapNotNull { anchor ->
            val link = anchor.absUrl("href")
            if (runCatching { URI(link).host == URI(url).host && fileId(link) != null }.getOrDefault(false))
                RemotePage.Choice(link, anchor.text().ifBlank { "Build ${fileId(link)}" }) else null
        }.distinctBy { it.url }
        return RemotePage(url, doc.title(), choices.singleOrNull()?.url, choices, DownloadSource.UPTODOWN, name)
    }

    /** An aborted request is not an attachment HTTP response. Validate both captures. */
    fun resolved(reply: JSONObject, page: RemotePage, target: String, agent: String): Pair<RemotePage, Map<String, String>> {
        val result = reply.optJSONObject("scriptResult")
            ?: throw IOException("Uptodown requires Byparr browser scripting support. Update your server.")
        val capture = result.optJSONObject("value")
        val resolution = result.optJSONObject("captures")?.optJSONObject("resolution")
        val id = scriptedId(target) ?: throw IOException("Uptodown requires an exact build page.")
        if (scriptedId(page.url) != id) throw IOException("Uptodown changed the requested build page.")
        val doc = org.jsoup.Jsoup.parse(reply.getJSONObject("solution").getString("response"))
        val appId = doc.selectFirst("#detail-download-button")?.attr("data-app-id")
        val expectedPost = URI(target).resolve("/ajax/app/$appId/file/$id/download-url").toString()
        val key = resolution?.optJSONObject("body")?.optJSONObject("data")?.opt("downloadURL") as? String
        val url = capture?.optString("url")
        if (result.optString("terminalCapture") != "attachment" ||
            resolution?.optString("kind") != "response" || resolution.optString("method") != "POST" ||
            resolution.optString("url") != expectedPost || resolution.opt("status") != 200 ||
            resolution.optJSONObject("body")?.opt("success") != 1 || key.isNullOrBlank() ||
            capture?.optString("kind") != "request" || capture.optString("method") != "GET" ||
            capture.opt("aborted") != true || url != "https://dw.uptodown.com/dwn/$key" ||
            !DownloadPolicy(false).downloadUrl(url, DownloadSource.UPTODOWN))
            throw IOException("Byparr did not capture a verified Uptodown download for the requested build.")
        val headers = capture.getJSONObject("requestHeaders")
        if (headers.optString("user-agent") != agent)
            throw IOException("The captured Uptodown browser user agent does not match.")
        val forwarded = listOf("accept", "referer").mapNotNull { name ->
            if (!headers.has(name)) null else name to headers.getString(name).also {
                if (it.isBlank() || it.contains('\r') || it.contains('\n')) throw IOException("Invalid captured browser header.")
                if (name == "referer") {
                    val ref = URI(it); val origin = URI(target)
                    if (ref.scheme != origin.scheme || ref.host != origin.host || ref.port != origin.port || ref.rawUserInfo != null)
                        throw IOException("The captured Uptodown referrer belongs to another origin.")
                }
            }
        }.toMap()
        return page.copy(next = url, choices = listOf(RemotePage.Choice(url, page.appName))) to forwarded
    }
}
