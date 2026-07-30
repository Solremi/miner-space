package fr.solremi.minerspace.simulation.economy

import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyCommandResult
import fr.solremi.minerspace.domain.economy.EconomyState

data class EconomySimulationReport(
    val finalState: EconomyState,
    val simulatedSeconds: Long,
    val steps: Long,
    val collectedTransactions: Long,
    val saleTransactions: Long,
    val validationErrors: List<String>,
)

class CoreEconomySimulator(
    private val engine: CoreEconomyEngine,
) {
    fun simulate(
        initialState: EconomyState,
        durationSeconds: Long,
        stepSeconds: Long = 60L,
        autoCollect: Boolean = true,
        autoSell: Boolean = true,
    ): EconomySimulationReport {
        require(durationSeconds >= 0L)
        require(stepSeconds > 0L)

        var state = initialState
        var elapsed = 0L
        var steps = 0L
        var collections = 0L
        var sales = 0L
        val errors = mutableListOf<String>()

        while (elapsed < durationSeconds) {
            val step = minOf(stepSeconds, durationSeconds - elapsed)
            state = engine.advanceExtraction(state, step).state

            if (autoCollect) {
                engine.definitions.deposits.keys
                    .sortedBy { it.value }
                    .forEach { depositId ->
                        when (val result = engine.collect(state, depositId)) {
                            is EconomyCommandResult.Applied -> {
                                state = result.state
                                collections++
                            }
                            is EconomyCommandResult.Rejected -> Unit
                        }
                    }
            }

            if (autoSell) {
                when (val result = engine.sellAllSellable(state)) {
                    is EconomyCommandResult.Applied -> {
                        state = result.state
                        sales++
                    }
                    is EconomyCommandResult.Rejected -> Unit
                }
            }

            errors += engine.validationErrors(state)
            elapsed += step
            steps++
        }

        return EconomySimulationReport(
            finalState = state,
            simulatedSeconds = elapsed,
            steps = steps,
            collectedTransactions = collections,
            saleTransactions = sales,
            validationErrors = errors.distinct(),
        )
    }
}
