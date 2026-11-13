package dev.multitimer.app

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reordering on a small (360dp) phone: drag after a long press, the Move menu after a long press
 * released in place, and the saved order. With -Pscreenshots the dragging state is written to
 * app/build/outputs/reorder/ for review (Robolectric doesn't render the menu's popup window, so the
 * menu is checked through its semantics instead).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h760dp-xxhdpi", application = ScreenshotApp::class)
class ReorderTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: Application

    @Before
    fun seed() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val now = System.currentTimeMillis()
        val min = 60_000L
        val timers = listOf(
            TimerItem(4, "Pasta", 9 * min, 1, null, TimerState.RUNNING, 0, now + 408_000),
            TimerItem(3, "Focaccia in oven", 25 * min, 0, null, TimerState.RUNNING, 0, now + 1_090_000),
            TimerItem(2, "Tea steep", 3 * min, 3, null, TimerState.PAUSED, 105_000, 0),
            TimerItem(1, "Laundry", 45 * min, 2, null, TimerState.PAUSED, 45 * min, 0),
        )
        app.getSharedPreferences("timers", Context.MODE_PRIVATE).edit()
            .putString("list", JSONArray().apply { timers.forEach { put(it.toJson()) } }.toString())
            .putInt("next_id", 5)
            .commit()
        AppSettings.init(app)
        forgetRepository() // the repository is a singleton shared with other tests in this JVM
        TimerRepository.init(app)
    }

    @After
    fun forgetRepository() {
        TimerRepository::class.java.getDeclaredField("app").apply { isAccessible = true }.set(TimerRepository, null)
    }

    private fun labels() = TimerRepository.timers.value.map { it.label }

    private fun savedIds(): List<Int> {
        val raw = app.getSharedPreferences("timers", Context.MODE_PRIVATE).getString("list", null)!!
        val arr = JSONArray(raw)
        return List(arr.length()) { arr.getJSONObject(it).getInt("id") }
    }

    @Test
    fun moveReordersAndSaves() {
        TimerRepository.move(4, 2) // Pasta below Tea steep
        assertEquals(listOf("Focaccia in oven", "Tea steep", "Pasta", "Laundry"), labels())
        assertEquals(listOf(3, 2, 4, 1), savedIds())
        TimerRepository.move(1, -5) // clamped to the top
        assertEquals(listOf(1, 3, 2, 4), savedIds())
        TimerRepository.move(1, 99) // clamped to the bottom
        assertEquals(listOf(3, 2, 4, 1), savedIds())
        TimerRepository.add("New", 60_000, 0)
        assertEquals("New", labels().first()) // new timers still go on top
    }

    @Test
    fun dragAndMoveMenu() {
        var dark by mutableStateOf(false)
        compose.mainClock.autoAdvance = false // the list's clock ticks forever
        compose.setContent { MultiTimerTheme(darkTheme = dark) { TimerScreen(onOpenSettings = {}) } }
        fun settle(ms: Long = 1_000) { Snapshot.sendApplyNotifications(); compose.mainClock.advanceTimeBy(ms) }
        settle()

        for (isDark in listOf(false, true)) {
            dark = isDark
            val theme = if (isDark) "dark" else "light"
            settle()
            val first = labels().first()

            // Long-press the first card's label and drag it down past the next card.
            compose.onNodeWithText(first).performTouchInput { down(center) }
            settle(800) // long press recognized: the card lifts
            repeat(18) { // ~720px: past the middle of the next card (~700px below at xxhdpi)
                compose.onNodeWithText(first).performTouchInput { moveBy(Offset(0f, 40f)) }
                settle(16)
            }
            settle(400)
            compose.onRoot().captureRoboImage("build/outputs/reorder/1-dragging-$theme.png")
            compose.onNodeWithText(first).performTouchInput { up() }
            settle(1_000)
            assertEquals("dragged down one place ($theme)", first, labels()[1])

            // Long-press the last card and release in place: the Move menu opens.
            val last = labels().last()
            compose.onNodeWithText(last).performTouchInput { down(center) }
            settle(800)
            compose.onNodeWithText(last).performTouchInput { up() }
            settle(500)
            compose.onNodeWithText("Move \"$last\"").assertIsDisplayed() // header names the timer
            compose.onNodeWithText("Move down").assertIsNotEnabled() // last card: down and bottom off
            compose.onNodeWithText("Move to bottom").assertIsNotEnabled()
            compose.onNodeWithText("Move to top").performClick()
            settle(1_000)
            assertEquals("moved to top from the menu ($theme)", last, labels().first())
        }
    }
}
