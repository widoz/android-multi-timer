# Design

## Gesture
- Each card listens for a long press followed by a drag (`detectDragGesturesAfterLongPress`). Taps and quick swipes keep their current meaning: buttons react to taps, the list scrolls on a swipe.
- The lift happens when the long press is recognized (haptic `LongPress`). If the finger is released without moving more than the touch slop, the drag ends without a move and the Move menu opens.
- Long press is disabled while fewer than two timers exist.

## Drag rendering
- During a drag the list shows a local copy of the timers in which the dragged timer is already at its landing position. LazyColumn's `animateItem` animates the other cards into their new places.
- The dragged item draws a shaded layer (on-surface at 10% opacity, sunk-in look) at its layout slot and draws the card above it, offset with `graphicsLayer` so it follows the finger: `translationY = startTop + dragDelta − currentTop`, using viewport positions from `LazyListState.layoutInfo`, which keeps the card under the finger while the list auto-scrolls. Lifted cards are scaled to 102% with a raised shadow and an on-surface outline.
- The dragged card swaps with a neighbour when its centre crosses the neighbour's middle; each swap gives a light haptic tick (`TextHandleMove`).
- Auto-scroll: while the finger is within 56dp of the list's top or bottom edge, a coroutine scrolls the list each frame, faster closer to the edge.
- If the dragged timer is the first visible item, the list's scroll anchor is preserved with `requestScrollToItem` so the list does not jump.
- On release the order is committed once with `TimerRepository.move`; the local copy is dropped when the repository emits the new order.

## Move menu and TalkBack
- The Move menu is a Material 3 `DropdownMenu` anchored to the card, with a header naming the timer and the four items; items that don't apply are disabled.
- Each card exposes `CustomAccessibilityAction`s Move up / Move down / Move to top / Move to bottom (only those that apply). After a move, the new position is announced ("Pasta moved to position 3 of 5").

## Width on small phones
The approved version 1 placed a grip at the start of the card's bottom row. Measured on a 360dp-wide phone, that row already uses about 290 of the 298dp available with 48dp touch targets, so the grip was replaced by the long press (mockup version 2, approved).

## Storage
Timers are already persisted as an ordered JSON array in SharedPreferences, loaded in the same order, so `move` only reorders the in-memory list and saves.
