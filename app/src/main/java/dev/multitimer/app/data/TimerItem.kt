package dev.multitimer.app.data

import org.json.JSONObject

enum class TimerState { RUNNING, PAUSED, FINISHED }

/**
 * One independent countdown.
 *
 * A running timer is defined by its wall-clock end time ([endAt]), so it never drifts
 * and survives process death. A paused timer stores what is left in [remainingMs].
 */
data class TimerItem(
    val id: Int,
    val label: String,
    val totalMs: Long,
    val colorIndex: Int,
    /** Alarm sound URI, or null to use the app's default sound. */
    val soundUri: String? = null,
    val state: TimerState,
    val remainingMs: Long,
    val endAt: Long,
) {
    /** Paused at its full duration: never started, or reset. */
    val isReady: Boolean get() = state == TimerState.PAUSED && remainingMs >= totalMs

    fun remainingAt(now: Long): Long = when (state) {
        TimerState.RUNNING -> (endAt - now).coerceAtLeast(0)
        TimerState.PAUSED -> remainingMs
        TimerState.FINISHED -> 0
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("totalMs", totalMs)
        .put("colorIndex", colorIndex)
        .put("soundUri", soundUri ?: JSONObject.NULL)
        .put("state", state.name)
        .put("remainingMs", remainingMs)
        .put("endAt", endAt)

    companion object {
        fun fromJson(o: JSONObject) = TimerItem(
            id = o.getInt("id"),
            label = o.getString("label"),
            totalMs = o.getLong("totalMs"),
            colorIndex = o.optInt("colorIndex", 0),
            soundUri = if (o.isNull("soundUri")) null else o.optString("soundUri"),
            state = runCatching { TimerState.valueOf(o.getString("state")) }
                .getOrDefault(TimerState.PAUSED),
            remainingMs = o.getLong("remainingMs"),
            endAt = o.optLong("endAt", 0),
        )
    }
}
