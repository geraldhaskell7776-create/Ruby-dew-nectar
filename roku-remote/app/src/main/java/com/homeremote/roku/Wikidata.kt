package com.homeremote.roku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** What the Roku needs to open a title directly: the service's ID and Roku's media type. */
data class DeepLink(val contentId: String, val mediaType: String, val label: String = "")

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

    private fun serviceKey(service: Provider) = service.name.lowercase().replace("+", "plus").filter { it.isLetterOrDigit() }

    /**
     * IDs Wikidata is missing, keyed "service|kind|tmdbId". Starts with ones looked up by hand;
     * the user can add more by pasting a link in the link tester. The app saves these.
     */
    val manual = mutableMapOf(
        // Night Court (1984), Season 1 on Prime Video: primevideo.com/detail/0JKQMQRYLQ7AQRRSNOGZQAN1PB
        "amazonprimevideo|tv|162" to "0JKQMQRYLQ7AQRRSNOGZQAN1PB",
    )

    private fun manualKey(title: Title, service: Provider) = "${serviceKey(service)}|${title.kind}|${title.id}"

    /**
     * Pulls a title's ID out of a link copied from a streaming service, e.g.
     * primevideo.com/detail/0JKQ..., netflix.com/title/80117552, disneyplus.com/.../entity-....
     * Returns null if the text doesn't look like such a link.
     */
    fun idFromLink(text: String): String? {
        val t = text.trim()
        Regex("""amzn1\.dv\.gti\.[0-9a-f-]{36}""").find(t)?.let { return it.value }
        Regex("""/(?:gp/video/)?detail/(?:[^/?#]+/)?([A-Z0-9]{10,})""").find(t)?.let { return it.groupValues[1] }
        Regex("""/title/(\d{5,})""").find(t)?.let { return it.groupValues[1] }
        Regex("""entity-[0-9a-f-]{36}""").find(t)?.let { return it.value }
        Regex("""urn:hbo:[a-z]+:[A-Za-z0-9_-]+""").find(t)?.let { return it.value }
        Regex("""hulu\.com/(?:series|movie)/[^?#]*?([0-9a-f]{8}-[0-9a-f-]{27})""").find(t)?.let { return it.groupValues[1] }
        return null
    }

    /** Remembers [id] as the ID of [title] on [service]. */
    fun remember(title: Title, service: Provider, id: String) {
        manual[manualKey(title, service)] = id
    }

    /** The raw ID we have for [title] on this service (one the user gave us first, then Wikidata's), or null. */
    private suspend fun rawId(title: Title, service: Provider): String? {
        manual[manualKey(title, service)]?.let { return it }
        val ids = services[serviceKey(service)] ?: return null
        val props = if (title.kind == TV) ids.tv else ids.movie
        val all = lookup(title)
        return props.firstNotNullOfOrNull { all[it]?.firstOrNull() }
    }

    /**
     * The best known way to open [title] directly on this service, or null if
     * Wikidata doesn't know it (or we don't know that service).
     */
    suspend fun idFor(title: Title, service: Provider): DeepLink? = candidates(title, service).links.firstOrNull()

    /** Every way we know to try opening [title] on this service, best guess first, plus notes on what failed. */
    suspend fun candidates(title: Title, service: Provider): Candidates {
        val notes = mutableListOf<String>()
        val raw = rawId(title, service)
        if (raw == null) {
            notes += if (services.containsKey(serviceKey(service))) {
                "No ${service.name} ID for this title yet. Paste a link to it from ${service.name} below to add one."
            } else {
                "No ID source for ${service.name}."
            }
            return Candidates(emptyList(), notes)
        }
        val tvTypes = listOf("series", "season", "episode", "movie")
        val movieTypes = listOf("movie", "series", "episode")
        val types = if (title.kind == TV) tvTypes else movieTypes
        if (serviceKey(service) != "amazonprimevideo") {
            // Max's IDs are stored as "series/urn:hbo:series:..."; the Roku wants the urn part.
            val id = raw.substringAfter('/', raw)
            notes += "${service.name} ID: $id"
            return Candidates(types.map { DeepLink(id, it, "ID as $it") }, notes)
        }
        // Prime: Wikidata has Amazon's store code (B0...). Prime's own page for it also names
        // the internal ID (amzn1.dv.gti....) and the ID in Prime's web links (detail/0T5B...).
        notes += "Prime ID we have: $raw"
        val page = primePage(raw)
        notes += page.note
        val links = mutableListOf<DeepLink>()
        page.detailId?.let { id -> types.forEach { links += DeepLink(id, it, "Web-link ID as $it") } }
        page.gti?.let { id -> types.forEach { links += DeepLink(id, it, "Internal ID as $it") } }
        types.forEach { links += DeepLink(raw, it, "Saved ID as $it") }
        return Candidates(links.distinctBy { it.contentId to it.mediaType }, notes)
    }

    class Candidates(val links: List<DeepLink>, val notes: List<String>)

    private class PrimePage(val gti: String?, val detailId: String?, val note: String)

    private val primeCache = mutableMapOf<String, PrimePage>()

    /** Reads Prime's web page for a store code and pulls out its other IDs. */
    private suspend fun primePage(code: String): PrimePage {
        primeCache[code]?.takeIf { it.gti != null || it.detailId != null }?.let { return it }
        val urls = listOf(
            "https://www.primevideo.com/detail/$code",
            "https://www.amazon.com/gp/video/detail/$code",
        )
        val problems = mutableListOf<String>()
        for (url in urls) {
            val result = withContext(Dispatchers.IO) {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 10000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36")
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    setRequestProperty("Accept", "text/html")
                }
                try {
                    val code2 = conn.responseCode
                    if (code2 >= 400) return@withContext "HTTP $code2 from ${conn.url.host}" to null
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val gti = Regex("""pageTitleId":"(amzn1\.dv\.gti\.[0-9a-f-]{36})""").find(html)?.groupValues?.get(1)
                    val detail = Regex("""rel="canonical" href="https://www\.primevideo\.com/detail/([A-Z0-9]{10,})""").find(html)?.groupValues?.get(1)
                    if (gti == null && detail == null) {
                        val robot = html.contains("captcha", ignoreCase = true) || html.contains("robot", ignoreCase = true)
                        (if (robot) "Robot check from ${conn.url.host}" else "No IDs found on ${conn.url.host}") to null
                    } else {
                        "Read Prime's page (${conn.url.host})" to PrimePage(gti, detail, "")
                    }
                } catch (e: Exception) {
                    "Couldn't load ${URL(url).host}: ${e.javaClass.simpleName}" to null
                } finally {
                    conn.disconnect()
                }
            }
            val page = result.second
            if (page != null) {
                val done = PrimePage(page.gti, page.detailId, result.first + ". Internal ID: ${page.gti ?: "none"}; web-link ID: ${page.detailId ?: "none"}")
                primeCache[code] = done
                return done
            }
            problems += result.first
        }
        return PrimePage(null, null, problems.joinToString("; "))
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
