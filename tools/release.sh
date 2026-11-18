#!/usr/bin/env bash
# Builds, signs, tags and publishes a Multi Timer release from the tip of main.
#
#   tools/release.sh            # full release
#   tools/release.sh --dry-run  # everything up to the build and checks; no tag, no push, no release
#
# Needs: JDK 17–24 (JAVA_HOME), the Android SDK (ANDROID_HOME or local.properties),
# keystore.properties at the project root, a GPG key for `git tag -s`, and `gh` logged in.
#
# Steps:
#   1. Checks tools and a clean working tree, then updates main from origin.
#   2. Asks for the tag (default: v<versionName> from app/build.gradle.kts, which must match).
#   3. Drafts release notes from the commits since the previous v* tag; you can edit them.
#   4. Builds the signed release APK and checks its certificate matches the previous release.
#   5. After a final confirmation: signed tag, push, GitHub release with the APK attached.
set -euo pipefail

DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    -h|--help) sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $arg (try --help)" >&2; exit 2 ;;
  esac
done

cd "$(dirname "$0")/.."

say()  { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[33m!! %s\033[0m\n' "$*" >&2; }
die()  { printf '\033[31mxx %s\033[0m\n' "$*" >&2; exit 1; }
confirm() { # confirm "Question" -> returns 0 on y/yes
  local answer
  read -r -p "$1 [y/N] " answer
  [[ "$answer" =~ ^[Yy]([Ee][Ss])?$ ]]
}

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# ---------------------------------------------------------------- 1. checks
say "Checking tools"

command -v java >/dev/null || die "Java not found. Install JDK 21 (brew install --cask temurin@21) and set JAVA_HOME."
JAVA_MAJOR=$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.specification.version/ {print $2}')
JAVA_MAJOR=${JAVA_MAJOR%%.*}
if (( JAVA_MAJOR < 17 || JAVA_MAJOR > 24 )); then
  die "Java $JAVA_MAJOR is in use; the build needs 17–24. Try: export JAVA_HOME=\$(/usr/libexec/java_home -v 21)"
fi
echo "Java $JAVA_MAJOR"

if [[ -z "${ANDROID_HOME:-}" ]]; then
  if [[ -f local.properties ]] && grep -q '^sdk.dir=' local.properties; then
    ANDROID_HOME=$(grep '^sdk.dir=' local.properties | cut -d= -f2-)
  elif [[ -d "$HOME/Library/Android/sdk" ]]; then
    ANDROID_HOME="$HOME/Library/Android/sdk"
  else
    die "Android SDK not found. Set ANDROID_HOME or open the project once in Android Studio."
  fi
fi
export ANDROID_HOME
APKSIGNER=$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name apksigner 2>/dev/null | sort -V | tail -1)
[[ -x "$APKSIGNER" ]] || die "apksigner not found in $ANDROID_HOME/build-tools. Install build-tools with the SDK Manager."
echo "Android SDK: $ANDROID_HOME"

command -v gh >/dev/null || die "GitHub CLI not found: brew install gh, then gh auth login."
gh auth status >/dev/null 2>&1 || die "gh is not logged in: run gh auth login."
git config user.signingkey >/dev/null || warn "No user.signingkey in git config; git tag -s will use your default GPG key."

[[ -f keystore.properties ]] || die "keystore.properties is missing: the APK would be unsigned."
STORE_FILE=$(sed -n 's/^storeFile=//p' keystore.properties | sed 's/[[:space:]]*$//')
[[ -n "$STORE_FILE" ]] || die "keystore.properties has no storeFile= line."
[[ "$STORE_FILE" != "~"* ]] || die "storeFile starts with ~, which Gradle doesn't expand. Use the full path: ${STORE_FILE/#\~/$HOME}"
[[ "$STORE_FILE" == /* ]] || STORE_FILE="$PWD/$STORE_FILE"
[[ -f "$STORE_FILE" ]] || die "Signing key not found at $STORE_FILE (storeFile in keystore.properties)."
[[ -z "$(git status --porcelain)" ]] || die "Working tree has uncommitted changes. Commit or stash them first."

say "Updating main"
git checkout --quiet main
git pull --ff-only --quiet origin main
git fetch --quiet --tags origin
echo "main is at $(git log -1 --format='%h %s')"

# ---------------------------------------------------------------- 2. tag
VERSION_NAME=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' app/build.gradle.kts)
VERSION_CODE=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' app/build.gradle.kts)
[[ -n "$VERSION_NAME" ]] || die "Could not read versionName from app/build.gradle.kts."
PREV_TAG=$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || true)

say "Tag"
echo "app/build.gradle.kts: versionName $VERSION_NAME, versionCode $VERSION_CODE"
echo "Previous release tag: ${PREV_TAG:-none}"
read -r -p "Tag to create [v$VERSION_NAME]: " TAG
TAG=${TAG:-v$VERSION_NAME}
[[ "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "Tag must look like v1.2.3, got: $TAG"
VERSION=${TAG#v}
[[ "$VERSION" == "$VERSION_NAME" ]] || die "Tag $TAG doesn't match versionName $VERSION_NAME. Bump versionName/versionCode first."
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null && die "Tag $TAG already exists."
[[ -z "$(git ls-remote --tags origin "refs/tags/$TAG")" ]] || die "Tag $TAG already exists on origin."

# ---------------------------------------------------------------- 3. notes
say "Release notes"
NOTES="$WORK/notes.md"
RANGE=${PREV_TAG:+$PREV_TAG..}HEAD
git log "$RANGE" --no-merges --reverse --format='- %s' | sed -E 's/ \(#[0-9]+\)$//' > "$NOTES"
[[ -s "$NOTES" ]] || die "No commits since $PREV_TAG: nothing to release."
echo "Commits in ${PREV_TAG:-the history} → HEAD:"
echo
cat "$NOTES"
echo
while true; do
  read -r -p "Use these notes? [y]es / [e]dit / [n]o, abort: " answer
  case "$answer" in
    y|Y|"") break ;;
    e|E) "${EDITOR:-nano}" "$NOTES"; echo; cat "$NOTES"; echo ;;
    *) die "Aborted." ;;
  esac
done
[[ -s "$NOTES" ]] || die "Release notes are empty."

# ---------------------------------------------------------------- 4. build
say "Building the release APK"
./gradlew --quiet clean assembleRelease
BUILT=app/build/outputs/apk/release/app-release.apk
[[ -f "$BUILT" ]] || die "No signed APK at $BUILT (is keystore.properties correct?)."
APK="$WORK/multi-timer-$VERSION.apk"
cp "$BUILT" "$APK"

say "Checking the signature"
"$APKSIGNER" verify --print-certs "$APK" > "$WORK/cert.txt" || die "apksigner verify failed."
NEW_CERT=$(awk -F': ' '/certificate SHA-256 digest/ {print $2; exit}' "$WORK/cert.txt")
echo "Certificate SHA-256: $NEW_CERT"
OLD_CERT=""
if [[ -n "$PREV_TAG" ]] && gh release download "$PREV_TAG" --pattern '*.apk' --dir "$WORK/prev" >/dev/null 2>&1; then
  OLD_CERT=$("$APKSIGNER" verify --print-certs "$(find "$WORK/prev" -name '*.apk' | head -1)" \
    | awk -F': ' '/certificate SHA-256 digest/ {print $2; exit}')
fi
if [[ -n "$OLD_CERT" ]]; then
  # A different key means phones can't update. Changing keys is a rare, deliberate event that gets
  # its own ADR (see docs/adr/0016), never a release-script option.
  [[ "$OLD_CERT" == "$NEW_CERT" ]] || die "Certificate differs from $PREV_TAG ($OLD_CERT). Phones can't update to this APK: check keystore.properties points at the release key."
  echo "Same certificate as $PREV_TAG."
else
  warn "Couldn't get the APK of ${PREV_TAG:-a previous release} to compare certificates."
  confirm "Is the certificate above the one you always sign with?" || die "Aborted."
fi

if (( DRY_RUN )); then
  KEEP="app/build/outputs/apk/release/multi-timer-$VERSION.apk"
  cp "$APK" "$KEEP"
  say "Dry run finished: no tag, no push, no release"
  echo "APK: $KEEP"
  exit 0
fi

# ---------------------------------------------------------------- 5. publish
say "Ready to publish"
echo "Tag:     $TAG (signed) on $(git log -1 --format='%h %s')"
echo "Release: $VERSION, with multi-timer-$VERSION.apk"
confirm "Create the tag, push it and publish the release on GitHub?" || die "Aborted. Nothing was published."

git tag -s "$TAG" -m "Multi Timer $VERSION"
git push origin "$TAG"
gh release create "$TAG" "$APK" --title "$VERSION" --notes-file "$NOTES" --verify-tag

say "Released $TAG"
gh release view "$TAG" --json url --jq .url
