# 0010. Neutral theme and in-app theme choice

- Status: Accepted
- Date: 2026-11-07

## Context
The first design used a teal accent with teal-tinted neutrals. The product owner preferred plain white for light, neutral grey for dark, grey controls, consistent borders, brighter timer colors in light mode, and an in-app theme toggle.

## Decision
- Light: white background, near-black controls. Dark: #121212 background, #1E1E1E cards, light-grey controls. No accent hue; color only for the five timer colors and the finished (orange) state.
- One outline color for every border and line (cards, dividers, chips, buttons, fields, menus); a lighter track only for progress rings.
- Timer colors have separate light (bright, at least 3:1 on white) and dark (pastel) variants, exposed through a composition local so they follow the app's theme.
- Theme choice (System default / Light / Dark) lives in Settings. On Android 12+ it is applied with `UiModeManager.setApplicationNightMode`, so the startup window, status bar and resources follow it. On Android 11 the app passes the choice to the Compose theme and to `enableEdgeToEdge` for system bar icons.
- Material You wallpaper colors remain available behind a flag (off), Android 12+ only.

## Consequences
- Android 11 may flash the phone's theme at startup when they differ.
- All colors are tokens in `ui/theme/Theme.kt`; see [0011](0011-wcag-aa-baseline.md) for the contrast rules they must satisfy.
