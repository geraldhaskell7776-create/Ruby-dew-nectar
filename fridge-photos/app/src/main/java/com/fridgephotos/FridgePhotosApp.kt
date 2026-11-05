package com.fridgephotos

import android.app.Application
import com.fridgephotos.data.AlbumStore

class FridgePhotosApp : Application() {
    lateinit var store: AlbumStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = AlbumStore(this)
    }
}
