package com.fridgephotos.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.MonthDay
import java.util.UUID

/**
 * Saves albums (names, dates, photo list) in SharedPreferences and the photos
 * themselves in the app's private folder. Nothing leaves the phone except the
 * slideshow on your home Wi-Fi.
 */
class AlbumStore(context: Context) {
    private val prefs = context.getSharedPreferences("fridge_photos", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "albums")

    private val _albums = MutableStateFlow(load())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _slideSeconds = MutableStateFlow(prefs.getInt(KEY_SECONDS, DEFAULT_SECONDS))
    val slideSeconds: StateFlow<Int> = _slideSeconds.asStateFlow()

    init {
        if (!prefs.contains(KEY_ALBUMS)) {
            // Starter album so the app isn't empty the first time
            save(listOf(Album(newId(), "New Years", MonthDay.of(12, 16), MonthDay.of(1, 2))))
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    @Synchronized
    fun upsert(album: Album) {
        val list = _albums.value
        val existing = list.indexOfFirst { it.id == album.id }
        save(if (existing >= 0) list.toMutableList().also { it[existing] = album } else list + album)
    }

    @Synchronized
    fun delete(id: String) {
        albumDir(id).deleteRecursively()
        save(_albums.value.filterNot { it.id == id })
    }

    @Synchronized
    fun addPhotos(albumId: String, photos: List<Photo>) {
        val album = _albums.value.firstOrNull { it.id == albumId } ?: return
        val known = album.photos.map { it.googleId }.toSet()
        upsert(album.copy(photos = album.photos + photos.filterNot { it.googleId in known }))
    }

    @Synchronized
    fun clearPhotos(albumId: String) {
        val album = _albums.value.firstOrNull { it.id == albumId } ?: return
        albumDir(albumId).deleteRecursively()
        upsert(album.copy(photos = emptyList()))
    }

    fun hasPhoto(albumId: String, googleId: String): Boolean =
        _albums.value.firstOrNull { it.id == albumId }?.photos?.any { it.googleId == googleId } == true

    fun setSlideSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_SECONDS, seconds).apply()
        _slideSeconds.value = seconds
    }

    fun albumDir(albumId: String): File = File(root, albumId).also { it.mkdirs() }

    /** Returns the photo file, or null if the names don't point inside the album folder. */
    fun photoFile(albumId: String, file: String): File? {
        if (!SAFE_NAME.matches(albumId) || !SAFE_NAME.matches(file)) return null
        return File(File(root, albumId), file).takeIf { it.isFile }
    }

    private fun save(list: List<Album>) {
        val arr = JSONArray()
        list.forEach { album ->
            val photos = JSONArray()
            album.photos.forEach { p ->
                photos.put(
                    JSONObject()
                        .put("googleId", p.googleId)
                        .put("file", p.file)
                        .apply { p.takenAt?.let { put("takenAt", it) } }
                )
            }
            arr.put(
                JSONObject()
                    .put("id", album.id)
                    .put("name", album.name)
                    .apply {
                        album.start?.let { put("start", it.toString()) }
                        album.end?.let { put("end", it.toString()) }
                    }
                    .put("photos", photos)
            )
        }
        prefs.edit().putString(KEY_ALBUMS, arr.toString()).apply()
        _albums.value = list
    }

    private fun load(): List<Album> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_ALBUMS, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val photos = o.optJSONArray("photos") ?: JSONArray()
            Album(
                id = o.getString("id"),
                name = o.getString("name"),
                start = o.optString("start").takeIf { it.isNotEmpty() }?.let(MonthDay::parse),
                end = o.optString("end").takeIf { it.isNotEmpty() }?.let(MonthDay::parse),
                photos = (0 until photos.length()).map { j ->
                    val p = photos.getJSONObject(j)
                    Photo(
                        googleId = p.getString("googleId"),
                        file = p.getString("file"),
                        takenAt = if (p.has("takenAt")) p.getLong("takenAt") else null,
                    )
                },
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        const val DEFAULT_SECONDS = 15
        private const val KEY_ALBUMS = "albums"
        private const val KEY_SECONDS = "slide_seconds"
        private val SAFE_NAME = Regex("[A-Za-z0-9._-]+")
    }
}
