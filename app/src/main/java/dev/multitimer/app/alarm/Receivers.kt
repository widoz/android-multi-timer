package dev.multitimer.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.multitimer.app.data.TimerRepository

/** Fired by AlarmManager when a timer reaches zero. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerRepository.init(context)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_TIMER_ID, -1)
        if (id >= 0) TimerRepository.finish(id)
    }
}

/** Buttons on the notifications: pause/resume, +1 min, reset/stop, stop all ringing. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerRepository.init(context)
        if (intent.action == ACTION_STOP_ALL) {
            TimerRepository.stopRinging()
            return
        }
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_TIMER_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_TOGGLE -> TimerRepository.toggle(id)
            ACTION_ADD_MINUTE -> TimerRepository.addMinute(id)
            ACTION_RESET -> TimerRepository.reset(id)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "dev.multitimer.app.action.TOGGLE"
        const val ACTION_ADD_MINUTE = "dev.multitimer.app.action.ADD_MINUTE"
        const val ACTION_RESET = "dev.multitimer.app.action.RESET"
        const val ACTION_STOP_ALL = "dev.multitimer.app.action.STOP_ALL"
    }
}

/** Alarms and notifications are cleared on reboot or update; put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            TimerRepository.init(context)
            TimerRepository.reconcile()
        }
    }
}
