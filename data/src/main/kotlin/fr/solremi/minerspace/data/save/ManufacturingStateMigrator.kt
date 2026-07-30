package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.shared.GameId

data class StateMigrationResult(
    val state: ManufacturingGameState,
    val changed: Boolean,
)

class ManufacturingStateMigrator(
    private val economyDefinitions: EconomyDefinitions,
    private val refiningDefinitions: RefiningDefinitions,
    private val assemblyDefinitions: AssemblyDefinitions,
) {
    fun migrate(source: ManufacturingGameState): StateMigrationResult {
        val inventory = economyDefinitions.resources.keys.associateWith { id ->
            (source.economy.inventory[id] ?: 0L)
                .coerceIn(0L, economyDefinitions.resources.getValue(id).storageCapacity)
        }
        val deposits = economyDefinitions.deposits.mapValues { (id, definition) ->
            val previous = source.economy.deposits[id]
            if (previous == null) {
                fr.solremi.minerspace.domain.economy.DepositState(definition.initialReserve, 0L)
            } else {
                previous.copy(
                    remainingReserve = previous.remainingReserve.coerceIn(0L, definition.initialReserve),
                    pendingCollection = previous.pendingCollection.coerceIn(0L, definition.transportCapacity),
                )
            }
        }

        val validResources = economyDefinitions.resources.keys
        val refiningJobs = source.refining.jobs
            .asSequence()
            .filter { refiningDefinitions.recipes.containsKey(it.recipeId) }
            .filter { it.outputResourceId in validResources }
            .filter { it.reservedInputs.keys.all(validResources::contains) }
            .distinctBy { it.id }
            .take(refiningDefinitions.robot.queueCapacity)
            .toList()
        val refundBuffer = source.refining.refundBuffer
            .filterKeys(validResources::contains)
            .mapValues { (_, quantity) -> quantity.coerceAtLeast(0L) }
            .filterValues { it > 0L }

        val installed = normalizeInstalledTechnologies(source.assembly.installedTechnologyIds)
        val assemblyJobs = source.assembly.jobs
            .asSequence()
            .filter { assemblyDefinitions.recipes.containsKey(it.recipeId) }
            .filter { it.outputResourceId in validResources }
            .filter { it.reservedInputs.keys.all(validResources::contains) }
            .distinctBy { it.id }
            .take(assemblyDefinitions.robot.queueCapacity)
            .toList()

        val migrated = source.copy(
            economy = source.economy.copy(
                inventory = inventory,
                deposits = deposits,
                spaceDollars = source.economy.spaceDollars.coerceAtLeast(0L),
                transactionSequence = source.economy.transactionSequence.coerceAtLeast(0L),
            ),
            refining = source.refining.copy(
                jobs = refiningJobs,
                refundBuffer = refundBuffer,
                nextJobSequence = source.refining.nextJobSequence.coerceAtLeast(1L),
            ),
            assembly = source.assembly.copy(
                jobs = assemblyJobs,
                installedTechnologyIds = installed,
                nextJobSequence = source.assembly.nextJobSequence.coerceAtLeast(1L),
            ),
        )
        return StateMigrationResult(migrated, migrated != source)
    }

    private fun normalizeInstalledTechnologies(source: Set<GameId>): Set<GameId> {
        val remaining = source.filterTo(linkedSetOf()) { assemblyDefinitions.technologies.containsKey(it) }
        val accepted = linkedSetOf<GameId>()
        var progressed: Boolean
        do {
            progressed = false
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val id = iterator.next()
                val definition = assemblyDefinitions.technologies.getValue(id)
                if (accepted.containsAll(definition.requiredTechnologyIds)) {
                    accepted += id
                    iterator.remove()
                    progressed = true
                }
            }
        } while (progressed)
        return accepted
    }
}
