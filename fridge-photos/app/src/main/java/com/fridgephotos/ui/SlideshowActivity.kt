package com.fridgephotos.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fridgephotos.server.SlideshowService

/**
 * Plays the slideshow full screen on this device. Use it when the app runs on the
 * screen itself (an Android fridge or a tablet on the fridge door).
 * Press Back to leave.
 */
class SlideshowActivity : ComponentActivity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        SlideshowService.start(this)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        setContentView(web)
        // Give the server a moment to start the first time
        web.postDelayed({ web.loadUrl("http://127.0.0.1:${SlideshowService.PORT}/") }, 800)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
