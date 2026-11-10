package dev.multitimer.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.ui.FullScreenTimer
import dev.multitimer.app.ui.theme.MultiTimerTheme
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the full-screen view for review: running, with the finished-timer tray, and the
 * full-screen timer itself finished. Record with -Pscreenshots (writes app/build/outputs/full-screen).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h820dp-xxhdpi", application = ScreenshotApp::class)
class FullScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun settings() = AppSettings.init(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun fullScreenStates() {
        val now = System.currentTimeMillis()
        val min = 60_000L
        val focaccia = TimerItem(3, "Focaccia in oven", 25 * min, 0, null, TimerState.RUNNING, 0, now + 1_090_000)
        val ended = listOf(
            TimerItem(2, "Tea steep", 3 * min, 3, null, TimerState.FINISHED, 0, now - 4_000),
            TimerItem(4, "Pasta", 9 * min, 1, "app:pixel_bell", TimerState.FINISHED, 0, now - 9_000),
            TimerItem(1, "Laundry", 45 * min, 2, null, TimerState.FINISHED, 0, now - 15_000),
        )
        var dark by mutableStateOf(false)
        var main by mutableStateOf(focaccia)
        var tray by mutableStateOf(emptyList<TimerItem>())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MultiTimerTheme(darkTheme = dark) { FullScreenTimer(main, tray, now, onExit = {}) }
        }
        fun shot(name: String) {
            Snapshot.sendApplyNotifications() // apply the test's state changes before rendering
            compose.mainClock.advanceTimeBy(2_000)
            compose.onRoot().captureRoboImage("build/outputs/full-screen/$name.png")
        }
        for (isDark in listOf(false, true)) {
            dark = isDark
            val theme = if (isDark) "dark" else "light"
            main = focaccia; tray = emptyList(); shot("1-running-$theme")
            tray = ended; shot("2-tray-$theme")
            main = focaccia.copy(state = TimerState.FINISHED, endAt = now - 2_000); tray = emptyList(); shot("3-own-finished-$theme")
        }
    }
}
