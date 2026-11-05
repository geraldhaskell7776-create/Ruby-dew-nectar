package com.fridgephotos.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.fridgephotos.google.GoogleAuth
import com.fridgephotos.server.SlideshowService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var auth: GoogleAuth

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startSlideshow() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = GoogleAuth(this)
        setContent {
            FridgeTheme {
                MainScreen(
                    viewModel = viewModel,
                    onAddPhotos = ::addPhotos,
                    onStartSlideshow = ::askAndStartSlideshow,
                    onStopSlideshow = { SlideshowService.stop(this) },
                    onShowHere = { startActivity(Intent(this, SlideshowActivity::class.java)) },
                    onAllowBackground = ::askToRunInBackground,
                    needsBackgroundPermission = { !isIgnoringBatteryOptimizations() },
                )
            }
        }
    }

    private fun addPhotos(albumId: String) {
        lifecycleScope.launch {
            val token = try {
                auth.accessToken()
            } catch (e: Exception) {
                viewModel.showError(albumId, "Google sign-in failed: ${e.message}")
                return@launch
            }
            viewModel.addPhotosFromGoogle(albumId, token)
        }
    }

    private fun askAndStartSlideshow() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startSlideshow()
        }
    }

    private fun startSlideshow() = SlideshowService.start(this)

    private fun isIgnoringBatteryOptimizations(): Boolean =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    /** Samsung phones close background apps to save battery; this keeps the slideshow alive. */
    private fun askToRunInBackground() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        )
    }
}
