# full-screen-timer Specification

## Purpose
Shows one timer large and always visible while still surfacing and controlling every other timer that finishes meanwhile.

## Requirements

### Requirement: Full-screen view
Each timer SHALL be viewable full screen, showing its color and label, a large progress ring with the remaining time and status, −1 min, start/pause and +1 min controls, and an exit button. Back SHALL also exit. Other timers SHALL keep running unaffected.

#### Scenario: Open and exit
- **WHEN** the user taps the full-screen button on the Pasta card, then the exit button
- **THEN** Pasta is shown full screen, and then the timer list is shown again

#### Scenario: Timer deleted elsewhere
- **WHEN** the full-screen timer is deleted from its notification
- **THEN** the full-screen view closes and the list is shown

### Requirement: Full-screen timer finishing
When the full-screen timer itself finishes, the view SHALL switch to the finished style in place (orange ring and time, "Time's up") with a large Stop button, and SHALL NOT move it to the tray.

#### Scenario: Stop in place
- **WHEN** the full-screen timer finishes and the user taps Stop
- **THEN** the alarm stops and the timer is shown ready at its full duration

### Requirement: Finished-timer tray
While a timer is full screen, every other timer that is ringing SHALL be shown in a tray at the bottom of the screen; the full-screen content SHALL shrink to make room. Each finished timer SHALL have its own card with its label, how long ago it ended, its sound, and Stop and +1 min actions. Cards SHALL be ordered newest first and browsed by swiping sideways, with page dots and previous/next buttons when there are several. The tray SHALL close when no other timer is ringing.

#### Scenario: Three timers end together
- **WHEN** Focaccia is full screen and Pasta, Tea steep and Laundry finish at the same time
- **THEN** the tray shows "3 timers done · swipe to see each" with three cards, and the next card peeks in from the side

#### Scenario: Stop each separately
- **WHEN** the user stops the cards one by one
- **THEN** each stop silences only that timer, and after the last one the tray closes and the view returns to full size

### Requirement: Keep screen on
While a timer is full screen and the Keep screen on setting is enabled, the screen SHALL NOT turn off or lock. The setting SHALL be on by default and SHALL also be changeable from a toggle in the full-screen view, which confirms the change. Outside full screen the screen SHALL follow the phone's normal timeout.

#### Scenario: Default
- **WHEN** the user opens a timer full screen without changing settings and leaves the phone untouched
- **THEN** the screen stays on

#### Scenario: Quick toggle
- **WHEN** the user taps the keep-screen-on toggle in the full-screen view
- **THEN** the setting turns off, a message says the screen can turn off in full screen, and Settings shows it off

### Requirement: System bars in full screen
While a timer is full screen, the status and navigation bars SHALL be hidden; swiping from a screen edge SHALL show them temporarily. They SHALL return when leaving full screen.

#### Scenario: Leaving full screen
- **WHEN** the user exits full screen
- **THEN** the status and navigation bars are visible again
