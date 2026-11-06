package dev.multitimer.app.data

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The in-app theme choice. It is applied with UiModeManager.setApplicationNightMode
 * (Android 12+), so the whole app follows it: Compose's isSystemInDarkTheme(), the
 * status bar icons, the startup window and values-night resources. The system
 * persists it too; we also keep it to show the current choice in the menu.
 */
@SuppressLint("StaticFieldLeak") // holds the Application context only
object ThemeSettings {
    private const val PREFS = "settings"
    private const val KEY_THEME = "theme_mode"

    private lateinit var app: Context
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        val saved = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        _mode.value = runCatching { ThemeMode.valueOf(saved ?: "") }.getOrDefault(ThemeMode.SYSTEM)
        applyNightMode(_mode.value)
    }

    fun set(mode: ThemeMode) {
        if (mode == _mode.value) return
        _mode.value = mode
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_THEME, mode.name) }
        applyNightMode(mode)
    }

    private fun applyNightMode(mode: ThemeMode) {
        app.getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        )
    }
}
