package dev.multitimer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * Neutral themes: white (light) and Material's neutral greys (dark), with grey controls.
 * Color is reserved for each timer's own color and the orange of a finished timer.
 *
 * All pairs meet WCAG 2.2 AA: text >= 4.5:1, control outlines and timer colors >= 3:1.
 * `outline` is used for every border and line (cards, dividers, chips, buttons, text
 * fields, menus) and must stay >= 3:1; `outlineVariant` is only the progress-ring track.
 */

private val Light = lightColorScheme(
    primary = Color(0xFF1F1F1F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECECEC),
    onPrimaryContainer = Color(0xFF1F1F1F),
    inversePrimary = Color(0xFFECECEC),
    secondary = Color(0xFF1F1F1F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECECEC),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF1F1F1F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECECEC),
    onTertiaryContainer = Color(0xFF1F1F1F),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFF5F6368),
    surfaceTint = Color(0xFFFFFFFF), // no tint on elevated surfaces
    inverseSurface = Color(0xFF1F1F1F),
    inverseOnSurface = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDADADA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFECECEC),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFE0E0E0),
    error = Color(0xFFB53C0B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4D6),
    onErrorContainer = Color(0xFF3A1200),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE3E3E3),
    onPrimary = Color(0xFF1F1F1F),
    primaryContainer = Color(0xFF2E2E2E),
    onPrimaryContainer = Color(0xFFE3E3E3),
    inversePrimary = Color(0xFF2E2E2E),
    secondary = Color(0xFFE3E3E3),
    onSecondary = Color(0xFF1F1F1F),
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color(0xFFE3E3E3),
    tertiary = Color(0xFFE3E3E3),
    onTertiary = Color(0xFF1F1F1F),
    tertiaryContainer = Color(0xFF2E2E2E),
    onTertiaryContainer = Color(0xFFE3E3E3),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceTint = Color(0xFF121212),
    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF121212),
    surfaceBright = Color(0xFF383838),
    surfaceDim = Color(0xFF121212),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFFF9A6B),
    onError = Color(0xFF1E1E1E),
    errorContainer = Color(0xFF4A2414),
    onErrorContainer = Color(0xFFFDE4D6),
)

/** Light-mode timer colors: brighter, each >= 3:1 on white. */
private val TimerColorsLight = listOf(
    Color(0xFF0F9AA0), // teal
    Color(0xFFD27206), // amber
    Color(0xFF8B5CF6), // violet
    Color(0xFF1F9E44), // green
    Color(0xFFE8476A), // rose
)

/** Dark-mode timer colors: pastel, each >= 8:1 on the dark card. */
private val TimerColorsDark = listOf(
    Color(0xFF7FD3D3),
    Color(0xFFF2B85B),
    Color(0xFFC3B0FF),
    Color(0xFF8FD694),
    Color(0xFFFF9FB0),
)

const val TIMER_COLOR_COUNT = 5

/** Whether the app is showing its dark theme (may differ from the phone's on Android 11). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun timerColor(index: Int): Color {
    val list = if (LocalDarkTheme.current) TimerColorsDark else TimerColorsLight
    return list[index.mod(list.size)]
}

/**
 * [darkTheme] follows the app's night mode, which the in-app Theme menu sets through
 * UiModeManager (see AppSettings). Set [dynamicColor] to true for wallpaper colors.
 */
@Composable
fun MultiTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // Android 12+
    val scheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
