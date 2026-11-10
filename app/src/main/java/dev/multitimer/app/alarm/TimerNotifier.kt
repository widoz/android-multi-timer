package dev.multitimer.app.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.multitimer.app.MainActivity
import dev.multitimer.app.R
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.data.formatClock
import dev.multitimer.app.data.formatDuration
import java.util.Date

/**
 * One notification per timer, using the timer id as notification id:
 * - running: silent, ongoing, with a live system countdown (no service needed)
 * - paused: silent, ongoing, with Resume
 * - finished: covered by AlarmService's single "ringing" notification, which plays the
 *   sound itself. Only if the service could not start does each finished timer get its
 *   own (vibrating) notification.
 */
object TimerNotifier {
    private const val CHANNEL_RUNNING = "running"
    private const val CHANNEL_RINGING = "ringing"
    private const val CHANNEL_RINGING_QUIET = "ringing_full_screen"
    private const val LEGACY_CHANNEL_ALARM = "alarm"
    private const val LEGACY_SOUND_CHANNEL_PREFIX = "alarm_"

    fun createChannels(context: Context) {
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.channel_running),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_running_desc)
            setShowBadge(false)
        }
        // No sound here: AlarmService plays it on the alarm stream. Vibrates until stopped.
        val ringing = NotificationChannel(
            CHANNEL_RINGING,
            context.getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        // Same, but no heads-up pop-up: used while a full-screen timer is visible.
        val ringingQuiet = NotificationChannel(
            CHANNEL_RINGING_QUIET,
            context.getString(R.string.channel_alarm_full_screen),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_alarm_full_screen_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600)
        }
        nm.createNotificationChannels(listOf(running, ringing, ringingQuiet))
        // Older versions played the sound through notification channels: remove them.
        nm.notificationChannels
            .filter { it.id == LEGACY_CHANNEL_ALARM || it.id.startsWith(LEGACY_SOUND_CHANNEL_PREFIX) }
            .forEach { nm.deleteNotificationChannel(it.id) }
    }

    fun sync(context: Context, t: TimerItem) {
        when {
            t.state == TimerState.FINISHED ->
                if (AlarmService.active) cancel(context, t.id) else post(context, t.id, finished(context, t))
            t.state == TimerState.RUNNING -> post(context, t.id, running(context, t))
            t.isReady -> cancel(context, t.id)
            else -> post(context, t.id, paused(context, t))
        }
    }

    fun cancel(context: Context, timerId: Int) {
        NotificationManagerCompat.from(context).cancel(timerId)
    }

    /** Android 13+: the POST_NOTIFICATIONS permission. Older: the app's notification switch. */
    fun canPost(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // checked by canPost()
    private fun post(context: Context, id: Int, n: Notification) {
        if (canPost(context)) NotificationManagerCompat.from(context).notify(id, n)
    }

    private fun base(context: Context, t: TimerItem, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(t.label)
            .setContentIntent(openApp(context))
            .setOngoing(true)

    private fun running(context: Context, t: TimerItem): Notification {
        val endsAt = DateFormat.getTimeFormat(context).format(Date(t.endAt))
        return base(context, t, CHANNEL_RUNNING)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(t.endAt)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentText(context.getString(R.string.status_ends, endsAt))
            .addAction(0, context.getString(R.string.pause), action(context, t.id, TimerActionReceiver.ACTION_TOGGLE))
            .addAction(0, context.getString(R.string.add_minute), action(context, t.id, TimerActionReceiver.ACTION_ADD_MINUTE))
            .addAction(0, context.getString(R.string.reset), action(context, t.id, TimerActionReceiver.ACTION_RESET))
            .build()
    }

    private fun paused(context: Context, t: TimerItem): Notification =
        base(context, t, CHANNEL_RUNNING)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentText(context.getString(R.string.notif_paused, formatClock(t.remainingMs)))
            .addAction(0, context.getString(R.string.resume), action(context, t.id, TimerActionReceiver.ACTION_TOGGLE))
            .addAction(0, context.getString(R.string.reset), action(context, t.id, TimerActionReceiver.ACTION_RESET))
            .build()

    private fun finished(context: Context, t: TimerItem): Notification =
        base(context, t, CHANNEL_RINGING)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentText(context.getString(R.string.notif_done, formatDuration(t.totalMs)))
            .addAction(0, context.getString(R.string.stop), action(context, t.id, TimerActionReceiver.ACTION_RESET))
            .addAction(0, context.getString(R.string.add_minute), action(context, t.id, TimerActionReceiver.ACTION_ADD_MINUTE))
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT } // keep ringing until handled

    /** AlarmService's notification: one for all ringing timers. */
    fun ringing(context: Context, timers: List<TimerItem>, quiet: Boolean = FullScreenState.visible): Notification {
        val single = timers.singleOrNull()
        val title = if (single != null) {
            context.getString(R.string.ringing_one, single.label)
        } else {
            context.resources.getQuantityString(R.plurals.ringing_many, timers.size, timers.size)
        }
        val text = if (single != null) {
            context.getString(R.string.notif_done, formatDuration(single.totalMs))
        } else {
            timers.joinToString(", ") { it.label }
        }
        val builder = NotificationCompat.Builder(context, if (quiet) CHANNEL_RINGING_QUIET else CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (quiet) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .addAction(
                0,
                context.getString(if (single != null) R.string.stop else R.string.stop_all),
                action(context, STOP_ALL_REQUEST, TimerActionReceiver.ACTION_STOP_ALL),
            )
        if (single != null) {
            builder.addAction(
                0,
                context.getString(R.string.add_minute),
                action(context, single.id, TimerActionReceiver.ACTION_ADD_MINUTE),
            )
        }
        return builder.build().apply { flags = flags or Notification.FLAG_INSISTENT } // keep vibrating
    }

    private const val STOP_ALL_REQUEST = -1

    private fun action(context: Context, timerId: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            timerId,
            Intent(context, TimerActionReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_TIMER_ID, timerId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
}
