package app.morphe.manager.downloaders

import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

object ByparrEndpoint {
    fun normalize(value: String, debugFixtures: Boolean): String? = runCatching {
        val uri = URI(value.trim())
        val loopback = debugFixtures && uri.scheme == "http" && uri.host in listOf("10.0.2.2", "localhost", "127.0.0.1")
        require((uri.scheme == "https" || loopback) && uri.host != null && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.port in -1..65535 && uri.port != 0)
        val path = uri.rawPath.orEmpty().trimEnd('/')
        uri.toString().trimEnd('/') + if (path.endsWith("/v1")) "" else "/v1"
    }.getOrNull()
}

data class RemoteCookie(val name: String, val value: String, val domain: String,
                        val path: String, val secure: Boolean, val expires: Double) {
    fun matches(url: String): Boolean = runCatching {
        val uri = URI(url)
        val host = uri.host.lowercase()
        val scope = domain.removePrefix(".").lowercase()
        val requestPath = uri.path.ifEmpty { "/" }
        val cookiePath = path.ifEmpty { "/" }
        (host == scope || (domain.startsWith('.') && host.endsWith(".$scope"))) &&
            (requestPath == cookiePath || (requestPath.startsWith(cookiePath) &&
                (cookiePath.endsWith('/') || requestPath.getOrNull(cookiePath.length) == '/'))) &&
            (!secure || uri.scheme == "https") && (expires <= 0 || expires > System.currentTimeMillis() / 1000.0) &&
            name.matches(Regex("[A-Za-z0-9_!#$%&'*+.^`|~-]+")) && value.all { it.code in 0x21..0x7e && it != ';' }
    }.getOrDefault(false)
}

data class RemoteMirrorPage(val url: String, val title: String, val next: String?, val choices: List<Choice>) {
    data class Choice(val url: String, val label: String)
    fun isAttachment(value: String): Boolean {
        val path = URI(value).path
        return path.endsWith("/download.php") || path.endsWith(".apk") || path.endsWith(".apkm")
    }
    companion object {
        fun parse(url: String, html: String, debugFixtures: Boolean = false): RemoteMirrorPage {
            val policy = ApkMirrorPolicy(debugFixtures)
            if (policy.pageUrl(url) == null) throw IOException("Byparr returned an unsupported page URL.")
            val doc = Jsoup.parse(html, url)
            val title = doc.title()
            if (doc.select("#challenge-running, #challenge-stage, iframe[src*=challenges.cloudflare.com]").isNotEmpty() ||
                title.startsWith("Just a moment", true) || title.startsWith("Checking your browser", true))
                throw IOException("Byparr returned an unresolved security check.")
            if (title.contains("Page Not Found", true)) throw IOException("APKMirror could not find that release.")
            val links = doc.select("a[href]").mapNotNull { anchor ->
                val raw = anchor.absUrl("href").substringBefore('#')
                val allowed = policy.pageUrl(raw) ?: raw.takeIf(policy::downloadUrl) ?: return@mapNotNull null
                anchor to Choice(allowed, (anchor.closest(".table-row") ?: anchor).text().ifBlank { allowed })
            }
            val buttons = links.filter { (a, link) -> a.hasClass("downloadButton") || a.id() == "download-link" ||
                URI(link.url).path.endsWith("/download.php") }.map { it.second }.distinctBy { it.url }
            val basePath = URI(url).path.trimEnd('/') + "/"
            val variants = links.map { it.second }.filter {
                val path = URI(it.url).path
                path.endsWith("-android-apk-download/") && path.startsWith(basePath) && it.url != url.substringBefore('#')
            }.distinctBy { it.url }
            val releases = links.map { it.second }.filter {
                val path = URI(it.url).path
                path.endsWith("-release/") && path.startsWith(basePath) && it.url != url.substringBefore('#')
            }.distinctBy { it.url }
            return RemoteMirrorPage(url, title, buttons.singleOrNull()?.url ?: variants.singleOrNull()?.url,
                buttons.ifEmpty { variants.ifEmpty { releases } })
        }
    }
}

