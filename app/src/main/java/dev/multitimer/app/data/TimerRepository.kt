package dev.multitimer.app.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import dev.multitimer.app.R
import dev.multitimer.app.alarm.AlarmScheduler
import dev.multitimer.app.alarm.AlarmService
import dev.multitimer.app.alarm.TimerNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Single source of truth for all timers. Every change is persisted, and the matching
 * alarm and notification are updated, so each timer behaves the same whether the app
 * is open, in the background, or was killed.
 */
@SuppressLint("StaticFieldLeak") // holds the Application context only
object TimerRepository {
    private const val PREFS = "timers"
    private const val KEY_LIST = "list"
    private const val KEY_NEXT_ID = "next_id"
    const val MINUTE = 60_000L

    private lateinit var app: Context
    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers.asStateFlow()
    private var nextId = 1

    @Synchronized
    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        TimerNotifier.createChannels(app)
        load()
        reconcile()
    }

    /** Re-arms alarms and notifications, and finishes timers that ended while we were away. */
    @Synchronized
    fun reconcile() {
        val now = System.currentTimeMillis()
        for (t in _timers.value) {
            if (t.state == TimerState.RUNNING && t.endAt <= now) finish(t.id) else sideEffects(t)
        }
    }

    @Synchronized
    fun add(label: String, totalMs: Long, colorIndex: Int, soundUri: String? = null) {
        val id = nextId++
        val name = label.trim().ifEmpty { app.getString(R.string.default_label, id) }
        val t = TimerItem(
            id = id,
            label = name,
            totalMs = totalMs,
            colorIndex = colorIndex,
            soundUri = soundUri,
            state = TimerState.RUNNING,
            remainingMs = totalMs,
            endAt = System.currentTimeMillis() + totalMs,
        )
        _timers.value = listOf(t) + _timers.value
        save()
        sideEffects(t)
    }

    fun toggle(id: Int) {
        when (get(id)?.state) {
            TimerState.RUNNING -> pause(id)
            TimerState.PAUSED -> start(id)
            TimerState.FINISHED -> reset(id)
            null -> Unit
        }
    }

    fun start(id: Int) = update(id) {
        if (it.state == TimerState.PAUSED && it.remainingMs > 0) {
            it.copy(state = TimerState.RUNNING, endAt = System.currentTimeMillis() + it.remainingMs)
        } else it
    }

    fun pause(id: Int) = update(id) {
        if (it.state == TimerState.RUNNING) {
            it.copy(state = TimerState.PAUSED, remainingMs = it.remainingAt(System.currentTimeMillis()))
        } else it
    }

    fun reset(id: Int) = update(id) {
        it.copy(state = TimerState.PAUSED, remainingMs = it.totalMs, endAt = 0)
    }

    fun addMinute(id: Int) = update(id) {
        when (it.state) {
            TimerState.RUNNING -> it.copy(totalMs = it.totalMs + MINUTE, endAt = it.endAt + MINUTE)
            TimerState.PAUSED -> it.copy(totalMs = it.totalMs + MINUTE, remainingMs = it.remainingMs + MINUTE)
            TimerState.FINISHED -> it.copy(
                state = TimerState.RUNNING,
                totalMs = it.totalMs + MINUTE,
                remainingMs = MINUTE,
                endAt = System.currentTimeMillis() + MINUTE,
            )
        }
    }

    /** Removes a minute. Ignored once a minute or less is left, so it never ends a timer. */
    fun subtractMinute(id: Int) = update(id) {
        val left = it.remainingAt(System.currentTimeMillis())
        val total = maxOf(MINUTE, it.totalMs - MINUTE)
        when {
            it.state == TimerState.FINISHED || left <= MINUTE -> it
            it.state == TimerState.RUNNING -> it.copy(totalMs = total, endAt = it.endAt - MINUTE)
            else -> it.copy(totalMs = total, remainingMs = it.remainingMs - MINUTE)
        }
    }

    /**
     * Changes label, color, sound and duration. Label, color and sound apply in place. A new duration
     * starts the timer over: a running timer restarts from the new time, any other
     * timer becomes ready at the new time.
     */
    fun edit(id: Int, label: String, totalMs: Long, colorIndex: Int, soundUri: String?) = update(id) {
        val renamed = it.copy(label = label.trim().ifEmpty { it.label }, colorIndex = colorIndex, soundUri = soundUri)
        when {
            totalMs == it.totalMs -> renamed
            it.state == TimerState.RUNNING -> renamed.copy(
                totalMs = totalMs,
                remainingMs = totalMs,
                endAt = System.currentTimeMillis() + totalMs,
            )
            else -> renamed.copy(state = TimerState.PAUSED, totalMs = totalMs, remainingMs = totalMs, endAt = 0)
        }
    }

    /** Called by the alarm, or by the UI if it notices first. Safe to call twice. */
    fun finish(id: Int) = update(id) {
        if (it.state == TimerState.RUNNING) it.copy(state = TimerState.FINISHED, remainingMs = 0) else it
    }

    fun finishDue(now: Long) {
        _timers.value.filter { it.state == TimerState.RUNNING && it.endAt <= now }.forEach { finish(it.id) }
    }

    /** Stops every ringing timer (the ringing notification's Stop / Stop all). */
    fun stopRinging() {
        _timers.value.filter { it.state == TimerState.FINISHED }.forEach { reset(it.id) }
    }

    fun pauseAll() {
        _timers.value.filter { it.state == TimerState.RUNNING }.forEach { pause(it.id) }
    }

    /** A deleted timer and where it was, so Undo can put it back. */
    data class Deleted(val timer: TimerItem, val index: Int)

    @Synchronized
    fun delete(id: Int): Deleted? {
        val index = _timers.value.indexOfFirst { it.id == id }
        if (index < 0) return null
        val timer = _timers.value[index]
        _timers.value = _timers.value.filterNot { it.id == id }
        save()
        AlarmScheduler.cancel(app, id)
        TimerNotifier.cancel(app, id)
        return Deleted(timer, index)
    }

    /** Undo for [delete]. A running timer kept counting while it was gone. */
    @Synchronized
    fun restore(deleted: Deleted) {
        val t = deleted.timer
        if (get(t.id) != null) return
        val list = _timers.value.toMutableList()
        list.add(deleted.index.coerceIn(0, list.size), t)
        _timers.value = list
        save()
        if (t.state == TimerState.RUNNING && t.endAt <= System.currentTimeMillis()) finish(t.id) else sideEffects(t)
    }

    private fun get(id: Int): TimerItem? = _timers.value.firstOrNull { it.id == id }

    @Synchronized
    private fun update(id: Int, transform: (TimerItem) -> TimerItem) {
        val old = get(id) ?: return
        val new = transform(old)
        if (new == old) return
        _timers.value = _timers.value.map { if (it.id == id) new else it }
        save()
        sideEffects(new)
    }

    private fun sideEffects(t: TimerItem) {
        if (t.state == TimerState.FINISHED) AlarmService.start(app) // plays the sound
        if (t.state == TimerState.RUNNING) AlarmScheduler.schedule(app, t) else AlarmScheduler.cancel(app, t.id)
        TimerNotifier.sync(app, t)
    }

    private fun load() {
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        nextId = prefs.getInt(KEY_NEXT_ID, 1)
        val raw = prefs.getString(KEY_LIST, null) ?: return
        _timers.value = runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { TimerItem.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun save() {
        val arr = JSONArray()
        _timers.value.forEach { arr.put(it.toJson()) }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_LIST, arr.toString())
            putInt(KEY_NEXT_ID, nextId)
        }
    }
}
