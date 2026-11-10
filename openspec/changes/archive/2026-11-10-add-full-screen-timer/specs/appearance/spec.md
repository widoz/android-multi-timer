# Spec Delta

## MODIFIED Requirements

### Requirement: Settings screen
A gear icon in the list screen's top bar SHALL open a Settings screen with an Appearance section (Theme), an Alarms section (Default sound) and a Full screen section (Keep screen on). Back SHALL return to the list.

#### Scenario: Open and close
- **WHEN** the user taps the gear icon and then Back
- **THEN** Settings opens and then the timer list is shown again unchanged

#### Scenario: Keep screen on switch
- **WHEN** the user opens Settings
- **THEN** the Full screen section shows a Keep screen on switch, on by default
