# Multi Timer

An Android app for running several countdown timers at once. Each timer is independent:
start, pause, reset, add or remove a minute, edit or delete any of them without affecting the others.

- **Edit** reuses the new-timer panel. Label and color change in place; a new duration restarts
  the timer, after a confirmation if it had already counted some time.
- **Delete** asks for confirmation, then offers Undo for a few seconds.
- **Theme**: the top-bar menu picks System default, Light or Dark.

- **Min Android version:** 13 (API 33). Targets API 35.
- **Stack:** Kotlin, Jetpack Compose, Material 3. No third-party libraries.

## Open and run

1. Open this folder in Android Studio (Ladybug or newer) with **File › Open**.
2. Let Gradle sync; Studio creates `local.properties` pointing at your Android SDK.
3. Pick an Android 13+ device or emulator and press **Run**.

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

## Theme and accessibility

- Light is white, dark uses Material's neutral greys (`#121212`, cards `#1E1E1E`). Controls are
  neutral; color is reserved for each timer's color and the orange of a finished timer.
- The Theme menu calls `UiModeManager.setApplicationNightMode` (Android 12+), so the status bar,
  startup window and dialogs follow the choice too (`data/ThemeSettings.kt`).
- Colors meet WCAG 2.2 AA in both themes: text >= 4.5:1, control outlines and timer colors >= 3:1.
  In `ui/theme/Theme.kt`, `outline` marks controls and must stay >= 3:1; `outlineVariant` is
  decorative only.
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
