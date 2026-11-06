package dev.multitimer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF0F6B72),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDECEB),
    onPrimaryContainer = Color(0xFF002022),
    secondaryContainer = Color(0xFFCDECEB),
    onSecondaryContainer = Color(0xFF002022),
    background = Color(0xFFF6FAFB),
    onBackground = Color(0xFF142126),
    surface = Color(0xFFF6FAFB),
    onSurface = Color(0xFF142126),
    onSurfaceVariant = Color(0xFF5D6F76),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF4F5),
    surfaceContainerHigh = Color(0xFFE6EEF0),
    surfaceContainerHighest = Color(0xFFDDE7EA),
    outline = Color(0xFFB9C9CE),
    outlineVariant = Color(0xFFD5E0E3),
    error = Color(0xFFC2410C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4D6),
    onErrorContainer = Color(0xFF3A1200),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD3D3),
    onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF1D4447),
    onPrimaryContainer = Color(0xFFCDECEB),
    secondaryContainer = Color(0xFF1D4447),
    onSecondaryContainer = Color(0xFFCDECEB),
    background = Color(0xFF111A1C),
    onBackground = Color(0xFFE2EEF0),
    surface = Color(0xFF111A1C),
    onSurface = Color(0xFFE2EEF0),
    onSurfaceVariant = Color(0xFF98ABB0),
    surfaceContainerLow = Color(0xFF1A2528),
    surfaceContainer = Color(0xFF1E2A2D),
    surfaceContainerHigh = Color(0xFF223033),
    surfaceContainerHighest = Color(0xFF2A393D),
    outline = Color(0xFF4A5E63),
    outlineVariant = Color(0xFF2A393D),
    error = Color(0xFFFF9A6B),
    onError = Color(0xFF3A1200),
    errorContainer = Color(0xFF4A2414),
    onErrorContainer = Color(0xFFFDE4D6),
)

private val TimerColorsLight = listOf(
    Color(0xFF0F6B72), Color(0xFF9A5B00), Color(0xFF6B4FB3), Color(0xFF2F7D32), Color(0xFFB3364F),
)
private val TimerColorsDark = listOf(
    Color(0xFF7FD3D3), Color(0xFFF2B85B), Color(0xFFC3B0FF), Color(0xFF8FD694), Color(0xFFFF9FB0),
)

const val TIMER_COLOR_COUNT = 5

@Composable
fun timerColor(index: Int): Color {
    val list = if (isSystemInDarkTheme()) TimerColorsDark else TimerColorsLight
    return list[index.mod(list.size)]
}

/** Set [dynamicColor] to true to follow the wallpaper colors (Material You). */
@Composable
fun MultiTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
