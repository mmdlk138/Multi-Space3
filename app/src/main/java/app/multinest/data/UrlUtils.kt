package app.multinest.data

import java.net.URI
import java.net.URLEncoder

object UrlUtils {
    /** Returns a normalized http(s) URL, or null if the input is not a valid site address. */
    fun normalize(input: String): String? {
        val t = input.trim()
        if (t.isEmpty() || t.contains(' ')) return null
        val withScheme = if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (!host.contains('.') && host != "localhost") return null
        return withScheme
    }

    /** Address-bar input: a URL if it looks like one, otherwise a web search. */
    fun toAddress(input: String): String =
        normalize(input) ?: "https://duckduckgo.com/?q=" + URLEncoder.encode(input.trim(), "UTF-8")
}