/** One explicitly configured Byparr request at a time; no retry or polling loop. */
class ByparrClient(private val debugFixtures: Boolean) {
    data class Page(val content: RemoteMirrorPage, val userAgent: String, val cookies: List<RemoteCookie>) {
        fun cookieHeader(url: String) = cookies.filter { it.matches(url) }.joinToString("; ") { "${it.name}=${it.value}" }
    }
    data class Attachment(val url: String, val disposition: String?, val mime: String?, val cookieHeader: String)
    private val cancelled = AtomicBoolean(false)
    @Volatile private var connection: HttpURLConnection? = null
    fun cancel() { cancelled.set(true); connection?.disconnect() }
    private fun connect(url: String): HttpURLConnection {
        if (cancelled.get()) throw IOException("Page request cancelled.")
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        connection = conn
        if (cancelled.get()) { conn.disconnect(); throw IOException("Page request cancelled.") }
        return conn
    }

    fun fetch(endpoint: String, url: String): Page {
        val api = ByparrEndpoint.normalize(endpoint, debugFixtures) ?: throw IOException("Enter a valid HTTPS Byparr endpoint.")
        val target = ApkMirrorPolicy(debugFixtures).pageUrl(url) ?: throw IOException("Only APKMirror page links are supported.")
        val conn = connect(api)
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            // maxTimeout is the Byparr protocol's own browser-request limit.
            val body = JSONObject().put("cmd", "request.get").put("url", target).put("maxTimeout", 60000)
                .toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }
            if (conn.responseCode !in 200..299) throw IOException("Byparr API failed (HTTP ${conn.responseCode}).")
            val reply = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (reply.optString("status") != "ok") throw IOException("Byparr did not resolve the page.")
            val solution = reply.getJSONObject("solution")
            if (solution.optInt("status", 0) !in 200..299) throw IOException("Byparr reported an unsuccessful target response.")
            val contentType = solution.optString("contentType", "text/html")
            if (!contentType.startsWith("text/html")) throw IOException("Byparr did not return an APKMirror page.")
            val html = solution.getString("response")
            if (html.isBlank()) throw IOException("Byparr returned an empty page.")
            val agent = solution.getString("userAgent")
            if (agent.isBlank() || agent.contains('\r') || agent.contains('\n')) throw IOException("Byparr returned an invalid user agent.")
            val cookies = solution.optJSONArray("cookies")
            val parsed = (0 until (cookies?.length() ?: 0)).map { index ->
                val cookie = cookies!!.getJSONObject(index)
                RemoteCookie(cookie.getString("name"), cookie.getString("value"), cookie.getString("domain"),
                    cookie.optString("path", "/"), cookie.optBoolean("secure"), cookie.optDouble("expires", -1.0))
            }
            return Page(RemoteMirrorPage.parse(solution.getString("url"), html, debugFixtures), agent, parsed)
        } finally { conn.disconnect(); connection = null }
    }

    fun attachment(page: Page, start: String): Attachment {
        val policy = ApkMirrorPolicy(debugFixtures)
        val visited = mutableSetOf<String>()
        var url = start
        while (visited.add(url)) {
            if (!policy.downloadUrl(url)) throw IOException("APKMirror redirected to an unsupported attachment host.")
            val conn = connect(url)
            try {
                conn.setRequestProperty("User-Agent", page.userAgent)
                conn.setRequestProperty("Referer", page.content.url)
                page.cookieHeader(url).takeIf { it.isNotEmpty() }?.let { conn.setRequestProperty("Cookie", it) }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location") ?: throw IOException("APKMirror returned an empty redirect.")
                    url = URI(url).resolve(location).toString()
                    continue
                }
                if (code !in 200..299) throw IOException("Attachment request failed (HTTP $code).")
                val mime = conn.contentType
                val disposition = conn.getHeaderField("Content-Disposition")
                if (mime.orEmpty().startsWith("text/") || mime.orEmpty().contains("html", true))
                    throw IOException("APKMirror returned a page instead of the original file. The server session may not work from this network.")
                return Attachment(url, disposition, mime, page.cookieHeader(url))
            } finally { conn.disconnect(); connection = null }
        }
        throw IOException("APKMirror returned a redirect cycle.")
    }
}
