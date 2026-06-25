package com.tatamotors.hcvcalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tata Motors brand palette
val TataBlue = Color(0xFF00529C)
val TataBlueDark = Color(0xFF003A6F)
val TataBlueLight = Color(0xFFE3F0FB)
val AccentGreen = Color(0xFF1B873B)
val AccentGreenLight = Color(0xFFE6F4EA)
val WarnRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = TataBlue,
    onPrimary = Color.White,
    primaryContainer = TataBlueLight,
    onPrimaryContainer = TataBlueDark,
    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = AccentGreenLight,
    onSecondaryContainer = Color(0xFF0B3D1C),
    surface = Color(0xFFFAFBFD),
    background = Color(0xFFF2F5F9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB5E6),
    onPrimary = Color(0xFF002F55),
    primaryContainer = Color(0xFF004478),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFF8CD5A0),
    onSecondary = Color(0xFF003917)
)

@Composable
fun TataHCVTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
