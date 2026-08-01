$ErrorActionPreference = "Stop"
& (Join-Path $PSScriptRoot "run-gradle.ps1") wrapper --distribution-type bin
exit $LASTEXITCODE
