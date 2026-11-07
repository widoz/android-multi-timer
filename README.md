# Multi Timer

An Android app for running several countdown timers at once. Each timer is independent:
start, pause, reset, add or remove a minute, edit or delete any of them without affecting the others.

- **Edit** reuses the new-timer panel. Label and color change in place; a new duration restarts
  the timer, after a confirmation if it had already counted some time.
- **Delete** asks for confirmation, then offers Undo for a few seconds.
- **Settings** (gear icon): Theme (System default, Light, Dark) and the default alarm sound.
- **Sounds**: each timer uses the default sound unless you pick its own in the New/Edit panel.
  The list shows the phone's alarm sounds plus "Phone's alarm sound" and "Silent"; tap one to
  preview it.
- **Ringing**: `alarm/AlarmService.kt` plays the sound itself on the alarm stream (alarm volume),
  looping until stopped, like a clock app. Notification sounds can be muted by the phone's sound
  mode or Do Not Disturb, so they are not used. One "ringing" notification covers all finished
  timers, with Stop / +1 min (or Stop all).

- **Min Android version:** 11 (API 30). Targets API 35.
  - Android 12: exact alarms use `SCHEDULE_EXACT_ALARM`, which the user can revoke (timers then
    ring a few minutes late). Android 13+ uses `USE_EXACT_ALARM`, granted at install.
  - Android 11–12: no notification permission prompt (notifications are on by default).
  - Android 11: the in-app Theme setting is applied by the app itself; the startup window may
    briefly show the phone's theme. Android 12+ uses per-app night mode.
- **Stack:** Kotlin, Jetpack Compose, Material 3. No third-party libraries.

## Open and run

1. Open this folder in Android Studio (Ladybug or newer) with **File › Open**.
2. Let Gradle sync; Studio creates `local.properties` pointing at your Android SDK.
3. Pick an Android 11+ device or emulator and press **Run**.

From the command line, with `ANDROID_HOME` set:

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## How it works

| Piece | File | Role |
|---|---|---|
| State | `data/TimerRepository.kt` | Single source of truth. Saves every change to SharedPreferences and updates the alarm and notification for that timer. |
| Model | `data/TimerItem.kt` | A running timer stores its end time, a paused one stores time left, so nothing drifts. |
| Alarms | `alarm/AlarmScheduler.kt` | One exact alarm per running timer (`setExactAndAllowWhileIdle`), so timers finish on time even in Doze or with the app closed. |
| Notifications | `alarm/TimerNotifier.kt` | One notification per timer. Running ones use the system countdown chronometer (no foreground service needed); finished ones ring with the alarm sound until stopped. |
| Receivers | `alarm/Receivers.kt` | Alarm firing, notification buttons (Pause/Resume, +1 min, Reset/Stop) and restoring timers after reboot or app update. |
| UI | `ui/` | Timer list and cards, quick-start chips, create/edit bottom sheet, confirmation dialogs, Undo snackbar. |

## Release builds

Release builds are signed with the key described in `keystore.properties` at the project root.
That file and the `.jks` key are git-ignored: keep them private and back them up, because updates
only install over an existing copy when signed with the same key.

```bash
./gradlew assembleRelease
```

The signed APK lands in `app/build/outputs/apk/release/app-release.apk`. Without
`keystore.properties`, the release APK is built unsigned. Bump `versionCode` and `versionName` in
`app/build.gradle.kts` for each release.

## Theme and accessibility

- Light is white, dark uses Material's neutral greys (`#121212`, cards `#1E1E1E`). Controls are
  neutral; color is reserved for each timer's color and the orange of a finished timer.
- The Theme setting calls `UiModeManager.setApplicationNightMode` on Android 12+, so the status
  bar, startup window and dialogs follow the choice too (`data/AppSettings.kt`). On Android 11
  `MainActivity` applies it to the Compose theme and system bars.
- Colors meet WCAG 2.2 AA in both themes: text >= 4.5:1, control outlines and timer colors >= 3:1.
  In `ui/theme/Theme.kt`, `outline` is used for every border and line and must stay >= 3:1;
  `outlineVariant` is only the progress-ring track.
- Touch targets are at least 48dp, icon buttons and color swatches have TalkBack labels, and the
  countdown shrinks to fit instead of clipping at large font sizes.

## Permissions

- `POST_NOTIFICATIONS`: asked on first launch (required on Android 13+). Without it timers still run but cannot alert you; the app shows a card linking to settings.
- `USE_EXACT_ALARM`: granted at install on Android 13+. Google Play only allows it for apps whose core function is alarms or timers, which this is.
- `RECEIVE_BOOT_COMPLETED`, `VIBRATE`.

## Customising

- Package name: `dev.multitimer.app` in `app/build.gradle.kts` (`namespace` and `applicationId`) and the `java/dev/multitimer/app` folder. Change it before publishing.
- Wallpaper-based colors: pass `dynamicColor = true` to `MultiTimerTheme` in `MainActivity.kt`
  (not contrast-checked).
