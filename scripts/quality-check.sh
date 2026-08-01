#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

if command -v python3 >/dev/null 2>&1; then
  python3 "$SCRIPT_DIR/source-safety-check.py"
elif command -v python >/dev/null 2>&1; then
  python "$SCRIPT_DIR/source-safety-check.py"
else
  echo "Python 3 is required for source-safety-check.py" >&2
  exit 1
fi

exec sh "$SCRIPT_DIR/run-gradle.sh" qualityCheck
