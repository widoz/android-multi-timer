package dev.multitimer.app

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiTimerTheme {
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
