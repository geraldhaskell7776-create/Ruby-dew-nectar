package com.rubydew.nectar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ruby palette
val Ruby = Color(0xFF8E0E1E)
val RubyDark = Color(0xFF4A0A12)
val Garnet = Color(0xFF6A0B14)
val Rose = Color(0xFFAD1457)
val RubyLight = Color(0xFFFFB3B8)
val AlertRed = Color(0xFFD50000)
val CardGold = Color(0xFFE6B84C)
val Leaf = CardGold
val Neutral = Color(0xFF9E9E9E)

private val LightColors = lightColorScheme(
    primary = Ruby,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8D7DA),
    onPrimaryContainer = RubyDark,
    secondary = Rose,
    onSecondary = Color.White,
    secondaryContainer = Garnet,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFB8860B),
    background = Color(0xFFFFF5F5),
    onBackground = Color(0xFF2B0A0E),
    surface = Color(0xFFFFF5F5),
    onSurface = Color(0xFF2B0A0E),
    surfaceVariant = Color(0xFFF6DDE0),
    onSurfaceVariant = Color(0xFF5E3A3E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F1),
    surfaceContainer = Color(0xFFFBE6E8),
    surfaceContainerHigh = Color(0xFFF8E0E3),
    surfaceContainerHighest = Color(0xFFF4D9DC),
    outline = Color(0xFF9C6B70),
)

private val DarkColors = darkColorScheme(
    primary = RubyLight,
    onPrimary = RubyDark,
    primaryContainer = Garnet,
    onPrimaryContainer = Color(0xFFFFDADC),
    secondary = Color(0xFFFF8FB1),
    onSecondary = Color(0xFF3B0716),
    secondaryContainer = Color(0xFF5C0A1E),
    onSecondaryContainer = Color(0xFFFFDADC),
    tertiary = CardGold,
    background = Color(0xFF1A0508),
    onBackground = Color(0xFFF5DDE0),
    surface = Color(0xFF1A0508),
    onSurface = Color(0xFFF5DDE0),
    surfaceVariant = Color(0xFF3A1A1E),
    onSurfaceVariant = Color(0xFFDDBFC2),
    surfaceContainerLowest = Color(0xFF140306),
    surfaceContainerLow = Color(0xFF22080C),
    surfaceContainer = Color(0xFF2A0B10),
    surfaceContainerHigh = Color(0xFF341016),
    surfaceContainerHighest = Color(0xFF3F151C),
    outline = Color(0xFFA78A8D),
)

@Composable
fun RubyDewTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
