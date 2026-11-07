# 0011. WCAG 2.2 AA as the accessibility baseline

- Status: Accepted
- Date: 2026-11-07

## Context
The product owner requested an accessibility audit of both themes against the AA standard.

## Decision
- All text at least 4.5:1, and control outlines, icons and timer colors at least 3:1, in both themes. The audit (every color pair, both themes) led to: outline #8A8A8A / #757575 (was 1.3–1.5:1), light finished orange #B53C0B, and adjusted light timer colors.
- `outline` is used for every border and line and must stay at least 3:1; `outlineVariant` is only for the progress-ring track.
- 48 dp minimum touch targets (default icon button and button sizes; 48 dp swatch targets).
- TalkBack labels for icon buttons, color swatches announced by name and selection, radio semantics for theme and sound options, headings for sections.
- The countdown shrinks to fit at large font sizes instead of clipping.
- Each mockup change is checked with axe-core (0 violations in every screen state, both themes).

## Consequences
- New colors must be contrast-checked in both themes before approval.
- Some screens are taller (48 dp targets); accepted.
