package dev.multitimer.app.alarm

/**
 * Whether a full-screen timer is on screen right now (set by the UI while it is resumed).
 * The alarm service then posts its ringing notification without a heads-up pop-up, because
 * the full-screen tray already shows the finished timers.
 */
object FullScreenState {
    @Volatile
    var visible: Boolean = false
}
