package dev.multitimer.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.core.net.toUri
import android.provider.Settings
import dev.multitimer.app.R

/**
 * Alarm sounds are stored as URI strings. Two special values:
 * - [PHONE_DEFAULT]: whatever alarm sound the phone is set to (follows later changes).
 * - [SILENT]: no sound, vibrate only.
 */
object Sounds {
    const val SILENT = "silent"
    val PHONE_DEFAULT: String = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

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
    fun title(context: Context, uri: String): String = when (uri) {
        SILENT -> context.getString(R.string.sound_silent)
        PHONE_DEFAULT -> context.getString(R.string.sound_phone_default)
        else -> runCatching { RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context) }
            .getOrNull() ?: context.getString(R.string.sound_unknown)
    }
}

/** Plays one sound at a time for the picker's preview. */
class SoundPreview(private val context: Context) {
    private var current: Ringtone? = null

    fun play(uri: String) {
        stop()
        if (uri == Sounds.SILENT) return
        current = runCatching {
            RingtoneManager.getRingtone(context, uri.toUri())?.apply {
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
