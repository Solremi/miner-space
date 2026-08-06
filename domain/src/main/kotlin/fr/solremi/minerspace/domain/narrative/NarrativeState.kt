package fr.solremi.minerspace.domain.narrative

import fr.solremi.minerspace.shared.GameId

data class NarrativeSnapshot(
    val unlockedSectorCount: Int,
    val installedTechnologyCount: Int,
    val inventory: Map<GameId, Long>,
    val robotMasteryById: Map<GameId, Long>,
) {
    init {
        require(unlockedSectorCount >= 1 && installedTechnologyCount >= 0)
        require(inventory.values.none { it < 0L })
        require(robotMasteryById.values.none { it < 0L })
    }
}

data class PendingNarrativeGrant(
    val grantId: String,
    val chapterId: GameId,
    val rareResourceId: GameId?,
    val expectedRareTotal: Long,
    val veteranRobotId: GameId?,
    val expectedVeteranMastery: Long,
) {
    init {
        require(grantId.isNotBlank())
        require(expectedRareTotal >= 0L && expectedVeteranMastery >= 0L)
        if (rareResourceId == null) require(expectedRareTotal == 0L)
        if (veteranRobotId == null) require(expectedVeteranMastery == 0L)
    }
}

data class NarrativeState(
    val readTransmissionIds: Set<GameId>,
    val resolvedChapterIds: Set<GameId>,
    val anomalyAttempts: Map<GameId, Int>,
    val discoveredRareResourceIds: Set<GameId>,
    val veteranRobotId: GameId?,
    val selectedChapterId: GameId?,
    val pendingGrant: PendingNarrativeGrant?,
    val transactionSequence: Long,
) {
    init {
        require(anomalyAttempts.values.none { it < 0 })
        require(transactionSequence >= 0L)
    }
}

data class NarrativeChapterView(
    val definition: NarrativeChapterDefinition,
    val available: Boolean,
    val read: Boolean,
    val resolved: Boolean,
    val attempts: Int,
)
