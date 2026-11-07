# 0013. Release signing, versioning and distribution

- Status: Accepted
- Date: 2026-11-06

## Context
Debug builds are signed with a throwaway key that differs per machine, so they cannot update each other, and the build machine is temporary. The product owner installs releases on a phone and shares the APK with others.

## Decision
- Sign release builds with a dedicated release key (RSA 4096, alias `multitimer`), read from a git-ignored `keystore.properties` at the project root; without it, release builds are produced unsigned. The key is backed up by the product owner and never committed.
- Every release increases `versionCode` and `versionName` (semantic versioning) and is tagged in git (`v0.1.0` … `v0.4.0`).
- Distribute the signed release APK directly (sideloading) until the app is published; see [0014](0014-publish-to-google-play.md).

## Consequences
- Releases install over each other, keeping user data.
- Losing the key means installed copies cannot be updated.
- The key was generated in a cloud session; the owner may replace it with one created locally (before publishing, since Play ties the app to its upload key).
