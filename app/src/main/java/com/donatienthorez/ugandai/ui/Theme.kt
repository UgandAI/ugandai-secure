package com.ugandai.ugandai.ui

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color


private val DarkColorPalette = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = DarkGreen,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = LightGrey,
    secondary = DarkGreen,
    onSecondary = LightGrey,
    secondaryContainer = MidGrey,
    onSecondaryContainer = DarkGrey,
    error = Color(0xFFBA1B1B),  // Red for error
    onError = Color(0xFFFFB4A9),
    background = DarkGrey,
    onBackground = LightGrey,
    surface = MidGrey,
    onSurface = Blue,
    inverseSurface = LightGrey,
    inverseOnSurface = DarkGrey,
    surfaceVariant = MidGrey,
    onSurfaceVariant = Blue,
    outline = Blue
)


@Composable
fun ChatGptBotAppTheme(content: @Composable () -> Unit) {
    val useDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        useDynamicColors -> dynamicDarkColorScheme(LocalContext.current)
        else -> DarkColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
