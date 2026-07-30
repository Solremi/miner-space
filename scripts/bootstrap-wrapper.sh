#!/usr/bin/env sh
set -eu

GRADLE_VERSION="9.5.0"
EXPECTED_SHA256="553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BOOTSTRAP_ROOT="$PROJECT_ROOT/.gradle-bootstrap"
ARCHIVE="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION-bin.zip"
DISTRIBUTION="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

mkdir -p "$BOOTSTRAP_ROOT"

if [ ! -f "$ARCHIVE" ]; then
  curl --fail --location "$URL" --output "$ARCHIVE"
fi

ACTUAL_SHA256=$(sha256sum "$ARCHIVE" | awk '{print $1}')
if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
  rm -f "$ARCHIVE"
  echo "Checksum Gradle invalide." >&2
  exit 1
fi

if [ ! -d "$DISTRIBUTION" ]; then
  unzip -q "$ARCHIVE" -d "$BOOTSTRAP_ROOT"
fi

cd "$PROJECT_ROOT"
"$DISTRIBUTION/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
"$DISTRIBUTION/bin/gradle" wrapper
