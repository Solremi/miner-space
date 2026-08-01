$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

& .\gradlew.bat :game:test :androidApp:connectedDebugAndroidTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
