package com.fridgephotos.server

import android.content.Context
import com.fridgephotos.data.AlbumStore
import com.fridgephotos.data.DateRules
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.time.Instant
import java.time.MonthDay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Serves the slideshow to the fridge's web browser:
 *   /             the full-screen slideshow page
 *   /api/current  today's album and its photos (JSON)
 *   /photo/a/f    one photo file
 */
class SlideshowServer(
    context: Context,
    private val store: AlbumStore,
    port: Int,
) : NanoHTTPD(port) {

    private val page: String = context.assets.open("slideshow.html").bufferedReader().use { it.readText() }
    private val captionFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        return when {
            uri == "/" || uri == "/index.html" ->
                newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", page)

            uri == "/api/current" ->
                newFixedLengthResponse(Response.Status.OK, "application/json", currentJson())
                    .apply { addHeader("Cache-Control", "no-store") }

            uri.startsWith("/photo/") -> servePhoto(uri.removePrefix("/photo/"))

            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    private fun servePhoto(path: String): Response {
        val parts = path.split('/')
        val file = (if (parts.size == 2) store.photoFile(parts[0], parts[1]) else null)
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        return newFixedLengthResponse(Response.Status.OK, "image/jpeg", FileInputStream(file), file.length())
            .apply { addHeader("Cache-Control", "max-age=86400") }
    }

    private fun currentJson(): String {
        val album = DateRules.activeAlbum(store.albums.value, MonthDay.now())
        val json = JSONObject().put("seconds", store.slideSeconds.value)
        if (album == null) {
            return json.put("albumId", "")
                .put("message", "No album is set for today. Add one in the Fridge Photos app.")
                .put("photos", JSONArray())
                .toString()
        }
        val photos = JSONArray()
        album.photos.forEach { p ->
            photos.put(
                JSONObject()
                    .put("url", "/photo/${album.id}/${p.file}")
                    .put("caption", p.takenAt?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(captionFormat)
                    } ?: "")
            )
        }
        return json.put("albumId", album.id).put("albumName", album.name).put("photos", photos).toString()
    }
}
