package dev.multitimer.app

import android.app.Application
import dev.multitimer.app.data.TimerRepository

class MultiTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TimerRepository.init(this)
    }
}
