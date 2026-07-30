package fr.solremi.minerspace.domain.economy

import fr.solremi.minerspace.shared.GameId
import java.math.BigInteger

data class ProductionMultipliers(
    val robot: Long = MULTIPLIER_SCALE,
    val modules: Long = MULTIPLIER_SCALE,
    val synergies: Long = MULTIPLIER_SCALE,
    val specialization: Long = MULTIPLIER_SCALE,
    val technologies: Long = MULTIPLIER_SCALE,
    val planet: Long = MULTIPLIER_SCALE,
    val event: Long = MULTIPLIER_SCALE,
    val prestige: Long = MULTIPLIER_SCALE,
) {
    init {
        ordered().forEach { require(it >= 0L) }
    }

    fun ordered(): List<Long> = listOf(
        robot,
        modules,
        synergies,
        specialization,
        technologies,
        planet,
        event,
        prestige,
    )

    companion object {
        val IDENTITY = ProductionMultipliers()
    }
}

object ProductionFormula {
    private val scale = BigInteger.valueOf(MULTIPLIER_SCALE)

    fun floor(base: Long, multipliers: ProductionMultipliers): Long =
        floor(base, multipliers.ordered())

    fun floor(base: Long, orderedMultipliers: List<Long>): Long {
        require(base >= 0L)
        orderedMultipliers.forEach { require(it >= 0L) }
        if (base == 0L) return 0L
        var numerator = BigInteger.valueOf(base)
        orderedMultipliers.forEach { multiplier ->
            numerator = numerator.multiply(BigInteger.valueOf(multiplier))
        }
        return numerator
            .divide(scale.pow(orderedMultipliers.size))
            .longValueExact()
    }
}

/**
 * Extraction avec l'ordre officiel : base, robot, modules, synergies,
 * spécialisation, technologies, planète, événement puis prestige.
 * Un seul arrondi inférieur est réalisé à la fin du calcul.
 */
fun CoreEconomyEngine.advanceExtraction(
    state: EconomyState,
    elapsedSeconds: Long,
    multipliers: ProductionMultipliers,
): ExtractionTickResult {
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
            val theoreticalProduction = ProductionFormula.floor(
                baseProduction,
                listOf(definition.productionMultiplier) + multipliers.ordered(),
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
