#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROPERTIES="$PROJECT_ROOT/gradle/wrapper/gradle-wrapper.properties"
BOOTSTRAP_ROOT="$PROJECT_ROOT/.gradle-bootstrap"

property() {
  sed -n "s/^$1=//p" "$PROPERTIES" | tail -n 1
}

DISTRIBUTION_URL=$(property distributionUrl | tr -d '\\')
EXPECTED_SHA256=$(property distributionSha256Sum)
[ -n "$DISTRIBUTION_URL" ] || { echo "distributionUrl is missing" >&2; exit 1; }
[ -n "$EXPECTED_SHA256" ] || { echo "distributionSha256Sum is missing" >&2; exit 1; }

ARCHIVE_NAME=$(basename "$DISTRIBUTION_URL")
DISTRIBUTION_NAME=$(printf '%s' "$ARCHIVE_NAME" | sed 's/-bin\.zip$//')
ARCHIVE="$BOOTSTRAP_ROOT/$ARCHIVE_NAME"
DISTRIBUTION="$BOOTSTRAP_ROOT/$DISTRIBUTION_NAME"

mkdir -p "$BOOTSTRAP_ROOT"
if [ ! -f "$ARCHIVE" ]; then
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location "$DISTRIBUTION_URL" --output "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$DISTRIBUTION_URL"
  else
    echo "curl or wget is required to bootstrap Gradle." >&2
    exit 1
  fi
fi

if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL_SHA256=$(sha256sum "$ARCHIVE" | awk '{print $1}')
elif command -v shasum >/dev/null 2>&1; then
  ACTUAL_SHA256=$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')
else
  echo "sha256sum or shasum is required to verify Gradle." >&2
  exit 1
fi

if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
  rm -f "$ARCHIVE"
  echo "Gradle distribution checksum mismatch." >&2
  exit 1
fi

if [ ! -d "$DISTRIBUTION" ]; then
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required to bootstrap Gradle." >&2; exit 1; }
  unzip -q "$ARCHIVE" -d "$BOOTSTRAP_ROOT"
fi

cd "$PROJECT_ROOT"
exec "$DISTRIBUTION/bin/gradle" "$@"
