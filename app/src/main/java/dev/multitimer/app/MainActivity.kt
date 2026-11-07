package dev.multitimer.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.ThemeMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.multitimer.app.ui.SettingsScreen
import dev.multitimer.app.ui.TimerScreen
import dev.multitimer.app.ui.theme.MultiTimerTheme

/** The scrims enableEdgeToEdge() uses by default for 3-button navigation bars. */
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by AppSettings.theme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status and navigation bar icons follow the app's theme, not only the phone's.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
                )
                onDispose { }
            }
            MultiTimerTheme(darkTheme = dark) {
                // Survives the recreation that a theme change triggers.
                var showSettings by rememberSaveable { mutableStateOf(false) }
                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    TimerScreen(onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}
