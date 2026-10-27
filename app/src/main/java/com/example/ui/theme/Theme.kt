package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenDark,
    onPrimary = Color(0xFF033907),
    primaryContainer = Color(0xFF145214),
    onPrimaryContainer = Color(0xFFACF5A3),
    secondary = SunbirdAmberDark,
    onSecondary = Color(0xFF452B00),
    tertiary = ZambeziTealDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB1F3A8),
    onPrimaryContainer = Color(0xFF002203),
    secondary = SunbirdAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB1),
    onSecondaryContainer = Color(0xFF2C1700),
    tertiary = ZambeziTeal,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
