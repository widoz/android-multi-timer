# alarm-sounds Specification

## Purpose
Lets the user choose how timers sound: an app-wide default and an optional sound per timer, from the app's own sounds or the phone's alarm sounds.

## Requirements
### Requirement: Built-in app sounds
The app SHALL include five original 8-bit sounds, the same on every phone: Power chime, Pixel bell, Beeper, Gentle rise and Jingle. Each SHALL be a short phrase followed by a rest so that it loops naturally while ringing.

#### Scenario: Available offline on any phone
- **WHEN** the user opens the sound picker on any supported phone
- **THEN** the five app sounds are listed and play

### Requirement: Default sound
Settings SHALL offer a Default sound used by every timer that has no sound of its own. Power chime SHALL be the default for new installs and for installs that never chose a default; a default the user chose SHALL be kept.

#### Scenario: Change the default
- **WHEN** the user sets the default sound to Jingle
- **THEN** every timer without its own sound rings with Jingle, and timer panels show "App default (Jingle)"

### Requirement: Per-timer sound
The New/Edit timer panel SHALL include a Sound row that shows "App default (<default sound>)" or the timer's own sound, and opens the sound picker. Choosing "App default" SHALL make the timer follow the default again. Changing a timer's sound SHALL never restart it.

#### Scenario: Override and revert
- **WHEN** the user sets a timer's sound to Pixel bell, saves, then later sets it back to App default
- **THEN** the timer rings with Pixel bell in between and with the default sound afterwards

### Requirement: Sound picker
The sound picker SHALL list, in order: App default (per-timer picker only); the "App sounds" group; the "Phone sounds" group starting with "Phone's alarm sound" followed by the phone's alarm sounds sorted by name; and "Silent (vibrate only)". Tapping a sound SHALL play a preview; OK SHALL save and Cancel SHALL discard.

#### Scenario: Preview
- **WHEN** the user taps Beeper in the picker
- **THEN** Beeper plays once and becomes the selected option, without saving yet

#### Scenario: No duplicate phone sounds
- **WHEN** the phone's media library holds several copies of a sound with the same name
- **THEN** the picker lists that name once, keeping the copy the user already chose if any

### Requirement: Phone's alarm sound
"Phone's alarm sound" SHALL follow whatever alarm sound the phone is set to, including later changes in the phone's settings.

#### Scenario: Phone setting changes
- **WHEN** a timer uses Phone's alarm sound and the user changes the phone's alarm sound in system settings
- **THEN** the timer rings with the new phone alarm sound

### Requirement: Fallback when a sound cannot play
If a timer's sound cannot be played (for example a phone sound that was deleted), the timer SHALL ring with Power chime instead, and if that fails, with the phone's alarm sound.

#### Scenario: Deleted phone sound
- **WHEN** a timer's chosen phone sound file no longer exists and the timer finishes
- **THEN** it rings with Power chime
