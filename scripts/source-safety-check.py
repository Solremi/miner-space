#!/usr/bin/env python3
"""Static guardrails for save safety, architecture, and local-only development."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        errors.append(f"missing required file: {relative}")
        return ""
    return path.read_text(encoding="utf-8")


def require_tokens(relative: str, *tokens: str) -> None:
    content = read(relative)
    for token in tokens:
        if token not in content:
            errors.append(f"required feature missing in {relative}: {token}")


def require_absent(relative: str) -> None:
    if (ROOT / relative).exists():
        errors.append(f"obsolete file must stay removed: {relative}")


# The project is intentionally local-only. Never reintroduce automation files.
if (ROOT / ".github" / "workflows").exists():
    errors.append("workflow directory is forbidden for this project")

for module in ("androidApp", "game", "domain", "data", "simulation", "shared"):
    if not (ROOT / module / "build.gradle.kts").is_file():
        errors.append(f"missing Gradle module: {module}")

# Domain remains independent from Android, LibGDX, and advertising SDK implementations.
domain_root = ROOT / "domain" / "src" / "main" / "kotlin"
for source in domain_root.rglob("*.kt"):
    content = source.read_text(encoding="utf-8")
    for forbidden in ("com.badlogic.gdx", "android.", "com.google.android.gms"):
        if forbidden in content:
            errors.append(f"domain purity violation in {source.relative_to(ROOT)}: {forbidden}")

# Save transaction and rollback architecture.
require_tokens(
    "data/src/main/kotlin/fr/solremi/minerspace/data/transaction/AtomicFeatureStateCoordinator.kt",
    "class RobotStateTransactionCoordinator",
    "class StrategyStateTransactionCoordinator",
    "class ProgressionStateTransactionCoordinator",
    "class AtomicSaveBundle",
)
require_tokens(
    "data/src/main/kotlin/fr/solremi/minerspace/data/manufacturing/ManufacturingCoordinator.kt",
    "class ManufacturingCoordinator",
    "lastSuccessfulSaveAtEpochMillis",
    "secondsSinceLastSave",
    "PersistenceFailed",
)
require_tokens(
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/services/GameServices.kt",
    "val deferredSave: DeferredSaveService? = null",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/presentation/PresentationController.kt",
    "services.deferredSave?.enqueue(payload)",
)
require_tokens(
    "androidApp/src/main/kotlin/fr/solremi/minerspace/android/platform/AndroidPlatformServices.kt",
    "CoalescingSaveService",
    "deferredSave = save",
    "save.flush(",
    "logger = AndroidDiagnosticLogger",
)
require_tokens(
    "androidApp/src/main/kotlin/fr/solremi/minerspace/android/MainActivity.kt",
    "logger = AndroidDiagnosticLogger",
)

# Ferrum is a feature package with explicit state and no global mutable visual bridge.
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/screen/FerrumCommandScreen.kt",
    "class FerrumCommandScreen",
    "FerrumScreenState",
    "FerrumActionController",
    "FerrumHudPresenter",
    "FerrumHudRenderer",
    "FerrumCameraController",
    "FerrumInputController",
    "developmentStage = state.development.stage",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/model/FerrumColonyDevelopment.kt",
    "enum class FerrumColonyStage",
    "data class FerrumColonyDevelopment",
    "fun from(state: ManufacturingGameState)",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/model/FerrumScreenState.kt",
    "data class FerrumScreenState",
    "var development: FerrumColonyDevelopment",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/text/FerrumTextCatalog.kt",
    "interface FerrumTextCatalog",
    "object FrenchFerrumText",
    "fun stageAnnouncement",
    "fun saveStatus",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/presentation/FerrumActionController.kt",
    "class FerrumActionController",
    "collectAllAvailable",
    "definition.sellable",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/presentation/FerrumProductionAssistant.kt",
    "object FerrumProductionAssistant",
    "FerrumTextCatalog",
    "missingOrReady",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/scene/FerrumPrimitiveScene.kt",
    "class FerrumPrimitiveScene",
    "developmentStage: FerrumColonyStage",
    "buildOrbitalStage",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/ui/FerrumHudRenderer.kt",
    "class FerrumHudRenderer",
)
require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/ferrum/input/FerrumInputController.kt",
    "class FerrumInputController",
)

for source in (ROOT / "game" / "src" / "main" / "kotlin").rglob("*.kt"):
    if "FerrumColonyVisualState" in source.read_text(encoding="utf-8"):
        errors.append(f"global Ferrum visual state reintroduced in {source.relative_to(ROOT)}")

# Active screens are physically grouped by feature and heavy rendering/layout code is extracted.
active_screen_requirements = {
    "game/src/main/kotlin/fr/solremi/minerspace/game/meteor/screen/MeteorShowerScreen.kt": (
        "class MeteorShowerScreen",
        "MeteorShowerRenderer",
        "MeteorShowerLayoutCalculator",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/meteor/screen/MeteorShowerUi.kt": (
        "class MeteorShowerRenderer",
        "object MeteorShowerLayoutCalculator",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/robots/screen/RobotFleetScreen.kt": (
        "class RobotFleetScreen",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/strategy/screen/StrategyLabScreen.kt": (
        "class StrategyLabScreen",
        "StrategyLabLayoutCalculator",
        "StrategyLabText",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/strategy/screen/StrategyLabLayout.kt": (
        "object StrategyLabLayoutCalculator",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/missions/screen/MissionControlScreen.kt": (
        "class MissionControlScreen",
        "MissionControlLayoutCalculator",
        "MissionControlText",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/missions/screen/MissionControlUi.kt": (
        "object MissionControlLayoutCalculator",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/narrative/screen/NarrativeArchiveScreen.kt": (
        "class NarrativeArchiveScreen",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/settings/screen/PresentationSettingsScreen.kt": (
        "class PresentationSettingsScreen",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/cryos/screen/CryosFrontierGatewayScreen.kt": (
        "class CryosFrontierGatewayScreen",
    ),
    "game/src/main/kotlin/fr/solremi/minerspace/game/frontier/screen/InterplanetaryFrontierScreen.kt": (
        "class InterplanetaryFrontierScreen",
    ),
}
for relative, tokens in active_screen_requirements.items():
    require_tokens(relative, *tokens)

screen_root = ROOT / "game" / "src" / "main" / "kotlin"
for source in screen_root.rglob("*Screen.kt"):
    size = source.stat().st_size
    if size > 22_000:
        errors.append(
            f"screen remains monolithic ({size} bytes > 22000): {source.relative_to(ROOT)}"
        )

# Old vertical slices and duplicate planet screens must not return.
for obsolete in (
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/EmptyPlanetScreen.kt",
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/FerrumVerticalSliceScreen.kt",
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/GameplayHubScreen.kt",
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/ManufacturingPlanetScreen.kt",
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/PlanetScreen.kt",
    "game/src/main/kotlin/fr/solremi/minerspace/game/screen/PresentationGameplayScreen.kt",
    "game/src/main/kotlin/fr/solremi/minerspace/game/exploration/screen/SectorExplorationScreen.kt",
):
    require_absent(obsolete)

# Large domain aggregates stay split into definitions, state, and engine files.
split_domain = {
    "assembly": ("AssemblyDefinitions.kt", "AssemblyState.kt", "AssemblyEngine.kt"),
    "refining": ("RefiningDefinitions.kt", "RefiningState.kt", "RefiningEngine.kt"),
    "robot": ("RobotDefinitions.kt", "RobotState.kt", "RobotAutomationEngine.kt"),
    "progression": ("ProgressionDefinitions.kt", "ProgressionState.kt", "ProgressionEngine.kt"),
    "ads": ("RewardedAdvertisingDefinitions.kt", "RewardedAdvertisingState.kt", "RewardedAdvertisingEngine.kt"),
    "event": ("MeteorEventDefinitions.kt", "MeteorEventState.kt", "MeteorEventEngine.kt", "MeteorRewardEngine.kt"),
    "strategy": ("StrategyDefinitions.kt", "StrategyState.kt", "StrategyEngine.kt"),
    "content": ("FerrumDeltaModels.kt", "FerrumDeltaContentValidator.kt"),
}
for package, filenames in split_domain.items():
    base = f"domain/src/main/kotlin/fr/solremi/minerspace/domain/{package}"
    for filename in filenames:
        read(f"{base}/{filename}")

for obsolete in (
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/assembly/Assembly.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/refining/Refining.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/robot/RobotAutomation.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/progression/Progression.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/ads/RewardedAdvertising.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/event/MeteorEvent.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/strategy/BuildStrategy.kt",
    "domain/src/main/kotlin/fr/solremi/minerspace/domain/content/FerrumDeltaContent.kt",
):
    require_absent(obsolete)

# Shared JSON parser remains the only parser used by content loaders.
for relative in (
    "data/src/main/kotlin/fr/solremi/minerspace/data/economy/CoreEconomyContentLoader.kt",
    "data/src/main/kotlin/fr/solremi/minerspace/data/progression/ProgressionContentLoader.kt",
):
    content = read(relative)
    if "StrictJson.parse" not in content:
        errors.append(f"content loader does not use StrictJson: {relative}")
    if "private class JsonParser" in content or "private class Parser" in content:
        errors.append(f"duplicated JSON parser remains in {relative}")

require_tokens(
    "game/src/main/kotlin/fr/solremi/minerspace/game/MinerSpaceGame.kt",
    "InitialRoute.FERRUM -> setScreen<FerrumCommandScreen>()",
    "game.ferrum.screen.FerrumCommandScreen",
)
require_tokens(
    "androidApp/build.gradle.kts",
    "jniLibs.srcDir(generatedNatives.get().asFile)",
    "resValues = true",
)

if errors:
    print("SOURCE SAFETY CHECK: FAILED", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    raise SystemExit(1)

print("SOURCE SAFETY CHECK: OK")
