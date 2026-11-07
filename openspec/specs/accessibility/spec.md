# accessibility Specification

## Purpose
Keeps the app usable by everyone, meeting WCAG 2.2 AA in both themes and working with screen readers, large fonts and right-to-left languages.

## Requirements
### Requirement: Contrast
In both light and dark themes, text SHALL have a contrast ratio of at least 4.5:1 against its background, and control outlines, icons and timer colors SHALL have at least 3:1. Disabled controls are exempt.

#### Scenario: Light theme finished state
- **WHEN** a timer is finished in light theme
- **THEN** the "Time's up" text and remaining time have at least 4.5:1 contrast against the orange-tinted card

#### Scenario: Control outlines
- **WHEN** text fields, chips and ±1 min buttons are shown in either theme
- **THEN** their outlines have at least 3:1 contrast against the background

### Requirement: Touch targets
Every interactive element SHALL have a touch target of at least 48×48 dp, even when drawn smaller.

#### Scenario: Small icons on a card
- **WHEN** the user taps the reset, edit or delete icon on a timer card
- **THEN** the touch area is at least 48×48 dp

### Requirement: Screen reader labels
Every icon-only control SHALL have a spoken label, color swatches SHALL be announced by color name and selection state, the theme and sound options SHALL be announced as radio buttons, and section titles SHALL be announced as headings.

#### Scenario: Color swatch
- **WHEN** TalkBack focuses the selected teal swatch
- **THEN** it announces "Teal, selected" as a radio button

### Requirement: Large fonts
With the phone's largest font size, the remaining time SHALL shrink to fit its space rather than being cut off, and all text SHALL remain readable.

#### Scenario: Largest font size
- **WHEN** the phone's font size is set to the largest value
- **THEN** each timer card shows its full remaining time

### Requirement: Right-to-left layouts
Layouts and dialogs SHALL mirror in right-to-left languages, with text aligned to the start of the line.

#### Scenario: Arabic
- **WHEN** the phone's language is Arabic
- **THEN** dialog text is right-aligned and buttons, chevrons and back arrows are mirrored
