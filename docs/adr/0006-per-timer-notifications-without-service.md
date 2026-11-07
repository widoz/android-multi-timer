# 0006. Per-timer notifications with a system countdown

- Status: Accepted
- Date: 2026-11-06

## Context
Users want to see and control running timers from the notification shade. The first plan was a foreground service to keep timers counting with the app closed.

## Decision
Running and paused timers each get their own silent notification (notification id = timer id) on a low-importance channel. Running notifications use the system chronometer in countdown mode (`setUsesChronometer` + `setChronometerCountDown` + `setWhen(endAt)`), so Android updates the countdown with no app process running. Actions (Pause/Resume, +1 min, Reset) go to a broadcast receiver that calls the repository. Ready timers have no notification.

## Alternatives considered
- Foreground service while any timer runs: unnecessary once alarms and the system chronometer exist; costs battery and requires a service type.

## Consequences
- No service runs while timers are only counting.
- Finished timers are handled differently; see [0008](0008-alarm-service-plays-sound.md).
