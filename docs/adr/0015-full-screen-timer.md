# 0015. Full-screen timer with a tray of finished timers

- Status: Accepted
- Date: 2026-11-10

## Context
Users following one timer closely (cooking, workouts) want it large and always visible, without losing the alerts of other timers running at the same time, and without the screen locking. Approved in the mockup's full-screen variation, including a Keep screen on option.

## Decision
- **Full screen is UI state** of the timer list screen: the id of the timer shown, saved across configuration changes (theme switches recreate the activity). If that timer disappears, full screen closes. Back and an exit button leave it.
- **Finished timers while full screen** go to a tray at the bottom: an `AnimatedVisibility` that expands from the bottom while the dial area (weighted) shrinks above it. The tray is a `HorizontalPager` of one card per finished timer (newest first by end time), with a peek of the next card, page dots (24dp targets) and previous/next buttons. Each card has Stop and +1 min. The full-screen timer itself finishes in place (finished style and a large Stop button), not in the tray.
- **Keep screen on** is a persisted setting (default on) applied with `View.keepScreenOn` on the full-screen view only, so it ends with the view and needs no permission or wake lock. A toggle in the full-screen view changes the same setting and confirms with a snackbar.
- **System bars** are hidden with `WindowInsetsControllerCompat` (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) while full screen and shown again on exit.
- **No heads-up over full screen**: while the full-screen view is resumed, a `FullScreenState` flag makes the alarm service post its ringing notification on a second channel, "Finished timers (full screen)", with default importance (no pop-up, still vibrating). The sound is unaffected because the service plays it ([0008](0008-alarm-service-plays-sound.md)).
- **Card button placement**: the full-screen button sits in the card's top row next to start/pause. The mockup first put it in the bottom row, which on a 360dp phone already uses about 298 of 300dp with 48dp targets ([0011](0011-wcag-aa-baseline.md)); the mockup was updated.

## Alternatives considered
- A separate activity for full screen: more lifecycle and back-stack handling for no benefit.
- `FLAG_SHOW_WHEN_LOCKED` / turning the screen on: not requested; keeping it on is enough.
- Wake lock (`PowerManager`): needs a permission and can leak; the window flag is scoped to the view.
- Showing finished timers as a stacked list instead of a pager: takes more height from the full-screen timer.
- Suppressing the ringing notification entirely: the foreground service must keep a notification; a quieter channel keeps it in the shade.

## Consequences
- One more notification channel appears in the app's notification settings.
- If the user leaves the app while full screen, notifications pop up again (the flag follows the activity's resumed state).
- `ShrinkToFitText` and the status-line text are shared between the card and the full-screen view.
