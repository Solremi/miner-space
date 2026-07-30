package fr.solremi.minerspace.domain.economy

import fr.solremi.minerspace.shared.GameId
import java.math.BigInteger

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

data class DepositState(
    val remainingReserve: Long,
    val pendingCollection: Long,
) {
    init {
        require(remainingReserve >= 0L)
        require(pendingCollection >= 0L)
    }
}

data class EconomyState(
    val inventory: Map<GameId, Long>,
    val deposits: Map<GameId, DepositState>,
    val spaceDollars: Long,
    val transactionSequence: Long,
) {
    init {
        require(inventory.values.none { it < 0L })
        require(spaceDollars >= 0L)
        require(transactionSequence >= 0L)
    }
}

data class EconomyTransaction(
    val sequence: Long,
    val reason: String,
    val resourceDeltas: Map<GameId, Long> = emptyMap(),
    val spaceDollarDelta: Long = 0L,
)

sealed interface EconomyCommandResult {
    val state: EconomyState

    data class Applied(
        override val state: EconomyState,
        val transaction: EconomyTransaction,
    ) : EconomyCommandResult

    data class Rejected(
        override val state: EconomyState,
        val code: String,
    ) : EconomyCommandResult
}

data class ExtractionTickResult(
    val state: EconomyState,
    val extractedByDeposit: Map<GameId, Long>,
)

object FixedPointMath {
    private val scale = BigInteger.valueOf(MULTIPLIER_SCALE)

    fun floorMultiply(value: Long, multiplierMillionths: Long): Long {
        require(value >= 0L)
        require(multiplierMillionths >= 0L)
        return BigInteger.valueOf(value)
            .multiply(BigInteger.valueOf(multiplierMillionths))
            .divide(scale)
            .longValueExact()
    }

    fun addExact(left: Long, right: Long): Long = Math.addExact(left, right)

    fun multiplyExact(left: Long, right: Long): Long = Math.multiplyExact(left, right)
}

