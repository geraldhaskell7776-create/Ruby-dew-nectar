package com.rubydew.nectar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ruby = Color(0xFF9B111E)
val RubyDark = Color(0xFF6A0B14)
val RubyLight = Color(0xFFFFB3B0)
val AlertRed = Color(0xFFD50000)
val Leaf = Color(0xFF2E7D32)
val Neutral = Color(0xFF9E9E9E)

private val LightColors = lightColorScheme(
    primary = Ruby,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = RubyDark,
    secondary = Leaf,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F5D9),
    onSecondaryContainer = Color(0xFF0B3D0F),
    background = Color(0xFFFFFBFA),
    surface = Color(0xFFFFFBFA),
)

private val DarkColors = darkColorScheme(
    primary = RubyLight,
    onPrimary = RubyDark,
    primaryContainer = Color(0xFF7A0E19),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFF9AD69D),
    onSecondary = Color(0xFF0B3D0F),
    secondaryContainer = Color(0xFF1E5221),
    onSecondaryContainer = Color(0xFFD7F5D9),
)

@Composable
fun RubyDewTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
