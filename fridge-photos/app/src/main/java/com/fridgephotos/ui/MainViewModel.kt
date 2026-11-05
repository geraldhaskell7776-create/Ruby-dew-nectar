package com.fridgephotos.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fridgephotos.FridgePhotosApp
import com.fridgephotos.data.Album
import com.fridgephotos.data.Photo
import com.fridgephotos.google.PhotosPickerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.MonthDay

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val store = (app as FridgePhotosApp).store

    /** Progress text per album while photos are being added ("Downloading 3 of 20…"). */
    private val _status = MutableStateFlow<Map<String, String>>(emptyMap())
    val status: StateFlow<Map<String, String>> = _status.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    fun saveAlbum(id: String?, name: String, start: MonthDay?, end: MonthDay?) {
        val existing = id?.let { i -> store.albums.value.firstOrNull { it.id == i } }
        store.upsert(
            existing?.copy(name = name, start = start, end = end)
                ?: Album(store.newId(), name, start, end)
        )
    }

    fun deleteAlbum(id: String) {
        jobs.remove(id)?.cancel()
        setStatus(id, null)
        store.delete(id)
    }

    fun clearPhotos(id: String) = store.clearPhotos(id)

    fun changeSeconds(delta: Int) =
        store.setSlideSeconds((store.slideSeconds.value + delta).coerceIn(5, 300))

    /**
     * Opens the Google Photos picker, waits for you to tap Done,
     * then downloads the photos you chose into the album.
     */
    fun addPhotosFromGoogle(albumId: String, accessToken: String) {
        if (jobs[albumId]?.isActive == true) return
        jobs[albumId] = viewModelScope.launch(Dispatchers.IO) {
            val client = PhotosPickerClient(accessToken)
            var sessionId: String? = null
            try {
                setStatus(albumId, "Opening Google Photos…")
                val session = client.createSession()
                sessionId = session.id
                withContext(Dispatchers.Main) { openUrl(session.pickerUri) }
                setStatus(albumId, "Pick your photos in Google Photos, tap Done, then come back here.")

                if (!client.waitForPick(session)) {
                    setStatus(albumId, "Google Photos timed out. Tap Add photos to try again.")
                    return@launch
                }

                setStatus(albumId, "Getting your photo list…")
                val picked = client.listPhotos(session.id).filterNot { store.hasPhoto(albumId, it.id) }
                val dir = store.albumDir(albumId)
                picked.forEachIndexed { i, photo ->
                    setStatus(albumId, "Downloading ${i + 1} of ${picked.size}…")
                    val file = fileNameFor(photo.id)
                    client.download(photo, java.io.File(dir, file))
                    store.addPhotos(albumId, listOf(Photo(photo.id, file, photo.takenAt)))
                }
                setStatus(albumId, if (picked.isEmpty()) "No new photos added." else "Added ${picked.size} photos ✓")
                delay(6_000)
                setStatus(albumId, null)
            } catch (e: Exception) {
                setStatus(albumId, "Something went wrong: ${e.message}")
            } finally {
                sessionId?.let { client.deleteSession(it) }
            }
        }
    }

    fun showError(albumId: String, message: String) = setStatus(albumId, message)

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    private fun setStatus(albumId: String, text: String?) = _status.update {
        if (text == null) it - albumId else it + (albumId to text)
    }

    private fun fileNameFor(googleId: String): String {
        val hash = MessageDigest.getInstance("SHA-1").digest(googleId.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(24) + ".jpg"
    }
}
