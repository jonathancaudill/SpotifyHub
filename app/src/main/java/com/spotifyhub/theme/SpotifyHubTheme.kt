package com.spotifyhub.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spotifyhub.R

private val SfProText = FontFamily(
    Font(R.font.sf_pro_text_light, FontWeight.Light),
    Font(R.font.sf_pro_text_regular, FontWeight.Normal),
    Font(R.font.sf_pro_text_medium, FontWeight.Medium),
    Font(R.font.sf_pro_text_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_text_bold, FontWeight.Bold),
)

private val SfProDisplay = FontFamily(
    Font(R.font.sf_pro_display_light, FontWeight.Light),
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
)

private val SfProTypography = Typography(
    // Display styles — large headlines
    displayLarge = TextStyle(fontFamily = SfProDisplay, fontWeight = FontWeight.Light, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = SfProDisplay, fontWeight = FontWeight.Light, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = SfProDisplay, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    // Headline styles
    headlineLarge = TextStyle(fontFamily = SfProDisplay, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = SfProDisplay, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = SfProDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    // Title styles
    titleLarge = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    // Body styles
    bodyLarge = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    // Label styles
    labelLarge = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = SfProText, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

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
        typography = SfProTypography,
        content = content,
    )
}

