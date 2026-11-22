package com.nxteam.irunner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nxteam.irunner.data.settings.ThemeMode

private val NxDarkColors = darkColorScheme(
    primary = Color(0xFF9D86FF),
    onPrimary = Color(0xFF1A0B5C),
    primaryContainer = Color(0xFF3A2C8C),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFF6BB6FF),
    onSecondary = Color(0xFF00315C),
    secondaryContainer = Color(0xFF1C3A66),
    onSecondaryContainer = Color(0xFFD3E4FF),
    background = Color(0xFF0B1026),
    onBackground = Color(0xFFE4E5F5),
    surface = Color(0xFF0B1026),
    onSurface = Color(0xFFE4E5F5),
    surfaceVariant = Color(0xFF1B2246),
    onSurfaceVariant = Color(0xFFC4C6E0),
    surfaceContainer = Color(0xFF141A38),
    surfaceContainerHigh = Color(0xFF1B2246),
    error = Color(0xFFFFB4AB),
)

private val NxLightColors = lightColorScheme(
    primary = Color(0xFF5B3FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1A0B5C),
    secondary = Color(0xFF1E6FD0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Color(0xFF001C3A),
    background = Color(0xFFF6F5FF),
    onBackground = Color(0xFF1A1B2E),
    surface = Color(0xFFF6F5FF),
    onSurface = Color(0xFF1A1B2E),
)

@Composable
fun NxIRunnerTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        darkTheme -> NxDarkColors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode == ThemeMode.LIGHT ->
            dynamicLightColorScheme(LocalContext.current)
        else -> NxLightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
