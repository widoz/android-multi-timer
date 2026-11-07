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
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.multitimer.app.MainActivity
import dev.multitimer.app.R
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.data.formatClock
import dev.multitimer.app.data.formatDuration
import java.util.Date

/**
 * One notification per timer, using the timer id as notification id:
 * - running: silent, ongoing, with a live system countdown (no service needed)
 * - paused: silent, ongoing, with Resume
 * - finished: high priority, alarm sound repeating until stopped
 */
object TimerNotifier {
    private const val CHANNEL_RUNNING = "running"
    private const val LEGACY_CHANNEL_ALARM = "alarm"

    fun createChannels(context: Context) {
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.channel_running),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_running_desc)
            setShowBadge(false)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(running)
        // 0.1.0 used one "alarm" channel; finished timers now use a channel per sound.
        nm.deleteNotificationChannel(LEGACY_CHANNEL_ALARM)
    }

    fun sync(context: Context, t: TimerItem) {
        when {
            t.state == TimerState.FINISHED -> post(context, t.id, finished(context, t))
            t.state == TimerState.RUNNING -> post(context, t.id, running(context, t))
            t.isReady -> cancel(context, t.id)
            else -> post(context, t.id, paused(context, t))
        }
    }

    fun cancel(context: Context, timerId: Int) {
        NotificationManagerCompat.from(context).cancel(timerId)
    }

    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

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
        base(context, t, Sounds.alarmChannelId(context, t.soundUri ?: AppSettings.defaultSound.value))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentText(context.getString(R.string.notif_done, formatDuration(t.totalMs)))
            .addAction(0, context.getString(R.string.stop), action(context, t.id, TimerActionReceiver.ACTION_RESET))
            .addAction(0, context.getString(R.string.add_minute), action(context, t.id, TimerActionReceiver.ACTION_ADD_MINUTE))
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT } // keep ringing until handled

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
