#!/usr/bin/env sh
set -eu

: "${ADMOB_APP_ID:?ADMOB_APP_ID is required}"
: "${ADMOB_REWARDED_UNIT_ID:?ADMOB_REWARDED_UNIT_ID is required}"
: "${PRIVACY_POLICY_URL:?PRIVACY_POLICY_URL is required}"
: "${SUPPORT_EMAIL:?SUPPORT_EMAIL is required}"
: "${RELEASE_STORE_FILE:?RELEASE_STORE_FILE is required}"
: "${RELEASE_STORE_PASSWORD:?RELEASE_STORE_PASSWORD is required}"
: "${RELEASE_KEY_ALIAS:?RELEASE_KEY_ALIAS is required}"
: "${RELEASE_KEY_PASSWORD:?RELEASE_KEY_PASSWORD is required}"

case "$PRIVACY_POLICY_URL" in https://*) ;; *) echo "PRIVACY_POLICY_URL must use HTTPS" >&2; exit 1 ;; esac

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
python3 "$SCRIPT_DIR/release-readiness.py" --repository-only

exec sh "$SCRIPT_DIR/run-gradle.sh" \
  :androidApp:validateReleaseConfiguration \
  :domain:test \
  :shared:test \
  :data:test \
  :simulation:test \
  :game:test \
  :androidApp:lintRelease \
  :androidApp:bundleRelease
