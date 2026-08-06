package fr.solremi.minerspace.domain.economy

import fr.solremi.minerspace.shared.GameId

const val MULTIPLIER_SCALE: Long = 1_000_000L

data class ResourceDefinition(
    val id: GameId,
    val nameKey: String,
    val unitSalePrice: Long,
    val storageCapacity: Long,
    val sellable: Boolean,
) {
    init {
        require(nameKey.isNotBlank())
        require(unitSalePrice >= 0L)
        require(storageCapacity > 0L)
    }
}

data class DepositDefinition(
    val id: GameId,
    val resourceId: GameId,
    val initialReserve: Long,
    val extractionPerSecond: Long,
    val transportCapacity: Long,
    val productionMultiplier: Long = MULTIPLIER_SCALE,
) {
    init {
        require(initialReserve >= 0L)
        require(extractionPerSecond > 0L)
        require(transportCapacity > 0L)
        require(productionMultiplier > 0L)
    }
}

data class EconomyDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val resources: Map<GameId, ResourceDefinition>,
    val deposits: Map<GameId, DepositDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(resources.isNotEmpty())
        require(deposits.isNotEmpty())
        deposits.values.forEach { deposit ->
            require(resources.containsKey(deposit.resourceId)) {
                "Deposit ${deposit.id} references unknown resource ${deposit.resourceId}."
            }
        }
    }
}
