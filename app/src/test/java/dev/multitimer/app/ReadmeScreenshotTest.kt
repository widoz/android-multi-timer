package dev.multitimer.app

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerRepository
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.ui.TimerScreen
import dev.multitimer.app.ui.theme.MultiTimerTheme
import org.json.JSONArray
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Plain Application, so the test seeds data before the repository loads it. */
class ScreenshotApp : Application()

/**
 * Renders the real timer list with sample timers for the README, in light and dark theme.
 * Record with: ./gradlew testDebugUnitTest -Pscreenshots  (writes docs/assets/app-*.png)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h820dp-xxhdpi", application = ScreenshotApp::class)
class ReadmeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun seedTimers() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val now = System.currentTimeMillis()
        val min = 60_000L
        val timers = listOf( // newest first, as the app lists them
            TimerItem(4, "Pasta", 9 * min, 1, "app:pixel_bell", TimerState.RUNNING, 0, now + 408_000),
            TimerItem(3, "Focaccia in oven", 25 * min, 0, null, TimerState.RUNNING, 0, now + 1_090_000),
            TimerItem(2, "Tea steep", 3 * min, 3, null, TimerState.PAUSED, 105_000, 0),
            TimerItem(1, "Laundry", 45 * min, 2, null, TimerState.PAUSED, 45 * min, 0),
        )
        app.getSharedPreferences("timers", Context.MODE_PRIVATE).edit()
            .putString("list", JSONArray().apply { timers.forEach { put(it.toJson()) } }.toString())
            .putInt("next_id", 5)
            .commit()
        AppSettings.init(app)
        TimerRepository.init(app)
    }

    @Test
    fun timerList() {
        var dark by mutableStateOf(false)
        compose.mainClock.autoAdvance = false // the list's clock ticks forever
        compose.setContent {
            MultiTimerTheme(darkTheme = dark) { TimerScreen(onOpenSettings = {}) }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("../docs/assets/app-light.png")

        dark = true
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("../docs/assets/app-dark.png")
    }
}
