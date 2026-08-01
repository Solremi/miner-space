$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

python scripts/release-readiness.py @args
exit $LASTEXITCODE
