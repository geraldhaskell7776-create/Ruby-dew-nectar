package com.rubydew.nectar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rubydew.nectar.ui.navigation.RubyDewNavHost
import com.rubydew.nectar.ui.theme.RubyDewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RubyDewTheme {
                RubyDewNavHost()
            }
        }
    }
}
