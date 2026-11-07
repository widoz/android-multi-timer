# 0003. Native Kotlin with Jetpack Compose and Material 3

- Status: Accepted
- Date: 2026-11-06

## Context
The app must run reliable background alarms, rich notifications and a foreground service, which depend closely on Android platform APIs. It is small, single-platform, and maintained by one person.

## Decision
Write the app in Kotlin with Jetpack Compose and Material 3, using only AndroidX/Jetpack libraries (core-ktx, activity-compose, lifecycle-runtime-compose, Compose BOM, material-icons-extended). No third-party runtime dependencies. Gradle with Kotlin DSL and a version catalog (`gradle/libs.versions.toml`).

## Alternatives considered
- Flutter or React Native: extra layers between the app and alarm/notification APIs, larger APK, no benefit for an Android-only app.
- XML Views: more boilerplate; Compose is the current Android standard.

## Consequences
- Release APK about 2.3 MB after R8 shrinking (debug builds are much larger because of the icon library).
- Version upgrades are limited to AndroidX; see [0014](0014-publish-to-google-play.md) for the pending target-SDK update.
