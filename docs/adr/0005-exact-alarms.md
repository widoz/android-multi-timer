# 0005. Exact alarms through AlarmManager

- Status: Accepted
- Date: 2026-11-06 (updated 2026-11-07 for Android 11–12)

## Context
A timer must finish on time even when the app is closed and the phone is in Doze. Android restricts exact alarms: Android 12 introduced `SCHEDULE_EXACT_ALARM` (user-revocable), and Android 13 added `USE_EXACT_ALARM` for apps whose core function is alarms or timers.

## Decision
Schedule one exact alarm per running timer with `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, endAt)`, keyed by timer id. Permissions:
- Android 13+: `USE_EXACT_ALARM`, granted at install.
- Android 12: `SCHEDULE_EXACT_ALARM` (declared with `maxSdkVersion="32"`).
- Android 11: no permission needed.
If exact alarms are not allowed, fall back to `setAndAllowWhileIdle` so timers still finish, possibly late. The UI also finishes due timers itself while it is visible. A boot/update receiver re-arms alarms.

## Alternatives considered
- WorkManager: not designed for exact timing.
- A foreground service counting down while timers run: battery cost and a permanent notification for nothing.

## Consequences
- Google Play allows `USE_EXACT_ALARM` only for alarm/timer apps; a declaration is required when publishing.
- On Android 12 a user who revokes the permission gets late alarms; a warning card could be added later.
