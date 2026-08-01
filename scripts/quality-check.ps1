$ErrorActionPreference = "Stop"
& (Join-Path $PSScriptRoot "run-gradle.ps1") qualityCheck
exit $LASTEXITCODE
