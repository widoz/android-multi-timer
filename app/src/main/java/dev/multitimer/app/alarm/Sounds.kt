package dev.multitimer.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.core.net.toUri
import android.provider.Settings
import dev.multitimer.app.R
import java.security.MessageDigest

/**
 * Alarm sounds are stored as URI strings. Two special values:
 * - [PHONE_DEFAULT]: whatever alarm sound the phone is set to (follows later changes).
 * - [SILENT]: no sound, vibrate only.
 */
object Sounds {
    const val SILENT = "silent"
    val PHONE_DEFAULT: String = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

    private val ALARM_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    data class Option(val uri: String, val title: String)

    /** The phone's alarm sounds, as listed by the system. Call off the main thread. */
    fun alarmSounds(context: Context): List<Option> = runCatching {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = manager.cursor // managed by RingtoneManager: must not be closed
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Option(
                        uri = manager.getRingtoneUri(cursor.position).toString(),
                        title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX),
                    )
                )
            }
        }.distinctBy { it.uri }
    }.getOrDefault(emptyList())

    /** Display name for a stored sound. May query the media store: prefer a background thread. */
    fun title(context: Context, uri: String): String = when (uri) {
        SILENT -> context.getString(R.string.sound_silent)
        PHONE_DEFAULT -> context.getString(R.string.sound_phone_default)
        else -> runCatching { RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context) }
            .getOrNull() ?: context.getString(R.string.sound_unknown)
    }

    /**
     * A notification channel's sound cannot change after it is created (Android 8+), so each
     * sound gets its own "Finished timers · <sound>" channel, created the first time it rings.
     */
    fun alarmChannelId(context: Context, uri: String): String {
        val id = "alarm_" + if (uri == SILENT) "silent" else sha1(uri).take(16)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(
                id,
                context.getString(R.string.channel_alarm_named, title(context, uri)),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                if (uri == SILENT) setSound(null, null) else setSound(uri.toUri(), ALARM_ATTRIBUTES)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600)
            }
            nm.createNotificationChannel(channel)
        }
        return id
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
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
