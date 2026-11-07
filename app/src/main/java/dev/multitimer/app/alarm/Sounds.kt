package dev.multitimer.app.alarm

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import dev.multitimer.app.R

/**
 * Alarm sounds are stored as strings:
 * - "app:<name>": one of the app's own sounds ([APP_SOUNDS], files in res/raw).
 * - [PHONE_DEFAULT]: whatever alarm sound the phone is set to (follows later changes).
 * - [SILENT]: no sound, vibrate only.
 * - anything else: a content URI of one of the phone's alarm sounds.
 */
object Sounds {
    const val SILENT = "silent"
    val PHONE_DEFAULT: String = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

    private const val APP_PREFIX = "app:"

    /** Original 8-bit sounds made for Multi Timer (generator: tools/make_sounds.py). */
    data class AppSound(val name: String, @RawRes val raw: Int, @StringRes val title: Int) {
        val id: String get() = APP_PREFIX + name
    }

    // Referencing R.raw here also keeps the files when release builds shrink resources.
    val APP_SOUNDS = listOf(
        AppSound("power_chime", R.raw.power_chime, R.string.sound_power_chime),
        AppSound("pixel_bell", R.raw.pixel_bell, R.string.sound_pixel_bell),
        AppSound("beeper", R.raw.beeper, R.string.sound_beeper),
        AppSound("gentle_rise", R.raw.gentle_rise, R.string.sound_gentle_rise),
        AppSound("jingle", R.raw.jingle, R.string.sound_jingle),
    )

    /** Default for new installs and for anyone who never changed it. */
    val DEFAULT: String = APP_SOUNDS.first().id

    private fun appSound(id: String) = APP_SOUNDS.firstOrNull { it.id == id }

    /** A URI MediaPlayer and Ringtone can play, or null for [SILENT] or an unknown app sound. */
    fun playableUri(context: Context, id: String): Uri? = when {
        id == SILENT -> null
        // android.resource://<package>/<resource id>. Built at play time, never stored, so the
        // id always matches the running build (stored values use the stable "app:<name>").
        id.startsWith(APP_PREFIX) -> appSound(id)?.let { sound ->
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.packageName)
                .appendPath(sound.raw.toString())
                .build()
        }
        else -> id.toUri()
    }

    data class Option(val uri: String, val title: String)

    /**
     * The phone's alarm sounds, one entry per name. Call off the main thread.
     *
     * The media library often holds the same sound more than once (system and device
     * storage, leftovers from updates), each with its own URI, so entries are merged by
     * name. When a duplicate is one of [keep] (sounds already chosen), that copy is kept so
     * the saved choice still shows as selected.
     */
    fun alarmSounds(context: Context, keep: Set<String> = emptySet()): List<Option> = runCatching {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = manager.cursor // managed by RingtoneManager: must not be closed
        val all = buildList {
            while (cursor.moveToNext()) {
                add(
                    Option(
                        uri = manager.getRingtoneUri(cursor.position).toString(),
                        title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty().trim(),
                    )
                )
            }
        }
        all.filter { it.title.isNotEmpty() }
            .groupBy { it.title.lowercase() }
            .values
            .map { copies -> copies.firstOrNull { it.uri in keep } ?: copies.first() }
            .sortedBy { it.title.lowercase() }
    }.getOrDefault(emptyList())

    /** Display name for a stored sound. May query the media store: prefer a background thread. */
    fun title(context: Context, id: String): String {
        appSound(id)?.let { return context.getString(it.title) }
        return when (id) {
            SILENT -> context.getString(R.string.sound_silent)
            PHONE_DEFAULT -> context.getString(R.string.sound_phone_default)
            else -> runCatching { RingtoneManager.getRingtone(context, id.toUri())?.getTitle(context) }
                .getOrNull() ?: context.getString(R.string.sound_unknown)
        }
    }
}

/** Plays one sound at a time for the picker's preview. */
class SoundPreview(private val context: Context) {
    private var current: Ringtone? = null

    fun play(id: String) {
        stop()
        val uri = Sounds.playableUri(context, id) ?: return
        current = runCatching {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }.getOrNull()
    }

    fun stop() {
        current?.stop()
        current = null
    }
}
