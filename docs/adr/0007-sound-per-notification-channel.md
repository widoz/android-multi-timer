# 0007. Alarm sound through notification channels, one per sound

- Status: Superseded by [0008](0008-alarm-service-plays-sound.md)
- Date: 2026-11-07

## Context
Version 0.2.0 added a sound per timer. Since Android 8, a notification's sound is fixed by its notification channel and cannot change after the channel is created.

## Decision
Create one high-importance "Finished timers · <sound>" channel per sound the first time a timer rings with it, and post the finished notification on that channel with `FLAG_INSISTENT` so the sound repeats.

## Consequences
- Extra channels appeared in the phone's notification settings.
- In practice finished timers only vibrated on the product owner's phone: notification sounds can be muted by the ringer mode, notification settings or Do Not Disturb. This led to [0008](0008-alarm-service-plays-sound.md).
