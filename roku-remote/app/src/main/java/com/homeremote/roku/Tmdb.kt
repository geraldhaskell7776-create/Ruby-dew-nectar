package com.homeremote.roku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A show or movie from TMDB. [kind] is "tv" or "movie". */
data class Title(val id: Int, val kind: String, val name: String, val poster: String?, val year: String)

data class Provider(val id: Int, val name: String, val priority: Int)

const val TV = "tv"
const val MOVIE = "movie"

/**
 * Reads TMDB (themoviedb.org) to list what each streaming service offers.
 * Only used for browsing; the Roku itself is still controlled over ECP.
 */
class Tmdb(private val token: String, val region: String) {
    private var providers: List<Provider>? = null

    val available get() = token.isNotBlank()

    /** Popular shows or movies on one streaming service, 20 per page. */
    suspend fun discover(kind: String, providerId: Int, page: Int): List<Title> {
        val json = get(
            "/3/discover/$kind?with_watch_providers=$providerId&watch_region=$region" +
                "&with_watch_monetization_types=flatrate%7Cfree%7Cads&sort_by=popularity.desc&page=$page",
        )
        val results = json.getJSONArray("results")
        return (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            val date = o.optString(if (kind == TV) "first_air_date" else "release_date", "")
            Title(
                id = o.getInt("id"),
                kind = kind,
                name = o.optString(if (kind == TV) "name" else "title", "?"),
                poster = o.optString("poster_path", "").takeIf { it.startsWith("/") },
                year = date.take(4),
            )
        }
    }

    /** Finds the TMDB streaming service for a Roku channel name, or null if there isn't one. */
    suspend fun providerFor(channelName: String): Provider? {
        val all = providers ?: loadProviders().also { providers = it }
        return matchProvider(channelName, all)
    }

    private suspend fun loadProviders(): List<Provider> {
        val seen = mutableMapOf<Int, Provider>()
        for (kind in listOf(TV, MOVIE)) {
            val results = get("/3/watch/providers/$kind?watch_region=$region").getJSONArray("results")
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val id = o.getInt("provider_id")
                val priority = o.optJSONObject("display_priorities")?.optInt(region, 999) ?: 999
                val p = Provider(id, o.getString("provider_name"), priority)
                if (seen[id]?.let { it.priority <= priority } != true) seen[id] = p
            }
        }
        return seen.values.sortedBy { it.priority }
    }

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL("https://api.themoviedb.org$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code >= 400) throw TmdbException(code)
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        fun posterUrl(path: String, width: Int = 342) = "https://image.tmdb.org/t/p/w$width$path"
    }
}

class TmdbException(val code: Int) : Exception("TMDB answered with HTTP $code")

private fun normalize(s: String) = s.lowercase().replace("+", "plus").filter { it.isLetterOrDigit() }

/** Roku channel names that differ from TMDB's name for the same service. */
private val aliases = mapOf(
    "max" to "hbomax",
    "primevideo" to "amazonprimevideo",
    "amazonprime" to "amazonprimevideo",
    "tubi" to "tubitv",
    "disney" to "disneyplus",
)

/**
 * Matches a Roku channel name (e.g. "Prime Video", "Peacock TV", "Tubi - Free Movies & TV")
 * to a TMDB streaming service. Exact name first, then a service whose name starts with the
 * channel's ("Paramount+" -> "Paramount Plus Premium"), skipping add-on channels sold
 * through Amazon, Apple or Roku, and preferring the best-known service.
 */
fun matchProvider(channelName: String, providers: List<Provider>): Provider? {
    val base = channelName.substringBefore(" - ").substringBefore(":").substringBefore("|")
    val keys = linkedSetOf<String>()
    for (k in listOf(normalize(channelName), normalize(base))) {
        if (k.isEmpty()) continue
        keys += k
        aliases[k]?.let { keys += it }
        if (k.endsWith("tv") && k.length > 4) {
            val noTv = k.removeSuffix("tv")
            keys += noTv
            aliases[noTv]?.let { keys += it }
        }
    }
    val addOn = listOf("amazonchannel", "appletvchannel", "rokupremiumchannel", "withads")
    val usable = providers.filter { p -> addOn.none { normalize(p.name).contains(it) } }
    for (k in keys) usable.firstOrNull { normalize(it.name) == k }?.let { return it }
    for (k in keys) {
        if (k.length < 3) continue
        usable.filter { normalize(it.name).startsWith(k) }.minByOrNull { it.priority }?.let { return it }
    }
    return null
}
