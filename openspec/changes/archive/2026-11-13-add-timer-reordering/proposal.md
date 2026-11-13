# Proposal

## Why

Timers always appear newest first, so the ones a user cares about most drift down the list as new timers are added. Users want to put timers in their own order. Approved in the reorder variation of the interactive mockup on 2026-11-13 (now `docs/design/mockup.html`): version 1 proposed a drag grip on each card; the feasibility check found no room for a 48dp grip in the card's action row on 360dp phones, so the approved version 2 uses a long press instead, with a Move menu as the non-dragging alternative.

## What Changes

- Long-press a timer card (outside its buttons) to lift it, then drag it up or down the list. Other cards move aside, a shaded layer shows where it will land, and the list scrolls by itself near its edges. Releasing drops the card there.
- Long-pressing and releasing without moving opens a Move menu on the card: Move to top, Move up, Move down, Move to bottom. TalkBack offers the same four actions on each card.
- The chosen order is saved like the rest of the timer state. New timers still appear at the top; Undo after a delete still restores the timer to its previous position.
- Haptic feedback when a card lifts and each time it passes another card.

## Capabilities

### New Capabilities
- `timer-reordering`: moving timers in the list by dragging, by the Move menu and by TalkBack actions, and keeping that order.

### Modified Capabilities
None. The existing "Newest timer first", undo-position and persistence requirements in `timer-management` stay as they are and already fit the new behavior.

## Impact

- UI: timer list (`TimerScreen`) gains the drag state, auto-scroll and the Move menu; `TimerCard` gains the long-press gesture, the lifted style and TalkBack custom actions.
- Data: `TimerRepository.move(id, toIndex)`; the stored list order is already the display order, so no storage change.
- No new permissions (haptics use the view's haptic feedback, which needs no VIBRATE permission), no new dependencies, no ADR (no architectural decision: no new service, storage, permission or library).
- Docs: mockup updated; version 0.6.0.
