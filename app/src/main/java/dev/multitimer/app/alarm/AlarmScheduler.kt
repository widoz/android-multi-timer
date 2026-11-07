package dev.multitimer.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.multitimer.app.data.TimerItem

/** One exact alarm per running timer, keyed by the timer id. */
object AlarmScheduler {
    const val EXTRA_TIMER_ID = "timer_id"

    fun schedule(context: Context, timer: TimerItem) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, timer.id)
        // Android 11: no permission needed. 12: SCHEDULE_EXACT_ALARM (user can revoke).
        // 13+: USE_EXACT_ALARM, granted at install. Fall back to inexact if not allowed.
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endAt, pi)
        }
    }

    fun cancel(context: Context, timerId: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, timerId))
    }

    private fun pendingIntent(context: Context, timerId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            timerId,
            Intent(context, TimerAlarmReceiver::class.java).putExtra(EXTRA_TIMER_ID, timerId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
