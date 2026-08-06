package fr.solremi.minerspace.game.screen

import fr.solremi.minerspace.domain.strategy.SpecializationId
import fr.solremi.minerspace.shared.GameId

object StrategyLabText {
    fun specializationName(id: SpecializationId): String = when (id) {
        SpecializationId.INDUSTRIAL -> "INDUSTRIE"
        SpecializationId.LOGISTICS -> "LOGISTIQUE"
        SpecializationId.RESEARCH -> "RECHERCHE"
        SpecializationId.PROSPECTOR -> "PROSPECTION"
    }

    fun moduleName(id: GameId): String = when (id.value) {
        "module_forge_drill" -> "Foreuse"
        "module_forge_thermal" -> "Thermique"
        "module_forge_chassis" -> "Châssis"
        "module_survey_optics" -> "Optique"
        "module_survey_quantum" -> "Quantique"
        "module_survey_archive" -> "Archive"
        "module_storage_capsule" -> "Stockage"
        else -> "Batterie"
    }

    fun shortResource(id: GameId): String = id.value
        .removePrefix("refined_")
        .removePrefix("component_")
        .replace('_', ' ')

    fun percent(value: Long): String = "%.0f%%".format(value / 10_000.0)

    fun rejection(code: String): String = when (code) {
        "specialization_cooldown" -> "Changement disponible après le délai"
        "insufficient_space_dollars" -> "SpaceDollars insuffisants"
        "missing_module_materials" -> "Matériaux manquants"
        "module_slots_full" -> "Emplacements pleins"
        "module_max_level" -> "Niveau maximal"
        else -> code
    }

    fun success(reason: String): String = when (reason) {
        "change_specialization" -> "Spécialisation appliquée"
        "craft_module" -> "Module fabriqué"
        "equip_module" -> "Module équipé"
        "unequip_module" -> "Module retiré"
        "upgrade_module" -> "Module amélioré"
        "dismantle_module" -> "Module démonté · 70 % restitués"
        else -> reason
    }
}
