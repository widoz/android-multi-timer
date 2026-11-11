---
name: new-feature
description: The required order for any new feature or behavior change in Multi Timer — mockup first and wait for the product owner's approval, then an OpenSpec change, ADR if needed, Android implementation, verification, docs, release and archive. Use whenever the user asks for a new feature, a "As a user I want…" story, or a change to how the app looks or behaves. Not for pure bug fixes that restore already-specified behavior.
---

# New feature workflow

Follow these steps in order. **Never write Android code before step 3 is approved.**
Decisions already recorded in `docs/adr` and constraints in `openspec/config.yaml` apply throughout.

## 1. Sync and understand

1. `git fetch origin main --tags`. If local `main` differs from `origin/main` (the owner re-signs
   commits on their Mac), confirm `git status --porcelain` is empty and the trees are identical,
   then `git reset --hard origin/main`. Never discard uncommitted work without asking.
2. Restate the request as a user story. Read the affected specs first:
   `npx -y @fission-ai/openspec@1.16.1 list --specs`, then `… show <spec> --type spec`.
3. Ask only questions whose answers change the design; otherwise propose sensible defaults
   and list them for confirmation in step 3.

## 2. Mockup variation

1. Copy `docs/design/mockup.html` to the scratchpad as a variation (keep the approved mockup
   untouched for comparison). Implement the feature there: same tokens, both themes,
   accessible markup, 48px targets, `prefers-reduced-motion`.
2. If the feature depends on timing or several states (e.g. timers finishing), add demo
   buttons beside the phone so the owner can trigger them.
3. Test before publishing: headless Playwright flow test of the new interactions in light and
   dark, axe-core (0 violations in every new screen state), and one screenshot review.
4. Publish as a **separate** artifact (new URL) and summarise how to try it.

## 3. Approval (stop here)

List every decision you made that the owner did not specify, as numbered questions, and wait.
Iterate on the variation until the owner says "approved". Apply follow-up requests to the
variation first, re-test, republish.

## 4. Feasibility check

Before coding, check the approved design against Android constraints (360dp-wide phones,
48dp touch targets, Android 11–15 differences, permissions). If something must change,
update the mockup to match, republish, and tell the owner exactly what changed and why.

## 5. OpenSpec change

1. `npx -y @fission-ai/openspec@1.16.1 new change <verb-noun>` (e.g. `add-full-screen-timer`).
2. Write `proposal.md` (Why, What Changes, Capabilities, Impact; cite the mockup approval and
   any ADRs), `design.md`, `tasks.md`, and delta specs under `specs/<capability>/spec.md`:
   `## ADDED Requirements` for new capabilities (with a `## Purpose`), `## MODIFIED Requirements`
   repeating the **full** existing requirement text plus all its scenarios. Requirements use
   SHALL; every requirement has `#### Scenario:` with `- **WHEN**` / `- **THEN**`. State Android
   versions for version-specific behavior and light/dark expectations for new colors.
3. `… validate <change> --strict` must pass.

## 6. ADR (when architecturally significant)

New services, channels, permissions, storage, dependencies, or a reversal of an earlier
decision: add `docs/adr/NNNN-title.md` from `0000-template.md` (next number), add it to
`docs/adr/README.md`, and mark superseded ADRs "Superseded by NNNN" instead of deleting them.

## 7. Implement

Kotlin + Jetpack Compose + Material 3, no third-party runtime libraries. All text in
`strings.xml`; colors only from `ui/theme/Theme.kt` tokens (outline for every border, ≥3:1);
48dp targets; TalkBack labels; large-font safe; guard APIs above minSdk 30.

## 8. Verify

Build with `ANDROID_HOME` set (add the session's Maven-mirror init script if Maven Central
rate-limits). All must pass:
- `./gradlew lintDebug` — 0 errors.
- `./gradlew testDebugUnitTest -Pscreenshots` — add or extend a Roborazzi test that renders the
  new UI (see `FullScreenScreenshotTest`), review the images in both themes, and call
  `Snapshot.sendApplyNotifications()` after changing test state.
- `python3 tools/make_screens.py` if the timer list changed (README screenshots).

## 9. Docs, release, archive

1. Copy the final variation to `docs/design/mockup.html`; update `docs/README.md` and the README
   highlights.
2. Bump `versionCode` (+1) and `versionName` (semver) in `app/build.gradle.kts`;
   `./gradlew assembleRelease`; verify with `apksigner verify --print-certs` (same certificate
   as before) and check permissions with `aapt2 dump permissions`.
3. Tick `tasks.md`, then `… archive <change> --yes` and `… validate --all --strict`.
4. Commit as `Guido Scialfa <dev@guidoscialfa.com>` (repo git config) and push `main`.
   Tag `vX.Y.Z` locally; tag pushes are blocked from cloud sessions.
5. Send the signed APK and tell the owner to re-sign the new commit and push the tag from
   their Mac:
   `git rebase --force-rebase --gpg-sign --committer-date-is-author-date origin/main~N`,
   `git push --force-with-lease origin main`, `git tag -s vX.Y.Z -m "Multi Timer X.Y.Z"`,
   `git push origin vX.Y.Z`.

Never commit `keystore.properties` or `*.jks`.
