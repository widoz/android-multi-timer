# 0016. New release signing key, created and kept locally

- Status: Accepted
- Date: 2026-11-18

## Context
[0013](0013-release-signing-and-versioning.md) signs releases with a key generated in a cloud session
(`multi-timer-release.jks`, alias `multitimer`; 0.5.0's certificate SHA-256 starts `8903d28e`).
The key and its `keystore.properties` stayed on that session's machine, which has since been
reclaimed, and no copy was found on the product owner's Mac or in the old session. The key is lost.

Android installs an update only when it is signed with the same key as the installed app, so no
build can update existing 0.5.0 installs any more.

## Decision
- Create a new release key on the product owner's Mac: RSA 4096, alias `multitimer`, stored in
  the project at `.keys/multi-timer-release.jks`. The `.keys/` folder is git-ignored, as are `*.jks`
  and `keystore.properties`. Back up the key file and its password in the owner's password manager
  as soon as it is created.
- `keystore.properties` (git-ignored, project root) points at it with the relative path
  `storeFile=.keys/multi-timer-release.jks`, resolved from the project root.
- 0.6.0 is the first release signed with the new key. Its release notes say to uninstall the
  previous version before installing.
- `tools/release.sh` compares each release's certificate with the APK of the previous release and
  stops on a change, unless it runs with `--new-key`, which accepts the change once after a
  confirmation and adds the uninstall note to the release notes.
- This key is also the upload key when the app moves to Google Play ([0014](0014-publish-to-google-play.md)).

## Alternatives considered
- **Use the GitHub GPG key.** Android signs APKs with an X.509 certificate in a Java keystore, not
  with GPG keys. Converting one is possible but brittle, and any key other than the lost one breaks
  updates in the same way. It would also tie the app's signing identity to the git identity.
- **Recover the old key.** Not possible: it existed only on the reclaimed cloud machine.

## Consequences
- Phones with 0.5.0 or earlier must uninstall before installing 0.6.0, losing their saved timers.
  So far that is only the owner's own installs, since the app is sideloaded.
- From 0.6.0 on, releases update each other again, as long as the new key is kept.
- Keys are never generated in temporary cloud machines again: they are created where they are kept.
- The key lives inside the working copy, so deleting or re-cloning the project, or running
  `git clean -x`, removes it. The password-manager backup is the copy that must not be lost.
