# 0012. Minimum Android version 11 (API 30)

- Status: Accepted
- Date: 2026-11-07
- Supersedes: the initial minimum of Android 13 (API 33)

## Context
The app first required Android 13, as originally requested. A shared APK then failed to install on another phone with a parse error, the usual symptom of a too-old Android version. Android 11 and 12 phones are still common.

## Decision
Lower `minSdk` to 30 and guard version-specific APIs:
- Exact alarms: see [0005](0005-exact-alarms.md).
- Notification permission prompt only on Android 13+; elsewhere check the app's notification switch.
- Theme choice: see [0010](0010-neutral-theme-and-in-app-theme-choice.md).
- Dynamic color only on Android 12+.
- `fullBackupContent` rules for Android 11 alongside `dataExtractionRules`.
Lint's NewApi check must pass with no errors.

## Consequences
- More phones can install the app; behavior differences are listed in the platform-support spec.
- Android 11 and 12 need testing on real phones; the build machine cannot run an emulator.
