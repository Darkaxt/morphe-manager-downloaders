package app.morphe.manager.downloaders

import java.net.URI

class ApkMirrorPolicy(private val debugFixtures: Boolean) {
    private fun parse(value: String): URI? = runCatching { URI(value) }.getOrNull()
    private fun mirror(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return (host == "apkmirror.com" || host.endsWith(".apkmirror.com")) &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 443, 80)
    }
    private fun fixture(uri: URI): Boolean = debugFixtures && uri.scheme == "http" &&
        uri.host in listOf("10.0.2.2", "127.0.0.1", "localhost") && uri.rawUserInfo == null

    fun pageUrl(value: String): String? {
        val uri = parse(value) ?: return null
        if (fixture(uri)) return value
        if (!mirror(uri)) return null
        return when (uri.scheme?.lowercase()) {
            "https" -> if (uri.port != 80) value else null
            // Decoded URI components would change escaped separators and signed query values.
            "http" -> "https://${uri.host}${uri.rawPath.orEmpty()}" +
                uri.rawQuery?.let { "?$it" }.orEmpty() + uri.rawFragment?.let { "#$it" }.orEmpty()
            else -> null
        }
    }
    fun downloadUrl(value: String): Boolean {
        val uri = parse(value) ?: return false
        return fixture(uri) || (mirror(uri) && uri.scheme == "https" && uri.port != 80)
    }
}
