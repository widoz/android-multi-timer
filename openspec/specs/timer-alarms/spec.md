# timer-alarms Specification

## Purpose
Makes timers finish on time and alert the user reliably, whether the app is open or not, through notifications and a ringing alarm.

## Requirements

### Requirement: On-time finish in the background
A running timer SHALL finish at its end time even when the app is closed, the screen is off or the phone is idle.

#### Scenario: Locked phone
- **WHEN** a 1-minute timer is started and the phone is locked
- **THEN** the timer finishes and rings after one minute

#### Scenario: Exact alarms not allowed
- **WHEN** exact alarms are not permitted on the device (possible only on Android 12 if the user revokes it)
- **THEN** the timer still finishes, possibly a few minutes late

### Requirement: Ringing until stopped
A finished timer SHALL ring with its sound, looping, on the alarm volume, and vibrate, until the user stops it, adds a minute, resets it or deletes it. It SHALL ring even when the phone is set to vibrate.

#### Scenario: Vibrate mode
- **WHEN** a timer finishes while the phone's ringer is set to vibrate
- **THEN** the timer's sound plays on the alarm volume

#### Scenario: Silent sound
- **WHEN** a timer whose sound is "Silent" finishes
- **THEN** the phone vibrates without sound

#### Scenario: Several timers finish together
- **WHEN** two timers are ringing at the same time
- **THEN** one sound plays (the newest ringing timer's) until all ringing timers are stopped

### Requirement: Ringing notification
While any timer is ringing, the app SHALL show one high-priority notification: for one timer "<label> is done" with Stop and +1 min; for several timers "<n> timers done" listing their labels, with Stop all. While a timer is full screen and the app is visible, the notification SHALL NOT pop up over the screen (it stays in the notification shade, still vibrating), because the full-screen tray shows the finished timers.

#### Scenario: Stop from the notification
- **WHEN** the user taps Stop on the ringing notification
- **THEN** the sound stops and the timer becomes ready at its full duration

#### Scenario: Stop all
- **WHEN** three timers ring and the user taps Stop all
- **THEN** all three stop ringing

#### Scenario: Full screen visible
- **WHEN** a timer finishes while another timer is full screen and the app is visible
- **THEN** no notification pops up over the screen, the alarm sound plays, and the finished timer appears in the tray

### Requirement: Per-timer notifications
Each running timer SHALL have a silent ongoing notification with a live countdown, its end time, and Pause, +1 min and Reset actions. Each paused timer with progress SHALL have a silent notification showing the time left with Resume and Reset. Ready timers SHALL have no notification.

#### Scenario: Pause from the notification
- **WHEN** the user taps Pause on a running timer's notification
- **THEN** the timer pauses in the app and its notification switches to the paused form

### Requirement: Notification permission
On Android 13 and later the app SHALL ask for notification permission on first launch. Whenever notifications are not allowed, the list screen SHALL show a "Turn on notifications" card that opens the app's notification settings. Timers SHALL keep running and ringing without the permission.

#### Scenario: Permission denied
- **WHEN** the user denies notification permission and a timer finishes
- **THEN** the alarm sound still plays, and the list screen shows the Turn on notifications card

### Requirement: Restore after restart or update
After a phone restart or an app update, the app SHALL restore alarms and notifications for all timers, and timers whose end time passed meanwhile SHALL be finished and ring.

#### Scenario: Timer ended during restart
- **WHEN** a timer's end time passes while the phone is restarting
- **THEN** after restart the timer is finished and rings
