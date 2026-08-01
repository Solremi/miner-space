$ErrorActionPreference = "Stop"

python (Join-Path $PSScriptRoot "source-safety-check.py")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& (Join-Path $PSScriptRoot "run-gradle.ps1") qualityCheck
exit $LASTEXITCODE
