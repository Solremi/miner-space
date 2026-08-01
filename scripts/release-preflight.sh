#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if command -v python3 >/dev/null 2>&1; then
  PYTHON=python3
elif command -v python >/dev/null 2>&1; then
  PYTHON=python
else
  echo "Python 3 is required for release preflight" >&2
  exit 1
fi

"$PYTHON" "$SCRIPT_DIR/source-safety-check.py"

: "${ADMOB_APP_ID:?ADMOB_APP_ID is required}"
: "${ADMOB_REWARDED_UNIT_ID:?ADMOB_REWARDED_UNIT_ID is required}"
: "${PRIVACY_POLICY_URL:?PRIVACY_POLICY_URL is required}"
: "${SUPPORT_EMAIL:?SUPPORT_EMAIL is required}"
: "${RELEASE_STORE_FILE:?RELEASE_STORE_FILE is required}"
: "${RELEASE_STORE_PASSWORD:?RELEASE_STORE_PASSWORD is required}"
: "${RELEASE_KEY_ALIAS:?RELEASE_KEY_ALIAS is required}"
: "${RELEASE_KEY_PASSWORD:?RELEASE_KEY_PASSWORD is required}"

case "$PRIVACY_POLICY_URL" in https://*) ;; *) echo "PRIVACY_POLICY_URL must use HTTPS" >&2; exit 1 ;; esac

"$PYTHON" "$SCRIPT_DIR/release-readiness.py" --repository-only

exec sh "$SCRIPT_DIR/run-gradle.sh" \
  :androidApp:validateReleaseConfiguration \
  :domain:test \
  :shared:test \
  :data:test \
  :simulation:test \
  :game:test \
  :androidApp:lintRelease \
  :androidApp:bundleRelease
