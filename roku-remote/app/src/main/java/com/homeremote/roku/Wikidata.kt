package com.homeremote.roku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** What the Roku needs to open a title directly: the service's ID and Roku's media type. */
data class DeepLink(val contentId: String, val mediaType: String)

/**
 * Looks up a show's or movie's ID inside each streaming service (e.g. Netflix's
 * number for "Ozark") on Wikidata, using the TMDB ID to find it. With that ID the
 * Roku can open the show itself instead of the channel's home screen.
 */
object Wikidata {
    /** Wikidata properties holding each service's IDs, per service (normalized TMDB name). */
    private val services = mapOf(
        "netflix" to Ids(tv = listOf("P1874"), movie = listOf("P1874")),
        "disneyplus" to Ids(tv = listOf("P13902", "P7596"), movie = listOf("P13902", "P7595")),
        "hulu" to Ids(tv = listOf("P6467"), movie = listOf("P6466")),
        "hbomax" to Ids(tv = listOf("P8298"), movie = listOf("P8298")),
        "amazonprimevideo" to Ids(tv = listOf("P14440", "P8055"), movie = listOf("P14440", "P8055")),
        "peacockpremium" to Ids(tv = listOf("P11815"), movie = listOf("P11815")),
    )

    private class Ids(val tv: List<String>, val movie: List<String>)

    private val cache = mutableMapOf<String, Map<String, List<String>>>()

    /**
     * The ID of [title] inside the service a Roku channel belongs to, or null if
     * Wikidata doesn't know it (or we don't know that service).
     */
    suspend fun idFor(title: Title, service: Provider): DeepLink? {
        val key = service.name.lowercase().replace("+", "plus").filter { it.isLetterOrDigit() }
        val ids = services[key] ?: return null
        val props = if (title.kind == TV) ids.tv else ids.movie
        val all = lookup(title)
        val raw = props.firstNotNullOfOrNull { all[it]?.firstOrNull() } ?: return null
        val mediaType = if (title.kind == TV) "series" else "movie"
        if (key == "amazonprimevideo") {
            // Wikidata stores Amazon's store code (e.g. B07QQQ52B3), which Prime's Roku channel
            // ignores. Prime's own page for that code names the internal ID it does use.
            val gti = primeTitleId(raw) ?: return null
            return DeepLink(gti, if (title.kind == TV) "season" else "movie")
        }
        // Max's IDs are stored as "series/urn:hbo:series:..."; the Roku wants the urn part.
        return DeepLink(raw.substringAfter('/', raw), mediaType)
    }

    private val primeCache = mutableMapOf<String, String?>()

    /** Reads primevideo.com/detail/<code> and returns the page's own "amzn1.dv.gti...." ID. */
    private suspend fun primeTitleId(code: String): String? {
        if (code.startsWith("amzn1.dv.gti.")) return code
        if (code in primeCache) return primeCache[code]
        val found = withContext(Dispatchers.IO) {
            val conn = (URL("https://www.primevideo.com/detail/" + URLEncoder.encode(code, "UTF-8")).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }
            try {
                if (conn.responseCode >= 400) return@withContext null
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                Regex("""pageTitleId":"(amzn1\.dv\.gti\.[0-9a-f-]{36})""").find(html)?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }
        primeCache[code] = found
        return found
    }

    private suspend fun lookup(title: Title): Map<String, List<String>> {
        val cacheKey = "${title.kind}/${title.id}"
        cache[cacheKey]?.let { return it }
        val tmdbProp = if (title.kind == TV) "P4983" else "P4947"
        val wanted = services.values.flatMap { it.tv + it.movie }.toSet().joinToString(" ") { "wdt:$it" }
        val sparql = "SELECT ?p ?v WHERE { ?item wdt:$tmdbProp \"${title.id}\" . VALUES ?p { $wanted } ?item ?p ?v . }"
        val result = withContext(Dispatchers.IO) {
            val url = "https://query.wikidata.org/sparql?format=json&query=" + URLEncoder.encode(sparql, "UTF-8")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6000
                setRequestProperty("Accept", "application/sparql-results+json")
                setRequestProperty("User-Agent", "HomeRokuRemote/1.0 (personal Android app)")
            }
            try {
                if (conn.responseCode >= 400) return@withContext emptyMap()
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val rows = json.getJSONObject("results").getJSONArray("bindings")
                val map = mutableMapOf<String, MutableList<String>>()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val prop = row.getJSONObject("p").getString("value").substringAfterLast('/')
                    map.getOrPut(prop) { mutableListOf() } += row.getJSONObject("v").getString("value")
                }
                map
            } finally {
                conn.disconnect()
            }
        }
        cache[cacheKey] = result
        return result
    }
}
