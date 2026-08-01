#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = (
    "README.md",
    "gradle.properties",
    "androidApp/build.gradle.kts",
    "androidApp/src/main/AndroidManifest.xml",
    "assets/legal/privacy-policy-fr.md",
    "assets/legal/third-party-notices.md",
    "docs/data-safety.md",
    "docs/play-store-listing-fr.md",
    "docs/release-checklist.md",
    "docs/release-rollout-and-rollback.md",
    "docs/save-compatibility-matrix.md",
    "docs/closed-test-plan.md",
    "docs/asset-production-pack.md",
)

EVIDENCE_FLAGS = (
    "signing_key_tested",
    "signed_bundle_installed",
    "play_assets_ready",
    "fresh_save_campaign_passed",
    "migration_matrix_passed",
    "contextual_ads_device_passed",
    "layout_matrix_passed",
    "accessibility_130_passed",
    "low_end_30fps_passed",
    "consent_matrix_passed",
    "privacy_https_published",
    "data_safety_validated",
    "stability_30min_passed",
    "closed_test_passed",
    "no_blocking_crash_anr",
    "play_forms_completed",
    "progressive_rollout_configured",
    "rollback_owner_confirmed",
)

EVIDENCE_VALUES = (
    "tested_commit",
    "signed_bundle_sha256",
    "privacy_policy_url",
    "test_report_location",
)


def parse_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def contains(path: str, text: str) -> bool:
    file = ROOT / path
    return file.exists() and text in file.read_text(encoding="utf-8")


def repository_checks() -> list[str]:
    errors: list[str] = []
    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            errors.append(f"missing required file: {relative}")

    properties = parse_properties(ROOT / "gradle.properties")
    version = properties.get("MINER_SPACE_VERSION", "")
    version_code = properties.get("MINER_SPACE_VERSION_CODE", "")
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?", version):
        errors.append("MINER_SPACE_VERSION is not semantic")
    if not version_code.isdigit() or int(version_code) <= 0:
        errors.append("MINER_SPACE_VERSION_CODE must be a positive integer")

    manifest_requirements = {
        'android:allowBackup="false"': "Android backup must remain disabled",
        'android:usesCleartextTraffic="false"': "cleartext traffic must remain disabled",
        'android:screenOrientation="sensorLandscape"': "sensor landscape must remain configured",
        'android:dataExtractionRules="@xml/data_extraction_rules"': "data extraction rules are missing",
    }
    for token, message in manifest_requirements.items():
        if not contains("androidApp/src/main/AndroidManifest.xml", token):
            errors.append(message)

    build_requirements = (
        "validateReleaseConfiguration",
        "isMinifyEnabled = true",
        "isShrinkResources = true",
        "PRIVACY_POLICY_URL",
        "RELEASE_STORE_FILE",
        "ADMOB_REWARDED_UNIT_ID",
    )
    for token in build_requirements:
        if not contains("androidApp/build.gradle.kts", token):
            errors.append(f"release build guard missing: {token}")

    if (ROOT / ".github" / "workflows").exists():
        errors.append("unexpected workflow directory: .github/workflows")

    critical_sources = (
        "data/src/main/kotlin/fr/solremi/minerspace/data/transaction/SaveTransactionCoordinator.kt",
        "data/src/main/kotlin/fr/solremi/minerspace/data/manufacturing/ManufacturingCoordinator.kt",
        "data/src/main/kotlin/fr/solremi/minerspace/data/ads/ContextualRewardedAdCoordinator.kt",
        "shared/src/main/kotlin/fr/solremi/minerspace/shared/diagnostics/GameDiagnostics.kt",
    )
    for relative in critical_sources:
        file = ROOT / relative
        if not file.is_file():
            errors.append(f"critical consolidation source missing: {relative}")
        elif "PLACEHOLDER" in file.read_text(encoding="utf-8"):
            errors.append(f"placeholder remains in critical source: {relative}")

    return errors


def evidence_checks(path: Path) -> list[str]:
    evidence = parse_properties(path)
    errors: list[str] = []
    if not evidence:
        return [f"release evidence file not found or empty: {path}"]
    for key in EVIDENCE_FLAGS:
        if evidence.get(key, "").lower() != "true":
            errors.append(f"external evidence not validated: {key}")
    for key in EVIDENCE_VALUES:
        if not evidence.get(key, "").strip():
            errors.append(f"external evidence value missing: {key}")
    privacy_url = evidence.get("privacy_policy_url", "")
    if privacy_url and not privacy_url.startswith("https://"):
        errors.append("privacy_policy_url evidence must use HTTPS")
    bundle_hash = evidence.get("signed_bundle_sha256", "")
    if bundle_hash and not re.fullmatch(r"[0-9a-fA-F]{64}", bundle_hash):
        errors.append("signed_bundle_sha256 must contain 64 hexadecimal characters")
    commit = evidence.get("tested_commit", "")
    if commit and not re.fullmatch(r"[0-9a-fA-F]{40}", commit):
        errors.append("tested_commit must be a complete 40-character commit SHA")
    return errors


def print_section(title: str, errors: list[str]) -> None:
    print(f"\n{title}: {'PASS' if not errors else 'FAIL'}")
    for error in errors:
        print(f"  - {error}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Miner Space release readiness.")
    parser.add_argument(
        "--repository-only",
        action="store_true",
        help="Check repository-controlled requirements without external release evidence.",
    )
    parser.add_argument(
        "--evidence",
        type=Path,
        default=Path(os.environ.get("MINER_SPACE_RELEASE_EVIDENCE", ROOT / "release-evidence.properties")),
        help="Path to the private release evidence properties file.",
    )
    args = parser.parse_args()

    repository_errors = repository_checks()
    print_section("Repository checks", repository_errors)
    if repository_errors:
        print("\nRESULT: NO-GO — repository checks failed")
        return 1
    if args.repository_only:
        print("\nRESULT: REPOSITORY READY — external release evidence not evaluated")
        return 0

    evidence_errors = evidence_checks(args.evidence)
    print_section("External evidence", evidence_errors)
    if evidence_errors:
        print("\nRESULT: NO-GO — repository is ready but publication evidence is incomplete")
        return 2
    print("\nRESULT: GO — repository and external evidence are complete")
    return 0


if __name__ == "__main__":
    sys.exit(main())
