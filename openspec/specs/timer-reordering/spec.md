# timer-reordering Specification

## Purpose
Lets the user put timers in their own order on the list screen, by dragging or without dragging, and keeps that order.

## Requirements

### Requirement: Drag to reorder
On all supported Android versions (11 and later), the user SHALL be able to move a timer by long-pressing its card outside any button, dragging it to another position and releasing it there. Reordering SHALL be unavailable while fewer than two timers exist.

#### Scenario: Move a timer down
- **WHEN** the list shows Pasta, Focaccia, Tea and Laundry, and the user long-presses Pasta and drags it below Tea, then releases
- **THEN** the list shows Focaccia, Tea, Pasta and Laundry

### Requirement: Drag feedback
While a card is lifted it SHALL follow the finger above the other cards with a raised look, the other cards SHALL move aside, and a shaded layer SHALL mark where it will land.

#### Scenario: Landing layer in both themes
- **WHEN** a card is lifted in light or dark theme
- **THEN** the landing position is shown as a shaded layer of the on-surface color at low opacity, visible against the list background in both themes

#### Scenario: Haptic feedback
- **WHEN** a card is lifted, and each time it passes another card
- **THEN** the phone gives a short haptic response, without any extra permission

### Requirement: Gestures keep their meaning
Taps on a card's buttons and swipes that start without a long press SHALL keep their existing behavior, and the list SHALL scroll by itself while a lifted card is held near its top or bottom edge.

#### Scenario: Quick swipe scrolls
- **WHEN** the user swipes up or down on a card without first holding it
- **THEN** the list scrolls and no card is lifted

#### Scenario: Auto-scroll near the edge
- **WHEN** a lifted card is held near the top or bottom edge of the list and more timers lie beyond that edge
- **THEN** the list scrolls in that direction until the card is moved away from the edge or the list reaches its end

### Requirement: Move without dragging
Long-pressing a card and releasing it without moving SHALL open a Move menu for that timer, headed with its label, offering Move to top, Move up, Move down and Move to bottom, with the options that don't apply disabled. Each card SHALL also offer the applicable move actions to screen readers as custom actions, and after a move the new position SHALL be announced.

#### Scenario: Move to top from the menu
- **WHEN** the user long-presses Laundry, the last of four timers, releases without moving and chooses Move to top
- **THEN** Laundry becomes the first timer, and Move down and Move to bottom were disabled in the menu

#### Scenario: TalkBack actions
- **WHEN** TalkBack focuses the second of three timer cards and the user opens its actions
- **THEN** Move up, Move down, Move to top and Move to bottom are offered, and choosing Move down announces that the timer is now in position 3 of 3

### Requirement: Custom order is kept
The order chosen by the user SHALL be saved and SHALL survive closing the app, process death, app updates and phone restarts. New timers, including quick-start timers, SHALL still appear at the top of the list, and Undo after a delete SHALL restore the timer to its position in the custom order.

#### Scenario: Phone restart
- **WHEN** the user moves Laundry to the top and then restarts the phone
- **THEN** after restart Laundry is still the first timer

#### Scenario: New timer after reordering
- **WHEN** the user has reordered the list and then taps "+ 5 min"
- **THEN** the new timer appears at the top and the other timers keep their custom order
