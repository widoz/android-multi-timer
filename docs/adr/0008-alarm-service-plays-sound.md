# 0008. A foreground alarm service plays the sound

- Status: Accepted
- Date: 2026-11-07
- Supersedes: [0007](0007-sound-per-notification-channel.md)

## Context
Ringing through notification sounds ([0007](0007-sound-per-notification-channel.md)) was silent on a real phone. Clock apps play alarm audio themselves so it rings regardless of ringer mode and notification settings.

## Decision
When any timer finishes, start `AlarmService`, a foreground service that:
- plays the timer's sound with `MediaPlayer` on the alarm stream (`USAGE_ALARM`), looping, with transient audio focus so music pauses;
- shows one ringing notification for all finished timers ("<label> is done" with Stop / +1 min, or "<n> timers done" with Stop all), vibrating with `FLAG_INSISTENT` on a single silent "ringing" channel;
- observes the repository and stops itself when nothing rings;
- falls back to Power chime, then the phone's alarm sounds, if the chosen sound cannot play.

The service type is `systemExempted`, which Android 14+ reserves for apps holding exact-alarm permissions; on older versions the manifest type is used. The exact alarm that finishes a timer allows the service to start from the background. If Android still refuses, each finished timer gets its own vibrating notification instead. Legacy per-sound channels are deleted on start.

## Alternatives considered
- Keep notification-channel sounds: unreliable, as observed.
- `mediaPlayback` service type: semantically wrong for alarms and subject to stricter background-start rules.

## Consequences
- Rings in vibrate mode and without notification permission; volume follows the alarm volume.
- When several timers ring, one sound plays (the newest timer's).
- Google Play requires declaring the foreground service type when publishing.
