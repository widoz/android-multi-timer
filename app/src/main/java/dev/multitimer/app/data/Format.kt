package dev.multitimer.app.data

import java.util.Locale

/** 04:09 or 1:04:09, rounding up so a timer never shows 00:00 while still running. */
fun formatClock(ms: Long): String {
    val s = (ms + 999) / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.ROOT, "%02d:%02d", m, sec)
}

/** 1 h 5 min, 45 s, etc. */
fun formatDuration(ms: Long): String {
    val s = (ms + 500) / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return listOfNotNull(
        if (h > 0) "$h h" else null,
        if (m > 0) "$m min" else null,
        if (sec > 0) "$sec s" else null,
    ).joinToString(" ").ifEmpty { "0 s" }
}
