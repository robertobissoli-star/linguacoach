package com.linguacoach.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF5B8CFF)

private val Dark = darkColorScheme(
    primary = Accent,
    background = Color(0xFF0E1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF1F2630),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
)
private val Light = lightColorScheme(
    primary = Color(0xFF2F6BFF),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF0F6),
    onBackground = Color(0xFF12161C),
    onSurface = Color(0xFF12161C),
)

@Composable
fun LinguaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
