$ErrorActionPreference = "Stop"

$GradleVersion = "9.5.0"
$ExpectedSha256 = "553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746"
$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$BootstrapRoot = Join-Path $ProjectRoot ".gradle-bootstrap"
$Archive = Join-Path $BootstrapRoot "gradle-$GradleVersion-bin.zip"
$Distribution = Join-Path $BootstrapRoot "gradle-$GradleVersion"
$Url = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

New-Item -ItemType Directory -Force -Path $BootstrapRoot | Out-Null

if (-not (Test-Path $Archive)) {
    Invoke-WebRequest -Uri $Url -OutFile $Archive
}

$ActualSha256 = (Get-FileHash -Path $Archive -Algorithm SHA256).Hash.ToLowerInvariant()
if ($ActualSha256 -ne $ExpectedSha256) {
    Remove-Item $Archive -Force
    throw "Checksum Gradle invalide. Attendu: $ExpectedSha256, obtenu: $ActualSha256"
}

if (-not (Test-Path $Distribution)) {
    Expand-Archive -Path $Archive -DestinationPath $BootstrapRoot -Force
}

$Gradle = Join-Path $Distribution "bin\gradle.bat"
Push-Location $ProjectRoot
try {
    & $Gradle wrapper --gradle-version $GradleVersion --distribution-type bin
    & $Gradle wrapper
}
finally {
    Pop-Location
}
