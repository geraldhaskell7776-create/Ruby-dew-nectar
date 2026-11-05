package com.fridgephotos.server

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fridgephotos.FridgePhotosApp
import com.fridgephotos.ui.MainActivity
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

/** Keeps the slideshow server running in the background, with a notification while it's on. */
class SlideshowService : Service() {
    private var server: SlideshowServer? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (server != null) return START_STICKY
        startInForeground()
        try {
            val store = (application as FridgePhotosApp).store
            server = SlideshowServer(this, store, PORT).also { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }
        } catch (e: Exception) {
            _error.value = "Couldn't start the slideshow: ${e.message}"
            stopSelf()
            return START_NOT_STICKY
        }
        acquireLocks()
        _error.value = null
        _running.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        wifiLock?.takeIf { it.isHeld }?.release()
        wakeLock?.takeIf { it.isHeld }?.release()
        _running.value = false
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Fridge slideshow", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Fridge slideshow is on")
            .setContentText("On the fridge browser open ${address() ?: "this phone's address"}")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        val wifi = applicationContext.getSystemService(WifiManager::class.java)
        wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "FridgePhotos:wifi").apply { acquire() }
        val power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FridgePhotos:server").apply { acquire() }
    }

    companion object {
        const val PORT = 8080
        private const val CHANNEL = "slideshow"
        private const val NOTIFICATION_ID = 1

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, SlideshowService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, SlideshowService::class.java))
        }

        /** The web address to type into the fridge browser, e.g. http://192.168.1.23:8080 */
        fun address(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedByDescending { it.name.startsWith("wlan") }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()?.let { "http://$it:$PORT" }
    }
}
