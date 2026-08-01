$ErrorActionPreference = "Stop"
& (Join-Path $PSScriptRoot "run-gradle.ps1") :simulation:test :game:test
exit $LASTEXITCODE
