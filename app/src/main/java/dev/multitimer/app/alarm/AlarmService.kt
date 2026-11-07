package dev.multitimer.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerRepository
import dev.multitimer.app.data.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Plays the alarm while at least one timer has finished, like a clock app does.
 *
 * Notification sounds can be muted by the phone's sound mode, notification settings or
 * Do Not Disturb, so the sound is played here instead, on the alarm stream (alarm volume),
 * looping until every finished timer is stopped. The service shows one "ringing"
 * notification with Stop / +1 min, and stops itself when nothing is ringing.
 *
 * Foreground service type "systemExempted" is the type Android 14+ reserves for alarm
 * apps holding USE_EXACT_ALARM; exact alarms may start it from the background.
 */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private var player: MediaPlayer? = null
    private var playingUri: String? = null
    private var shownIds: List<Int>? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TimerRepository.init(this)
        // Must go to the foreground right away, even if nothing rings any more.
        showForeground(ringing(TimerRepository.timers.value))
        if (observer == null) {
            observer = scope.launch {
                TimerRepository.timers.collect { update(ringing(it)) }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        active = false
        scope.cancel()
        stopPlayback()
        super.onDestroy()
    }

    private fun ringing(timers: List<TimerItem>) = timers.filter { it.state == TimerState.FINISHED }

    private fun update(ringing: List<TimerItem>) {
        if (ringing.isEmpty()) {
            stopPlayback()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (ringing.map { it.id } != shownIds) showForeground(ringing)
        // The newest timer in the list decides the sound when several ring together.
        val sound = ringing.first().soundUri ?: AppSettings.defaultSound.value
        if (sound != playingUri) play(sound)
    }

    private fun showForeground(ringing: List<TimerItem>) {
        shownIds = ringing.map { it.id }
        val notification = TimerNotifier.ringing(this, ringing)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun play(uri: String) {
        stopPlayback()
        playingUri = uri
        if (uri == Sounds.SILENT) return // vibration comes from the notification
        val candidates = listOfNotNull(
            if (uri == Sounds.PHONE_DEFAULT) null else uri,
            Sounds.PHONE_DEFAULT,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString(),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString(),
        ).distinct()
        for (candidate in candidates) {
            val started = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(ALARM_ATTRIBUTES)
                    setDataSource(this@AlarmService, candidate.toUri())
                    isLooping = true
                    prepare()
                    start()
                }
            }.onFailure { Log.w(TAG, "Cannot play $candidate", it) }.getOrNull()
            if (started != null) {
                player = started
                requestFocus()
                return
            }
        }
    }

    private fun stopPlayback() {
        player?.runCatching { stop(); release() }
        player = null
        playingUri = null
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    /** Pauses or lowers music while the alarm rings. */
    private fun requestFocus() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(ALARM_ATTRIBUTES)
            .build()
        getSystemService(AudioManager::class.java).requestAudioFocus(request)
        focusRequest = request
    }

    companion object {
        private const val TAG = "AlarmService"

        /** Far from timer ids, which count up from 1. */
        const val NOTIFICATION_ID = Int.MAX_VALUE

        private val ALARM_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** True while the service is running (or was just asked to start). */
        @Volatile
        var active: Boolean = false
            private set

        /** Starts ringing. Returns false if Android refused to start the service. */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java))
            active = true
            true
        } catch (e: Exception) { // e.g. ForegroundServiceStartNotAllowedException
            Log.w(TAG, "Cannot start the alarm service", e)
            false
        }
    }
}
