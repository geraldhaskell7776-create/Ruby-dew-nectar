package com.fridgephotos.google

import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/**
 * Talks to the Google Photos Picker API.
 * Google only lets apps see photos you choose yourself in its picker,
 * so the flow is: create a session, you pick in Google Photos, we download copies.
 * https://developers.google.com/photos/picker
 */
class PhotosPickerClient(private val accessToken: String) {

    data class Session(
        val id: String,
        val pickerUri: String,
        val pollIntervalMs: Long,
        val timeoutMs: Long,
    )

    data class PickedPhoto(
        val id: String,
        val baseUrl: String,
        val takenAt: Long?,
    )

    fun createSession(): Session = parseSession(JSONObject(request("POST", "$API/sessions", "{}")))

    /** Waits until you tap Done in the picker. Returns false if it timed out. */
    suspend fun waitForPick(session: Session): Boolean {
        val deadline = System.currentTimeMillis() + session.timeoutMs
        var interval = session.pollIntervalMs
        while (System.currentTimeMillis() < deadline) {
            delay(interval)
            val json = JSONObject(request("GET", "$API/sessions/${session.id}"))
            if (json.optBoolean("mediaItemsSet")) return true
            interval = parseDuration(json.optJSONObject("pollingConfig")?.optString("pollInterval"), interval)
        }
        return false
    }

    /** Lists the photos you picked (videos are skipped). */
    fun listPhotos(sessionId: String): List<PickedPhoto> {
        val out = mutableListOf<PickedPhoto>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$API/mediaItems?pageSize=100&sessionId=").append(enc(sessionId))
                pageToken?.let { append("&pageToken=").append(enc(it)) }
            }
            val json = JSONObject(request("GET", url))
            val items = json.optJSONArray("mediaItems")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.optString("type") != "PHOTO") continue
                    val file = item.optJSONObject("mediaFile") ?: continue
                    out += PickedPhoto(
                        id = item.getString("id"),
                        baseUrl = file.getString("baseUrl"),
                        takenAt = runCatching { Instant.parse(item.getString("createTime")).toEpochMilli() }.getOrNull(),
                    )
                }
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return out
    }

    /** Downloads a photo sized to fit a fridge screen (max 1920 px). */
    fun download(photo: PickedPhoto, target: File) {
        val conn = open("GET", "${photo.baseUrl}=w1920-h1920")
        val code = conn.responseCode
        if (code !in 200..299) throw IOException("Photo download failed ($code)")
        val tmp = File(target.parentFile, target.name + ".part")
        conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        if (!tmp.renameTo(target)) throw IOException("Could not save photo")
    }

    fun deleteSession(sessionId: String) {
        runCatching { request("DELETE", "$API/sessions/$sessionId") }
    }

    private fun parseSession(json: JSONObject): Session {
        val polling = json.optJSONObject("pollingConfig")
        return Session(
            id = json.getString("id"),
            pickerUri = json.getString("pickerUri"),
            pollIntervalMs = parseDuration(polling?.optString("pollInterval"), 5_000),
            timeoutMs = parseDuration(polling?.optString("timeoutIn"), 30 * 60_000),
        )
    }

    private fun open(method: String, url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

    private fun request(method: String, url: String, body: String? = null): String {
        val conn = open(method, url)
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IOException("Google Photos error $code: ${text.take(300)}")
        return text
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/photospicker.mediaitems.readonly"
        private const val API = "https://photospicker.googleapis.com/v1"

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

        /** Google sends durations like "5s" or "1799.5s". */
        internal fun parseDuration(value: String?, fallbackMs: Long): Long =
            value?.removeSuffix("s")?.toDoubleOrNull()?.let { (it * 1000).toLong() } ?: fallbackMs
    }
}
