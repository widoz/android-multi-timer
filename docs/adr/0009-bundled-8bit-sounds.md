# 0009. Bundled original 8-bit sounds, Power chime as default

- Status: Accepted
- Date: 2026-11-07

## Context
The product owner wanted sounds that are the same on every phone. Sounds copied from a phone belong to its manufacturer and cannot be redistributed. A first set of synthesized sounds was then asked to be in 8-bit style.

## Decision
- Ship five original sounds synthesized from scratch by `tools/make_sounds.py` (numpy): Power chime, Pixel bell, Beeper, Gentle rise and Jingle. They use console-style techniques (pulse and triangle waves, 60 Hz stepped envelopes, arpeggios, noise ticks, DC blocking, 8-bit depth) with original melodies, each one phrase plus a rest for looping, loudness-matched.
- Encode as OGG Vorbis (`-q:a 5`) in `res/raw`, about 93 KB in total.
- Store sounds as strings: `app:<name>` for app sounds, the content URI for phone sounds, the phone's default alarm URI for "Phone's alarm sound", and `silent`. App sounds are resolved to `android.resource://<package>/<resource id>` only at play time, so stored values survive builds and resource shrinking.
- Power chime is the default. The previous default (phone's alarm sound) was never saved, so installs that never chose a default move to Power chime; explicit choices are kept.

## Alternatives considered
- Bundling phone sounds: licensing problem.
- CC0 downloads: per-file license checks; less distinctive.
- Storing resource ids or names: ids change between builds; names can be affected by resource optimization.

## Consequences
- The app owns its sounds outright and can regenerate or tweak them.
- The picker deduplicates phone sounds by name (media libraries often hold copies).
