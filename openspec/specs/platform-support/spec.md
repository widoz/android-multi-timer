# platform-support Specification

## Purpose
Defines which Android versions the app runs on, how behavior differs between them, and how releases are versioned and signed so updates install over earlier versions.

## Requirements
### Requirement: Supported Android versions
The app SHALL install and run on Android 11 (API 30) and later. Behavior differences SHALL be limited to: Android 11–12 have no notification permission prompt; Android 12 exact alarms can be revoked by the user; Android 11 may briefly show the phone's theme at startup; the monochrome themed icon appears on Android 13 and later.

#### Scenario: Android 10 phone
- **WHEN** the APK is opened on an Android 10 phone
- **THEN** Android refuses to install it

#### Scenario: Android 11 phone
- **WHEN** the APK is installed on an Android 11 phone
- **THEN** all features work, with the differences listed above

### Requirement: Signed releases that update in place
Every release SHALL be signed with the same release key and SHALL increase its version code, so that it installs over any earlier release while keeping the user's timers and settings.

#### Scenario: Update from 0.3.0 to 0.4.0
- **WHEN** a user with 0.3.0 installs 0.4.0
- **THEN** the app updates without uninstalling, and timers, sounds and theme are preserved

### Requirement: Backups
Saved timers and settings SHALL be included in Android's backup and device-to-device transfer.

#### Scenario: New phone
- **WHEN** the user moves to a new phone with Android's device transfer
- **THEN** the app's timers and settings come across

### Requirement: No data collection
The app SHALL work fully offline and SHALL NOT collect, transmit or share any personal data; it SHALL request no network permission.

#### Scenario: Airplane mode
- **WHEN** the phone is in airplane mode
- **THEN** every feature of the app works