class CoreEconomyEngine(
    val definitions: EconomyDefinitions,
) {
    fun initialState(initialSpaceDollars: Long = 0L): EconomyState {
        require(initialSpaceDollars >= 0L)
        return EconomyState(
            inventory = definitions.resources.keys.associateWith { 0L },
            deposits = definitions.deposits.mapValues { (_, definition) ->
                DepositState(
                    remainingReserve = definition.initialReserve,
                    pendingCollection = 0L,
                )
            },
            spaceDollars = initialSpaceDollars,
            transactionSequence = 0L,
        )
    }

    fun advanceExtraction(state: EconomyState, elapsedSeconds: Long): ExtractionTickResult {
        require(elapsedSeconds >= 0L)
        requireValid(state)
        if (elapsedSeconds == 0L) return ExtractionTickResult(state, emptyMap())

        val updatedDeposits = state.deposits.toMutableMap()
        val extracted = linkedMapOf<GameId, Long>()

        definitions.deposits.values
            .sortedBy { it.id.value }
            .forEach { definition ->
                val depositState = updatedDeposits.getValue(definition.id)
                if (depositState.remainingReserve == 0L) return@forEach

                val resource = definitions.resources.getValue(definition.resourceId)
                val stored = state.inventory[resource.id] ?: 0L
                val pendingForResource = definitions.deposits.values
                    .asSequence()
                    .filter { it.resourceId == resource.id }
                    .sumOf { current -> updatedDeposits.getValue(current.id).pendingCollection }
                val storageAvailable = (resource.storageCapacity - stored - pendingForResource)
                    .coerceAtLeast(0L)
                val transportAvailable = (definition.transportCapacity - depositState.pendingCollection)
                    .coerceAtLeast(0L)
                val baseProduction = FixedPointMath.multiplyExact(
                    definition.extractionPerSecond,
                    elapsedSeconds,
                )
                val theoreticalProduction = FixedPointMath.floorMultiply(
                    baseProduction,
                    definition.productionMultiplier,
                ).let { produced ->
                    if (baseProduction > 0L && produced == 0L) 1L else produced
                }
                val quantity = minOf(
                    depositState.remainingReserve,
                    transportAvailable,
                    storageAvailable,
                    theoreticalProduction,
                )
                if (quantity <= 0L) return@forEach

                updatedDeposits[definition.id] = depositState.copy(
                    remainingReserve = depositState.remainingReserve - quantity,
                    pendingCollection = FixedPointMath.addExact(
                        depositState.pendingCollection,
                        quantity,
                    ),
                )
                extracted[definition.id] = quantity
            }

        if (extracted.isEmpty()) return ExtractionTickResult(state, emptyMap())
        return ExtractionTickResult(
            state = state.copy(
                deposits = updatedDeposits,
                transactionSequence = FixedPointMath.addExact(state.transactionSequence, 1L),
            ),
            extractedByDeposit = extracted,
        )
    }

    fun collect(state: EconomyState, depositId: GameId): EconomyCommandResult {
        requireValid(state)
        val definition = definitions.deposits[depositId]
            ?: return EconomyCommandResult.Rejected(state, "unknown_deposit")
        val depositState = state.deposits.getValue(depositId)
        val quantity = depositState.pendingCollection
        if (quantity <= 0L) return EconomyCommandResult.Rejected(state, "nothing_to_collect")

        val resource = definitions.resources.getValue(definition.resourceId)
        val stored = state.inventory[resource.id] ?: 0L
        if (resource.storageCapacity - stored < quantity) {
            return EconomyCommandResult.Rejected(state, "storage_full")
        }

        val sequence = FixedPointMath.addExact(state.transactionSequence, 1L)
        val inventory = state.inventory.toMutableMap().apply {
            this[resource.id] = FixedPointMath.addExact(stored, quantity)
        }
        val deposits = state.deposits.toMutableMap().apply {
            this[depositId] = depositState.copy(pendingCollection = 0L)
        }
        val next = state.copy(
            inventory = inventory,
            deposits = deposits,
            transactionSequence = sequence,
        )
        requireValid(next)
        return EconomyCommandResult.Applied(
            state = next,
            transaction = EconomyTransaction(
                sequence = sequence,
                reason = "collect",
                resourceDeltas = mapOf(resource.id to quantity),
            ),
        )
    }

    fun sell(state: EconomyState, resourceId: GameId, quantity: Long): EconomyCommandResult {
        requireValid(state)
        if (quantity <= 0L) return EconomyCommandResult.Rejected(state, "invalid_quantity")
        val resource = definitions.resources[resourceId]
            ?: return EconomyCommandResult.Rejected(state, "unknown_resource")
        if (!resource.sellable) return EconomyCommandResult.Rejected(state, "resource_not_sellable")
        val stored = state.inventory[resourceId] ?: 0L
        if (stored < quantity) return EconomyCommandResult.Rejected(state, "insufficient_stock")

        val revenue = FixedPointMath.multiplyExact(quantity, resource.unitSalePrice)
        val sequence = FixedPointMath.addExact(state.transactionSequence, 1L)
        val inventory = state.inventory.toMutableMap().apply {
            this[resourceId] = stored - quantity
        }
        val next = state.copy(
            inventory = inventory,
            spaceDollars = FixedPointMath.addExact(state.spaceDollars, revenue),
            transactionSequence = sequence,
        )
        requireValid(next)
        return EconomyCommandResult.Applied(
            state = next,
            transaction = EconomyTransaction(
                sequence = sequence,
                reason = "sell",
                resourceDeltas = mapOf(resourceId to -quantity),
                spaceDollarDelta = revenue,
            ),
        )
    }

    fun sellAllSellable(state: EconomyState): EconomyCommandResult {
        requireValid(state)
        val deltas = linkedMapOf<GameId, Long>()
        var revenue = 0L
        val inventory = state.inventory.toMutableMap()

        definitions.resources.values
            .sortedBy { it.id.value }
            .filter { it.sellable }
            .forEach { resource ->
                val quantity = inventory[resource.id] ?: 0L
                if (quantity > 0L) {
                    revenue = FixedPointMath.addExact(
                        revenue,
                        FixedPointMath.multiplyExact(quantity, resource.unitSalePrice),
                    )
                    inventory[resource.id] = 0L
                    deltas[resource.id] = -quantity
                }
            }

        if (deltas.isEmpty()) return EconomyCommandResult.Rejected(state, "nothing_to_sell")
        val sequence = FixedPointMath.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            inventory = inventory,
            spaceDollars = FixedPointMath.addExact(state.spaceDollars, revenue),
            transactionSequence = sequence,
        )
        requireValid(next)
        return EconomyCommandResult.Applied(
            state = next,
            transaction = EconomyTransaction(
                sequence = sequence,
                reason = "sell_all",
                resourceDeltas = deltas,
                spaceDollarDelta = revenue,
            ),
        )
    }

    fun validationErrors(state: EconomyState): List<String> {
        val errors = mutableListOf<String>()
        if (state.spaceDollars < 0L) errors += "negative_space_dollars"
        if (state.transactionSequence < 0L) errors += "negative_transaction_sequence"
        definitions.resources.values.forEach { resource ->
            val quantity = state.inventory[resource.id] ?: 0L
            if (quantity < 0L) errors += "negative_inventory:${resource.id}"
            if (quantity > resource.storageCapacity) errors += "storage_overflow:${resource.id}"
        }
        definitions.deposits.values.forEach { definition ->
            val deposit = state.deposits[definition.id]
            if (deposit == null) {
                errors += "missing_deposit:${definition.id}"
            } else {
                if (deposit.remainingReserve < 0L) errors += "negative_reserve:${definition.id}"
                if (deposit.pendingCollection < 0L) errors += "negative_pending:${definition.id}"
                if (deposit.pendingCollection > definition.transportCapacity) {
                    errors += "transport_overflow:${definition.id}"
                }
            }
        }
        return errors
    }

    fun requireValid(state: EconomyState) {
        val errors = validationErrors(state)
        require(errors.isEmpty()) { errors.joinToString() }
    }
}
