package com.homeremote.roku

import android.content.Context
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

data class RokuDevice(val host: String, val name: String)
data class RokuApp(val id: String, val name: String)

/**
 * Talks to a Roku using its built-in External Control Protocol (ECP):
 * plain HTTP on port 8060 of the Roku, on your home Wi-Fi. No account, no pairing.
 */
class RokuClient(val host: String) {
    private val base = "http://$host:8060"

    /** Sends a command such as "/keypress/Home" or "/launch/12". */
    suspend fun post(path: String) = withContext(Dispatchers.IO) {
        val conn = open(path).apply {
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(0)
        }
        try {
            conn.outputStream.close()
            val code = conn.responseCode
            if (code >= 400) throw RokuException(code)
        } finally {
            conn.disconnect()
        }
    }

    /** Returns the name to show for this Roku, or throws if nothing answers. */
    suspend fun deviceName(): String {
        val xml = getText("/query/device-info")
        return listOf("user-device-name", "friendly-device-name", "model-name")
            .firstNotNullOfOrNull { tag(xml, it)?.takeIf(String::isNotBlank) }
            ?: "Roku ($host)"
    }

    /** The channels installed on the Roku, in the order the Roku lists them. */
    suspend fun apps(): List<RokuApp> {
        val xml = getText("/query/apps")
        return Regex("""<app\s+id="([^"]+)"[^>]*>([^<]*)</app>""")
            .findAll(xml)
            .map { RokuApp(it.groupValues[1], unescape(it.groupValues[2]).trim()) }
            .toList()
    }

    suspend fun icon(appId: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val conn = open("/query/icon/$appId")
        try {
            if (conn.responseCode != 200) return@withContext null
            val bytes = conn.inputStream.use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun getText(path: String): String = withContext(Dispatchers.IO) {
        val conn = open(path)
        try {
            val code = conn.responseCode
            if (code >= 400) throw RokuException(code)
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun open(path: String) =
        (URL(base + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2500
            readTimeout = 4000
            useCaches = false
        }

    private fun tag(xml: String, name: String): String? =
        Regex("<$name>([^<]*)</$name>").find(xml)?.groupValues?.get(1)?.let(::unescape)

    private fun unescape(s: String) = s
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
        .replace("&amp;", "&")
}

/** The Roku answered but said no (403 usually means "Control by mobile apps" is off). */
class RokuException(val code: Int) : Exception("Roku answered with HTTP $code")

/**
 * Finds Rokus on the current Wi-Fi with an SSDP search ("ST: roku:ecp").
 * Every Roku replies with its address, e.g. "LOCATION: http://192.168.1.42:8060/".
 * Returns the IP addresses found.
 */
suspend fun discoverRokus(context: Context, listenMs: Long = 3000): List<String> =
    withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock("roku-search")?.apply {
            setReferenceCounted(false)
            acquire()
        }
        val found = linkedSetOf<String>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 400
                val message = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                        "Host: 239.255.255.250:1900\r\n" +
                        "Man: \"ssdp:discover\"\r\n" +
                        "ST: roku:ecp\r\n" +
                        "MX: 2\r\n\r\n"
                    ).toByteArray()
                val group = InetAddress.getByName("239.255.255.250")
                // Send twice: UDP can drop a packet on busy Wi-Fi.
                repeat(2) { socket.send(DatagramPacket(message, message.size, group, 1900)) }

                val buffer = ByteArray(2048)
                val end = System.currentTimeMillis() + listenMs
                while (System.currentTimeMillis() < end) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    val reply = String(packet.data, 0, packet.length)
                    Regex("""(?im)^location:\s*http://([^:/\s]+)""")
                        .find(reply)?.groupValues?.get(1)?.let { found += it }
                }
            }
        } catch (e: Exception) {
            // No Wi-Fi or the network blocks multicast; the user can type the IP instead.
        } finally {
            lock?.release()
        }
        found.toList()
    }
