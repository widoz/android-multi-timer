# Design

See [ADR 0015](../../../docs/adr/0015-full-screen-timer.md) for the decisions and alternatives.

## Key points
- Full screen is UI state of the timer list screen (the id of the timer shown), saved across configuration changes; it closes itself if that timer is deleted.
- Keep screen on uses the window's keep-screen-on flag on the full-screen view only, so it never outlives the view and needs no permission.
- System bars are hidden with the window insets controller (transient bars on swipe) and restored when leaving full screen.
- The tray lists finished timers other than the one shown, newest first, in a horizontal pager with a peek of the next card, page dots and previous/next buttons.
- While a full-screen timer is visible (activity resumed), the alarm service posts its ringing notification on a channel with default importance (no heads-up, still vibrating) instead of the high-importance channel. The sound is unchanged: it is played by the service.

## Width on small phones
The mockup placed the full-screen button in the card's bottom row. On a 360dp phone that row already uses ~298 of 300dp with 48dp touch targets, so the button sits in the top row next to start/pause instead (mockup updated to match).
