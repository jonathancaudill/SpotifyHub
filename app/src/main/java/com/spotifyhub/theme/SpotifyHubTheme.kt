package com.spotifyhub.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9FD3C7),
    onPrimary = Color(0xFF0A0E14),
    secondary = Color(0xFF2B3A42),
    background = Color(0xFF0A0E14),
    surface = Color(0xFF111823),
    onSurface = Color(0xFFEAF7F3),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1C4F48),
    onPrimary = Color.White,
    secondary = Color(0xFF38545A),
    background = Color(0xFFF5FBF8),
    surface = Color.White,
    onSurface = Color(0xFF0A0E14),
)

@Composable
fun SpotifyHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}

