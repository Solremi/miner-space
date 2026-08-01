$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.properties"
$bootstrapRoot = Join-Path $projectRoot ".gradle-bootstrap"
$properties = @{}

Get-Content $propertiesPath | ForEach-Object {
  if ($_ -match '^([^#=]+)=(.*)$') {
    $properties[$matches[1].Trim()] = $matches[2].Trim()
  }
}

$url = $properties["distributionUrl"].Replace('\:', ':')
$expectedSha256 = $properties["distributionSha256Sum"].ToLowerInvariant()
if ([string]::IsNullOrWhiteSpace($url)) { throw "distributionUrl is missing" }
if ([string]::IsNullOrWhiteSpace($expectedSha256)) { throw "distributionSha256Sum is missing" }

$archiveName = [System.IO.Path]::GetFileName($url)
$distributionName = $archiveName -replace '-bin\.zip$', ''
$archive = Join-Path $bootstrapRoot $archiveName
$distribution = Join-Path $bootstrapRoot $distributionName

New-Item -ItemType Directory -Force -Path $bootstrapRoot | Out-Null
if (-not (Test-Path $archive)) {
  Invoke-WebRequest -Uri $url -OutFile $archive
}

$actualSha256 = (Get-FileHash -Algorithm SHA256 -Path $archive).Hash.ToLowerInvariant()
if ($actualSha256 -ne $expectedSha256) {
  Remove-Item -Force $archive
  throw "Gradle distribution checksum mismatch"
}

if (-not (Test-Path $distribution)) {
  Expand-Archive -Path $archive -DestinationPath $bootstrapRoot -Force
}

Push-Location $projectRoot
try {
  & (Join-Path $distribution "bin\gradle.bat") @args
  exit $LASTEXITCODE
} finally {
  Pop-Location
}
