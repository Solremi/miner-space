package fr.solremi.minerspace.domain.exploration

import fr.solremi.minerspace.shared.GameId

data class SectorBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(x >= 0 && y >= 0)
        require(width > 0 && height > 0)
    }

    val centerX: Int get() = x + width / 2
    val centerY: Int get() = y + height / 2
}

data class SectorDefinition(
    val id: GameId,
    val nameKey: String,
    val strategicReason: String,
    val bounds: SectorBounds,
    val unlockCostSpaceDollars: Long,
    val scannerLevelRequired: Int,
    val requiredSectorIds: Set<GameId>,
    val requiredTechnologyIds: Set<GameId>,
    val requiredComponents: Map<GameId, Long>,
    val rareDepositId: GameId?,
    val missionTarget: Boolean,
    val initiallyUnlocked: Boolean,
) {
    init {
        require(nameKey.isNotBlank())
        require(strategicReason.isNotBlank())
        require(unlockCostSpaceDollars >= 0L)
        require(scannerLevelRequired >= 1)
        require(requiredComponents.values.all { it > 0L })
        require(id !in requiredSectorIds)
        if (initiallyUnlocked) {
            require(unlockCostSpaceDollars == 0L)
            require(requiredSectorIds.isEmpty())
            require(requiredTechnologyIds.isEmpty())
            require(requiredComponents.isEmpty())
        }
    }
}

data class ExplorationDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val sectors: Map<GameId, SectorDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(sectors.isNotEmpty())
        require(sectors.values.any { it.initiallyUnlocked })
        sectors.values.forEach { sector ->
            sector.requiredSectorIds.forEach { require(sectors.containsKey(it)) }
        }
        require(noSectorDependencyCycle()) { "Sector dependency cycle" }
    }

    private fun noSectorDependencyCycle(): Boolean {
        val visiting = mutableSetOf<GameId>()
        val visited = mutableSetOf<GameId>()
        fun visit(id: GameId): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            val valid = sectors.getValue(id).requiredSectorIds.all(::visit)
            visiting.remove(id)
            visited += id
            return valid
        }
        return sectors.keys.all(::visit)
    }
}
