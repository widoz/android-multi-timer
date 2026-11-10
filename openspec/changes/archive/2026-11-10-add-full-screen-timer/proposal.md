# Proposal

## Why

Users following one timer closely (cooking, workouts) want it large and always visible, without losing alerts from the other timers running at the same time. Approved in the full-screen variation of the interactive mockup on 2026-11-10 (now `docs/design/mockup.html`).

## What Changes

- Each timer card gets a full-screen button; the full-screen view shows one timer with a large ring and countdown, −1 min, start/pause and +1 min, and an exit button (Back also exits).
- While a timer is full screen and other timers end, the view shrinks from the bottom and a tray shows every finished timer as a swipeable card with its own Stop and +1 min. The tray closes when the last one is stopped.
- If the full-screen timer itself ends, it turns to the finished style in place with a large Stop button.
- New setting **Keep screen on** (Settings › Full screen, on by default) keeps the screen from turning off or locking while a timer is full screen; a quick toggle in the full-screen view changes the same setting.
- While full screen and visible, the status and navigation bars are hidden (swipe from an edge shows them), and the ringing notification does not pop up over the screen, since the tray already shows finished timers.

## Capabilities

### New Capabilities
- `full-screen-timer`: the full-screen view of one timer, the tray of finished timers, keep-screen-on and system bars while full screen.

### Modified Capabilities
- `timer-management`: the timer card gains a full-screen button.
- `timer-alarms`: the ringing notification does not pop up over a visible full-screen timer.
- `appearance`: the Settings screen gains a Full screen section.

## Impact

- UI: new full-screen composable with a horizontal pager for the tray; timer card top row; Settings screen.
- Settings: new persisted `keep_screen_on` value.
- Alarm service: chooses a non-intrusive notification channel while a full-screen timer is visible.
- Docs: ADR 0015; mockup updated; version 0.5.0.
- No new permissions (keeping the screen on uses a window flag).
