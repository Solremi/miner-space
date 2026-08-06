package fr.solremi.minerspace.domain.narrative

import fr.solremi.minerspace.shared.GameId

enum class NarrativeChapterKind { CONTACT, RUINS, ANOMALY, LEGACY }

data class NarrativeChapterDefinition(
    val id: GameId,
    val kind: NarrativeChapterKind,
    val title: String,
    val transmission: String,
    val archiveSummary: String,
    val requiredUnlockedSectors: Int,
    val requiredTechnologies: Int,
    val requiredResolvedChapterIds: Set<GameId>,
    val anomalyChancePercent: Int,
    val pityAttempts: Int,
    val deterministicSeed: Int,
    val rareResourceId: GameId?,
    val grantsVeteranRobot: Boolean,
) {
    init {
        require(title.isNotBlank() && transmission.isNotBlank() && archiveSummary.isNotBlank())
        require(requiredUnlockedSectors >= 1 && requiredTechnologies >= 0)
        require(anomalyChancePercent in 0..100)
        require(pityAttempts in 1..20)
        require(id !in requiredResolvedChapterIds)
        if (grantsVeteranRobot) require(rareResourceId != null)
    }
}

data class NarrativeDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val veteranMasteryPoints: Long,
    val veteranRobotId: GameId,
    val chapters: Map<GameId, NarrativeChapterDefinition>,
) {
    init {
        require(schemaVersion > 0 && contentVersion.isNotBlank())
        require(veteranMasteryPoints > 0L)
        require(chapters.isNotEmpty())
        chapters.values.forEach { chapter ->
            require(chapter.requiredResolvedChapterIds.all(chapters::containsKey))
        }
        require(noDependencyCycle())
    }

    private fun noDependencyCycle(): Boolean {
        val visiting = mutableSetOf<GameId>()
        val visited = mutableSetOf<GameId>()
        fun visit(id: GameId): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            val valid = chapters.getValue(id).requiredResolvedChapterIds.all(::visit)
            visiting.remove(id)
            visited += id
            return valid
        }
        return chapters.keys.all(::visit)
    }
}
