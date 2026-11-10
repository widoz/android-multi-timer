# Spec Delta

## MODIFIED Requirements

### Requirement: Timer card
Each timer SHALL be shown as a card with its color, label, remaining time, a progress ring with percentage, a status line, a full-screen button and a start/pause button, and the actions −1 min, +1 min, reset, edit and delete.

#### Scenario: Status line
- **WHEN** a timer is running, paused partway, ready (full duration) or finished
- **THEN** its status reads "Ends at <time>", "Paused · of <duration>", "Ready · <duration>" or "Time's up" respectively

#### Scenario: Own sound shown
- **WHEN** a timer has its own sound instead of the app default
- **THEN** the status line ends with " · <sound name>"

#### Scenario: Full-screen button
- **WHEN** the user taps a card's full-screen button
- **THEN** that timer opens full screen
