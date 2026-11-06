package dev.multitimer.app

import android.app.Application
import dev.multitimer.app.data.ThemeSettings
import dev.multitimer.app.data.TimerRepository

class MultiTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeSettings.init(this)
        TimerRepository.init(this)
    }
}
