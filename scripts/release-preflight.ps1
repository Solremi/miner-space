$ErrorActionPreference = "Stop"

$required = @(
  "ADMOB_APP_ID",
  "ADMOB_REWARDED_UNIT_ID",
  "PRIVACY_POLICY_URL",
  "SUPPORT_EMAIL",
  "RELEASE_STORE_FILE",
  "RELEASE_STORE_PASSWORD",
  "RELEASE_KEY_ALIAS",
  "RELEASE_KEY_PASSWORD"
)

foreach ($name in $required) {
  if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
    throw "$name is required"
  }
}

if (-not $env:PRIVACY_POLICY_URL.StartsWith("https://")) {
  throw "PRIVACY_POLICY_URL must use HTTPS"
}

./gradlew.bat :androidApp:validateReleaseConfiguration :domain:test :shared:test :data:test :simulation:test :androidApp:bundleRelease
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
