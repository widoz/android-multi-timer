# appearance Specification

## Purpose
Defines the app's visual identity, its light and dark themes, and the Settings screen where the theme is chosen.

## Requirements

### Requirement: Settings screen
A gear icon in the list screen's top bar SHALL open a Settings screen with an Appearance section (Theme), an Alarms section (Default sound) and a Full screen section (Keep screen on). Back SHALL return to the list.

#### Scenario: Open and close
- **WHEN** the user taps the gear icon and then Back
- **THEN** Settings opens and then the timer list is shown again unchanged

#### Scenario: Keep screen on switch
- **WHEN** the user opens Settings
- **THEN** the Full screen section shows a Keep screen on switch, on by default

### Requirement: Theme choice
Settings SHALL offer Theme: System default (follows the phone), Light, or Dark. The choice SHALL apply immediately to the whole app, including status bar icons, and SHALL be remembered.

#### Scenario: Force light on a dark phone
- **WHEN** the phone uses dark mode and the user picks Light
- **THEN** the app shows its light theme with dark status bar icons

#### Scenario: Android 11 startup
- **WHEN** on Android 11 the chosen theme differs from the phone's theme and the app starts
- **THEN** the startup window may briefly show the phone's theme before the app's theme appears (Android 12 and later show the chosen theme from the start)

### Requirement: Neutral palette
Light theme SHALL use a white background; dark theme SHALL use neutral greys (background #121212, cards #1E1E1E). Controls (buttons, New timer button, chips, radio buttons) SHALL be neutral: near-black in light theme and light grey in dark theme. Color SHALL be reserved for each timer's own color and for the orange of a finished timer.

#### Scenario: No accent tint
- **WHEN** the app is shown in either theme
- **THEN** backgrounds, cards and controls contain no hue other than grey, except timer colors and the finished state

### Requirement: Timer colors
Each timer SHALL have one of five colors (teal, amber, violet, green, rose), shown on its dot and progress ring, with brighter variants in light theme and pastel variants in dark theme.

#### Scenario: Color follows theme
- **WHEN** the user switches from dark to light theme
- **THEN** each timer keeps its color, shown in the light-theme variant

### Requirement: Borders and lines
Timer card borders, the divider inside each card, chips, ±1 min buttons, text fields, the sound row and menus SHALL all use the same outline color (#8A8A8A light, #757575 dark). The progress ring track SHALL stay lighter.

#### Scenario: Consistent outlines
- **WHEN** the list screen is shown
- **THEN** card borders and quick-start chip borders have the same color and width

### Requirement: Finished state
A ringing timer's card SHALL use an orange-tinted background, orange border and orange remaining time, and its main button SHALL become Stop.

#### Scenario: Timer finishes while the app is open
- **WHEN** a timer finishes while the list is visible
- **THEN** its card switches to the finished style and the summary counts it as ringing
