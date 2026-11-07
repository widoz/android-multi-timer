# 0014. Publish to Google Play

- Status: Proposed
- Date: 2026-11-07

## Context
The product owner asked to publish the app on Google Play. Requirements checked on 2026-11-07:
- New apps and updates must target Android 16 (API 36) since 2026-08-31 (developer.android.com/google/play/requirements/target-sdk). The app targets API 35.
- Personal developer accounts created after 2023-11-13 must run a closed test with at least 12 testers opted in for 14 continuous days before production access (Play Console Help, answer 14151465).
- The package name `dev.multitimer.app` is a placeholder and cannot change after the first upload.
- Play requires an Android App Bundle, a privacy policy, the Data safety form, content rating, and declarations for `USE_EXACT_ALARM` and the foreground service type.

## Decision (proposed)
1. Choose the final package name (suggested `com.guidoscialfa.multitimer`) and app name.
2. Update compileSdk/targetSdk to 36 and the Android Gradle Plugin accordingly; re-run lint and the release build.
3. Build a signed `.aab` with the release key as the upload key, using Play App Signing.
4. Prepare the listing (short and full description, 512×512 icon, 1024×500 feature graphic), a privacy policy stating no data is collected, Data safety answers, and permission declarations. Screenshots come from the real app.
5. Run the closed test if required, then apply for production.

## Open questions
Final package name, app name, account type and age, and listing languages (English, Italian).

## Consequences
- Changing the package name makes it a new app: sideloaded installs must be reinstalled once.
- Publication takes at least about 2–3 weeks if the closed-test rule applies.
