# 0001. Record architecture decisions

- Status: Accepted
- Date: 2026-11-07

## Context
Multi Timer was designed and built through a long conversation between the product owner and an AI assistant, with many decisions taken, revised and reversed along the way (for example how alarms ring, which Android versions to support, and the visual theme). Without a record, the reasons behind the current design would be lost.

## Decision
Record architecturally significant decisions as ADRs in `docs/adr`, numbered sequentially, using the format in `0000-template.md`. Decisions are never deleted: a reversed decision is marked "Superseded by" its replacement. Behavior (what the app does) is specified separately with OpenSpec in `openspec/specs`; ADRs explain why it is built that way.

## Consequences
- New contributors can read why things are as they are before changing them.
- Every significant change needs an ADR or an update to an existing one's status.
