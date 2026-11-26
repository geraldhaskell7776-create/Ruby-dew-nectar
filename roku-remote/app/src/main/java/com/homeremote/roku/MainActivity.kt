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

/** The accent colour for the main buttons: Christmas red. */
val RokuPurple = ChristmasRed

private val RemoteColors = darkColorScheme(
    primary = RokuPurple,
    onPrimary = Color.White,
    secondary = ChristmasGreen,
    background = Color(0xFF0A1630),
    surface = Color(0xFF102036),
    surfaceVariant = Color(0xE61A2D47),
    onSurface = Color(0xFFF1F4F8),
    onSurfaceVariant = Color(0xFFC3CEDC),
    error = Color(0xFFFF6B6B),
)
