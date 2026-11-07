package dev.multitimer.app

import android.app.Application
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.TimerRepository

class MultiTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        TimerRepository.init(this)
    }
}
