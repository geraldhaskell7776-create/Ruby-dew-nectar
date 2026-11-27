package com.homeremote.roku

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

/** A show or movie the user starred, and the Roku channel it was found on. */
data class Favorite(val appId: String, val appName: String, val title: Title)

/** What the Channels page knows about one channel's shows (or movies). */
data class Browse(
    val loading: Boolean = false,
    val titles: List<Title> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    /** TMDB has no listing for this channel. */
    val noMatch: Boolean = false,
    val error: String? = null,
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("roku", Context.MODE_PRIVATE)
    private var client: RokuClient? = null

    /** The Roku we're controlling, or null while on the setup screen. */
    var device by mutableStateOf<RokuDevice?>(null)
        private set
    var found by mutableStateOf<List<RokuDevice>>(emptyList())
        private set
    var scanning by mutableStateOf(false)
        private set
    var connecting by mutableStateOf(false)
        private set
    /** A problem to show the user, or null when everything is working. */
    var problem by mutableStateOf<String?>(null)
        private set
    var apps by mutableStateOf<List<RokuApp>>(emptyList())
        private set

    /** A short friendly note, e.g. what the TV is doing now; clears itself. */
    var notice by mutableStateOf<String?>(null)
        private set
    private var noticeJob: Job? = null

    private val tmdb = Tmdb(BuildConfig.TMDB_TOKEN, Locale.getDefault().country.ifBlank { "US" })
    val canBrowse get() = tmdb.available
    /** Keyed by "appId/tv" or "appId/movie". */
    val browse = mutableStateMapOf<String, Browse>()

    /** Starred shows and movies, newest first. Kept on the phone only. */
    var favorites by mutableStateOf(loadFavorites())
        private set
    val icons = mutableStateMapOf<String, ImageBitmap>()
    private var loadingApps = false

    // Commands go out one at a time, in order, so fast typing doesn't scramble letters.
    private val commands = Channel<String>(Channel.UNLIMITED)

    init {
        val savedHost = prefs.getString("host", null)
        if (savedHost != null) {
            use(RokuDevice(savedHost, prefs.getString("name", null) ?: savedHost))
        } else {
            scan()
        }
        viewModelScope.launch {
            for (path in commands) {
                val c = client ?: continue
                try {
                    c.post(path)
                    problem = null
                } catch (e: RokuException) {
                    problem = if (e.code == 403) TURNED_OFF else "The Roku said no (error ${e.code})."
                } catch (e: Exception) {
                    problem = "Can't reach ${device?.name ?: "the Roku"}. Is the phone on the same Wi-Fi?"
                }
            }
        }
    }

    fun press(key: String) {
        commands.trySend("/keypress/$key")
    }

    /** Types one character into whatever search box is open on the TV. */
    fun type(ch: Char) {
        val encoded = URLEncoder.encode(ch.toString(), "UTF-8").replace("+", "%20")
        commands.trySend("/keypress/Lit_$encoded")
    }

    fun launch(app: RokuApp) {
        commands.trySend("/launch/${app.id}")
    }

    /** Loads the first (or, with [more], the next) page of a channel's shows or movies. */
    fun browse(app: RokuApp, kind: String, more: Boolean = false) {
        val key = "${app.id}/$kind"
        val now = browse[key] ?: Browse()
        if (now.loading || now.noMatch) return
        if (!more && now.page > 0 && now.error == null) return
        if (more && !now.hasMore) return
        browse[key] = now.copy(loading = true, error = null)
        viewModelScope.launch {
            browse[key] = try {
                val provider = tmdb.providerFor(app.name)
                if (provider == null) {
                    Browse(noMatch = true)
                } else {
                    val page = now.page + 1
                    val found = tmdb.discover(kind, provider.id, page)
                    val known = now.titles.map { it.id }.toSet()
                    now.copy(
                        loading = false,
                        titles = now.titles + found.filter { it.id !in known },
                        page = page,
                        hasMore = found.size >= 20,
                    )
                }
            } catch (e: TmdbException) {
                now.copy(loading = false, error = "TMDB said no (error ${e.code}). The token may be wrong or expired.")
            } catch (e: Exception) {
                now.copy(loading = false, error = "Couldn't load shows. Check the phone's internet connection.")
            }
        }
    }

    /**
     * Opens the channel a show is on. Roku doesn't let phone apps jump
     * straight into a show on most channels, so the user picks it there.
     */
    fun openTitle(appId: String, appName: String, title: Title) {
        commands.trySend("/launch/$appId")
        showNotice("Opening $appName. Find \"${title.name}\" there; the keyboard button can type it for you.")
    }

    fun isFavorite(appId: String, title: Title) = favorites.any { it.appId == appId && it.title.id == title.id && it.title.kind == title.kind }

    fun toggleFavorite(app: RokuApp, title: Title) {
        favorites = if (isFavorite(app.id, title)) {
            favorites.filterNot { it.appId == app.id && it.title.id == title.id && it.title.kind == title.kind }
        } else {
            listOf(Favorite(app.id, app.name, title)) + favorites
        }
        saveFavorites()
    }

    fun removeFavorite(f: Favorite) {
        favorites = favorites - f
        saveFavorites()
    }

    private fun showNotice(text: String) {
        notice = text
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(10_000)
            notice = null
        }
    }

    private fun loadFavorites(): List<Favorite> = try {
        val array = JSONArray(prefs.getString("starred", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Favorite(
                o.getString("appId"),
                o.getString("appName"),
                Title(o.getInt("id"), o.getString("kind"), o.getString("name"), o.optString("poster").ifBlank { null }, o.optString("year")),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun saveFavorites() {
        val array = JSONArray()
        for (f in favorites) {
            array.put(
                JSONObject().put("appId", f.appId).put("appName", f.appName)
                    .put("id", f.title.id).put("kind", f.title.kind).put("name", f.title.name)
                    .put("poster", f.title.poster ?: "").put("year", f.title.year),
            )
        }
        prefs.edit().putString("starred", array.toString()).remove("favorites").apply()
    }

    fun scan() {
        if (scanning) return
        scanning = true
        problem = null
        viewModelScope.launch {
            val hosts = discoverRokus(getApplication<Application>())
            found = hosts.map { host ->
                async {
                    val name = runCatching { RokuClient(host).deviceName() }.getOrDefault("Roku ($host)")
                    RokuDevice(host, name)
                }
            }.awaitAll()
            scanning = false
            if (found.isEmpty()) {
                problem = "No Roku found on this Wi-Fi. You can type its IP address below."
            }
        }
    }

    /** Connects to an IP the user typed (Roku: Settings > Network > About). */
    fun connectTo(ip: String) {
        val host = ip.trim()
        if (host.isEmpty()) return
        connecting = true
        problem = null
        viewModelScope.launch {
            try {
                use(RokuDevice(host, RokuClient(host).deviceName()))
            } catch (e: RokuException) {
                problem = if (e.code == 403) TURNED_OFF else "The Roku said no (error ${e.code})."
            } catch (e: Exception) {
                problem = "No Roku answered at $host. Check the address and that you're on the same Wi-Fi."
            }
            connecting = false
        }
    }

    fun use(roku: RokuDevice) {
        client = RokuClient(roku.host)
        device = roku
        problem = null
        prefs.edit().putString("host", roku.host).putString("name", roku.name).apply()
        loadApps()
    }

    /** Goes back to the setup screen to pick a different Roku. */
    fun changeRoku() {
        client = null
        device = null
        apps = emptyList()
        icons.clear()
        prefs.edit().remove("host").remove("name").apply()
        scan()
    }

    fun loadApps() {
        val c = client ?: return
        if (loadingApps) return
        loadingApps = true
        viewModelScope.launch {
            try {
                val list = c.apps()
                if (client !== c) return@launch
                apps = list
                for (app in list) {
                    if (app.id !in icons) {
                        c.icon(app.id)?.let { icons[app.id] = it }
                    }
                }
            } catch (e: Exception) {
                // Leave the list as it was; the Channels tab has a retry button.
            } finally {
                loadingApps = false
            }
        }
    }

    private companion object {
        const val TURNED_OFF =
            "The Roku is blocking phone control. On the Roku go to Settings > System > " +
                "Advanced system settings > Control by mobile apps and turn it on."
    }
}
