# 0004. Timer model, single repository and JSON persistence

- Status: Accepted
- Date: 2026-11-06

## Context
Timers must stay correct when the app is in the background, killed, updated or the phone restarts. Many places change timers: the UI, notification buttons, alarm broadcasts and the boot receiver.

## Decision
- A running timer stores its wall-clock end time (`endAt`); a paused timer stores the time left. Remaining time is always computed, never counted down, so nothing drifts.
- One process-wide repository (`TimerRepository`) owns the list as a `StateFlow` and is the only place that changes timers. Every change is saved and then applies its side effects for that timer: schedule or cancel its alarm, update its notification, start the alarm service when it finishes.
- Timers and settings are stored as JSON in SharedPreferences, using the platform's `org.json`.
- Optional fields (such as a timer's sound) are nullable and read with defaults, so older saved data keeps loading.

## Alternatives considered
- Room database: adds a dependency and annotation processing for a list of a few dozen items.
- Counting down in a service: drifts, and dies with the process.
- DataStore: adds a dependency; SharedPreferences is sufficient for this size.

## Consequences
- Simple, testable state transitions in one file.
- The repository holds the application context (lint `StaticFieldLeak` suppressed deliberately).
- Wall-clock end times mean changing the phone's clock changes when timers end.
