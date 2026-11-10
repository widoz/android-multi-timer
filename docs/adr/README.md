# Architecture Decision Records

Why Multi Timer is built the way it is. Behavior (what it does) is specified in [`openspec/specs`](../../openspec/specs). Format: [`0000-template.md`](0000-template.md); see [0001](0001-record-architecture-decisions.md).

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-mockup-first-workflow.md) | Mockup-first workflow | Accepted |
| [0003](0003-native-kotlin-compose.md) | Native Kotlin with Jetpack Compose and Material 3 | Accepted |
| [0004](0004-timer-state-and-persistence.md) | Timer model, single repository and JSON persistence | Accepted |
| [0005](0005-exact-alarms.md) | Exact alarms through AlarmManager | Accepted |
| [0006](0006-per-timer-notifications-without-service.md) | Per-timer notifications with a system countdown | Accepted |
| [0007](0007-sound-per-notification-channel.md) | Alarm sound through notification channels, one per sound | Superseded by 0008 |
| [0008](0008-alarm-service-plays-sound.md) | A foreground alarm service plays the sound | Accepted |
| [0009](0009-bundled-8bit-sounds.md) | Bundled original 8-bit sounds, Power chime as default | Accepted |
| [0010](0010-neutral-theme-and-in-app-theme-choice.md) | Neutral theme and in-app theme choice | Accepted |
| [0011](0011-wcag-aa-baseline.md) | WCAG 2.2 AA as the accessibility baseline | Accepted |
| [0012](0012-minimum-android-11.md) | Minimum Android version 11 (API 30) | Accepted |
| [0013](0013-release-signing-and-versioning.md) | Release signing, versioning and distribution | Accepted |
| [0014](0014-publish-to-google-play.md) | Publish to Google Play | Proposed |
| [0015](0015-full-screen-timer.md) | Full-screen timer with a tray of finished timers | Accepted |
