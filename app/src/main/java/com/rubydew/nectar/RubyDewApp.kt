package com.rubydew.nectar

import android.app.Application
import com.rubydew.nectar.data.LocalStore
import org.osmdroid.config.Configuration

class RubyDewApp : Application() {
    lateinit var store: LocalStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = LocalStore(this)

        // OpenStreetMap asks apps to identify themselves. Keep the map tile cache small.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            tileFileSystemCacheMaxBytes = 20L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 15L * 1024 * 1024
        }
    }
}
