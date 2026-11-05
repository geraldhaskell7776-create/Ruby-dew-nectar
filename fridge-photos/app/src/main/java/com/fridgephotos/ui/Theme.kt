package com.fridgephotos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1F5FAF),
    secondary = Color(0xFF3E7C6E),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9CC3FF),
    secondary = Color(0xFF8FD3C0),
)

@Composable
fun FridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
