#!/usr/bin/env python3
"""Static guardrails for save safety and local development hygiene."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        errors.append(f"missing required file: {relative}")
        return ""
    return path.read_text(encoding="utf-8")


errors: list[str] = []

if (ROOT / ".github" / "workflows").exists():
    errors.append("workflow directory is forbidden for this project")

coordinator = read(
    "data/src/main/kotlin/fr/solremi/minerspace/data/transaction/"
    "AtomicFeatureStateCoordinator.kt"
)
for symbol in (
    "class RobotStateTransactionCoordinator",
    "class StrategyStateTransactionCoordinator",
    "class ProgressionStateTransactionCoordinator",
    "class AtomicSaveBundle",
):
    if symbol not in coordinator:
        errors.append(f"missing atomic transaction symbol: {symbol}")

screen_requirements = {
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/RobotFleetScreen.kt": (
        "RobotStateTransactionCoordinator",
        "commitBoth(",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/StrategyLabScreen.kt": (
        "StrategyStateTransactionCoordinator",
        "transactions.commit(",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/MissionControlScreen.kt": (
        "ProgressionStateTransactionCoordinator",
        "transactions.commit(",
    ),
}
for relative, required in screen_requirements.items():
    content = read(relative)
    for token in required:
        if token not in content:
            errors.append(f"{relative} does not use {token}")
    for legacy in ("saveMain(oldMain)", "saveMain(nextMain)) {", "saveMain(nextMain);", "saveMain(nextMain) &&"):
        if legacy in content:
            errors.append(f"legacy manual rollback remains in {relative}: {legacy}")

all_screen_text = "\n".join(read(path) for path in screen_requirements)
for typo in ("SUHVERE", "VERROUILLI\""):
    if typo in all_screen_text:
        errors.append(f"known UI typo remains: {typo}")

coalescing = read("data/src/main/kotlin/fr/solremi/minerspace/data/CoalescingSaveService.kt")
for token in (
    "class CoalescingSaveService",
    "DeferredSaveService",
    "pending.remove(payload.slotId)",
    "miner-space-save-io",
):
    if token not in coalescing:
        errors.append(f"coalescing save guard missing: {token}")

services = read(
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/services/GameServices.kt"
)
if "val deferredSave: DeferredSaveService? = null" not in services:
    errors.append("GameServices does not expose optional deferred saves")

presentation = read(
    "game/src/main/kotlin/fr/solremi/minerspace/game/presentation/PresentationController.kt"
)
if "services.deferredSave?.enqueue(payload)" not in presentation:
    errors.append("presentation settings are not using the deferred save queue")

android_services = read(
    "androidApp/src/main/kotlin/fr/solremi/minerspace/android/platform/"
    "AndroidPlatformServices.kt"
)
for token in ("CoalescingSaveService", "deferredSave = save", "save.flush("):
    if token not in android_services:
        errors.append(f"Android save integration missing: {token}")

if errors:
    print("SOURCE SAFETY CHECK: FAILED", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    raise SystemExit(1)

print("SOURCE SAFETY CHECK: OK")
