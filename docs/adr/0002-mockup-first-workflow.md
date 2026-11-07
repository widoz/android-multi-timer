# 0002. Mockup-first workflow

- Status: Accepted
- Date: 2026-11-06

## Context
The product owner wanted to see and try the app before code was written, and later asked that every UI change be shown first for approval. Building and installing an APK for each iteration is slow, especially when the developer cannot run an emulator.

## Decision
Keep an interactive HTML mockup of the app (`docs/design/mockup.html`) that behaves like the real app: live timers, dialogs, settings, theme switching and the real bundled sounds. Every user-visible change is made in the mockup first, approved by the product owner, and only then implemented in Android code. The mockup is also used for automated accessibility checks (axe-core) and contrast calculations.

## Alternatives considered
- Static images or Figma: cannot demonstrate timing, sounds or interactions.
- Iterating directly in the app: each iteration needs a build and an install on a phone.

## Consequences
- Fast design iteration (many rounds on colors, borders, dialogs, sounds) without builds.
- The mockup must be kept in sync with the app; it is a design reference, not production code.
- Some behavior cannot be shown in a browser (phone sounds, notifications, background alarms) and is only described there.
