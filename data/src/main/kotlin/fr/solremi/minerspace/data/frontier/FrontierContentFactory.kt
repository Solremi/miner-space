package fr.solremi.minerspace.data.frontier

import fr.solremi.minerspace.domain.frontier.*
import fr.solremi.minerspace.shared.GameId

object FrontierContentFactory {
    fun create(): FrontierDefinitions {
        val sectors = FrontierVisualFamily.entries.flatMap { family -> sectorTemplates(family) }.associateBy { it.id }
        val scarcity = id("modifier_resource_scarcity")
        val rich = id("modifier_rich_veins")
        val modifiers = listOf(
            modifier("modifier_low_gravity", "Faible gravité", ALL, emptySet(), emptySet(), 850_000, 900_000),
            modifier("modifier_dense_atmosphere", "Atmosphère dense", ALL, emptySet(), emptySet(), 1_100_000, 1_150_000),
            modifier(scarcity.value, "Ressources rares", ALL, setOf(rich), emptySet(), 1_250_000, 1_450_000),
            modifier(rich.value, "Veines abondantes", ALL, setOf(scarcity), emptySet(), 800_000, 850_000),
            modifier("modifier_unstable_orbit", "Orbite instable", ALL, emptySet(), setOf(FrontierCapability.NETWORK), 1_150_000, 1_250_000),
            modifier("modifier_electromagnetic_storm", "Orage électromagnétique", ALL, emptySet(), setOf(FrontierCapability.EVENT), 1_100_000, 1_200_000),
            modifier("modifier_ancient_beacons", "Balises anciennes", ALL, emptySet(), setOf(FrontierCapability.ARTIFACT), 950_000, 1_150_000),
            modifier("modifier_automated_ruins", "Ruines automatisées", ALL, emptySet(), setOf(FrontierCapability.CONSTRUCTION), 1_050_000, 1_200_000),
            modifier("modifier_thermal_fissures", "Failles thermiques", setOf(FrontierVisualFamily.VOLCANIC), emptySet(), emptySet(), 1_050_000, 1_150_000),
            modifier("modifier_crystal_resonance", "Résonance cristalline", setOf(FrontierVisualFamily.CRYSTALLINE), emptySet(), emptySet(), 950_000, 1_100_000),
            modifier("modifier_derelict_drones", "Drones à la dérive", setOf(FrontierVisualFamily.DERELICT), emptySet(), emptySet(), 1_100_000, 1_250_000),
            modifier("modifier_temporal_echo", "Écho temporel", ALL, emptySet(), emptySet(), 1_000_000, 1_300_000),
        ).associateBy { it.id }
        val objectives = listOf(
            objective("objective_extract_reserves", "Extraire les réserves", FrontierCapability.EXTRACTION, 120, FrontierRewardKind.PERMANENT_BONUS),
            objective("objective_refine_alloys", "Raffiner les alliages", FrontierCapability.REFINING, 110, FrontierRewardKind.COLLECTION),
            objective("objective_stabilize_network", "Stabiliser le réseau", FrontierCapability.NETWORK, 100, FrontierRewardKind.PERMANENT_BONUS),
            objective("objective_resolve_anomalies", "Résoudre les anomalies", FrontierCapability.EVENT, 90, FrontierRewardKind.COSMETIC),
            objective("objective_recover_artifacts", "Récupérer les artefacts", FrontierCapability.ARTIFACT, 95, FrontierRewardKind.COLLECTION),
            objective("objective_build_outpost", "Construire l’avant-poste", FrontierCapability.CONSTRUCTION, 105, FrontierRewardKind.COSMETIC),
        ).associateBy { it.id }
        return FrontierDefinitions(1, "1.0.0", sectors, modifiers, objectives)
    }

    private fun sectorTemplates(family: FrontierVisualFamily): List<FrontierSectorTemplate> {
        val prefix = family.name.lowercase()
        val names = when (family) {
            FrontierVisualFamily.VOLCANIC -> listOf("Plaine de cendres", "Forge basaltique", "Pont magmatique", "Caldeira active", "Crypte d’obsidienne", "Bastion thermique", "Faille minérale", "Relais de lave")
            FrontierVisualFamily.CRYSTALLINE -> listOf("Jardin prismatique", "Atelier de verre", "Nexus lumineux", "Tempête aurorale", "Sanctuaire de quartz", "Citadelle de glace", "Veine résonante", "Relais spectral")
            FrontierVisualFamily.DERELICT -> listOf("Soute éventrée", "Raffinerie fantôme", "Anneau de relais", "Pont de commandement", "Archive scellée", "Chantier orbital", "Cargo minéral", "Nœud automatisé")
        }
        val capabilities = listOf(
            setOf(FrontierCapability.EXTRACTION),
            setOf(FrontierCapability.REFINING),
            setOf(FrontierCapability.NETWORK),
            setOf(FrontierCapability.EVENT),
            setOf(FrontierCapability.ARTIFACT),
            setOf(FrontierCapability.CONSTRUCTION),
            setOf(FrontierCapability.EXTRACTION, FrontierCapability.ARTIFACT),
            setOf(FrontierCapability.NETWORK, FrontierCapability.EVENT),
        )
        return names.mapIndexed { index, name ->
            FrontierSectorTemplate(id("sector_${prefix}_${index + 1}"), family, name, capabilities[index], 2 + index)
        }
    }

    private fun modifier(
        id: String,
        name: String,
        families: Set<FrontierVisualFamily>,
        incompatible: Set<GameId>,
        required: Set<FrontierCapability>,
        progress: Long,
        reward: Long,
    ) = FrontierModifierDefinition(GameId.of(id), name, families, incompatible, required, progress, reward)

    private fun objective(id: String, name: String, capability: FrontierCapability, target: Long, reward: FrontierRewardKind) =
        FrontierObjectiveDefinition(GameId.of(id), name, capability, target, reward)

    private fun id(value: String) = GameId.of(value)
    private val ALL = FrontierVisualFamily.entries.toSet()
}
