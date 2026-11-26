package com.homeremote.roku

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private val vm: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = RemoteColors) {
                RemoteApp(vm)
            }
        }
    }

    /** The phone's own volume buttons change the TV volume while a Roku is connected. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (vm.device != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { vm.press("VolumeUp"); return true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { vm.press("VolumeDown"); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        // Channels may have been added or removed while we were away.
        vm.loadApps()
    }
}

val RokuPurple = Color(0xFF8E5CF7)

private val RemoteColors = darkColorScheme(
    primary = RokuPurple,
    onPrimary = Color.White,
    secondary = Color(0xFFB79CFF),
    background = Color(0xFF121016),
    surface = Color(0xFF1B1822),
    surfaceVariant = Color(0xFF2A2533),
    onSurface = Color(0xFFECE8F2),
    onSurfaceVariant = Color(0xFFB9B2C6),
    error = Color(0xFFFF6B6B),
)
