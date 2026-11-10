# Spec Delta

## MODIFIED Requirements

### Requirement: Ringing notification
While any timer is ringing, the app SHALL show one high-priority notification: for one timer "<label> is done" with Stop and +1 min; for several timers "<n> timers done" listing their labels, with Stop all. While a timer is full screen and the app is visible, the notification SHALL NOT pop up over the screen (it stays in the notification shade, still vibrating), because the full-screen tray shows the finished timers.

#### Scenario: Stop from the notification
- **WHEN** the user taps Stop on the ringing notification
- **THEN** the sound stops and the timer becomes ready at its full duration

#### Scenario: Stop all
- **WHEN** three timers ring and the user taps Stop all
- **THEN** all three stop ringing

#### Scenario: Full screen visible
- **WHEN** a timer finishes while another timer is full screen and the app is visible
- **THEN** no notification pops up over the screen, the alarm sound plays, and the finished timer appears in the tray
