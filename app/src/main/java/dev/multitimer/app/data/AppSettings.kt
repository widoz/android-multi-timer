package dev.multitimer.app.data

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import dev.multitimer.app.alarm.Sounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App-wide settings: theme and the default alarm sound.
 *
 * On Android 12+ the theme is applied with UiModeManager.setApplicationNightMode, so the
 * whole app follows it, startup window included. On Android 11 MainActivity applies it to
 * the Compose theme and system bars; the startup window may briefly show the phone's theme.
 *
 * The default sound is used by every timer whose own sound is null. It is Power chime
 * ([Sounds.DEFAULT]) unless the user picked another one. Before 0.4.0 the built-in default was
 * the phone's alarm sound; it was never saved, so those installs move to Power chime too.
 */
@SuppressLint("StaticFieldLeak") // holds the Application context only
object AppSettings {
    private const val PREFS = "settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_DEFAULT_SOUND = "default_sound"

    private lateinit var app: Context
    private val _theme = MutableStateFlow(ThemeMode.SYSTEM)
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()
    private val _defaultSound = MutableStateFlow(Sounds.DEFAULT)
    val defaultSound: StateFlow<String> = _defaultSound.asStateFlow()

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _theme.value = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM)
        _defaultSound.value = prefs.getString(KEY_DEFAULT_SOUND, null) ?: Sounds.DEFAULT
        applyNightMode(_theme.value)
    }

    fun setTheme(mode: ThemeMode) {
        if (mode == _theme.value) return
        _theme.value = mode
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_THEME, mode.name) }
        applyNightMode(mode)
    }

    fun setDefaultSound(uri: String) {
        _defaultSound.value = uri
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_DEFAULT_SOUND, uri) }
    }

    /** Android 12+ only. On Android 11 the Compose theme applies the choice (see MainActivity). */
    private fun applyNightMode(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        app.getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        )
    }
}
