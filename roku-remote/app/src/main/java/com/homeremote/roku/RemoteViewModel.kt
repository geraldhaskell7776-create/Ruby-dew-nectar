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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.net.URLEncoder

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
