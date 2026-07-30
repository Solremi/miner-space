package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.assembly.AssemblyJob
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.DepositState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningJob
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId

class ManufacturingSnapshotCodec {
    fun encode(
        state: ManufacturingGameState,
        contentVersion: String,
        slotId: String = DEFAULT_SLOT,
    ): SavePayload {
        require(contentVersion.isNotBlank())
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("contentVersion=$contentVersion")
            appendLine("spaceDollars=${state.economy.spaceDollars}")
            appendLine("transactionSequence=${state.economy.transactionSequence}")
            appendLine("inventory=${encodeMap(state.economy.inventory)}")
            appendLine("deposits=${encodeDeposits(state.economy.deposits)}")
            appendLine("refiningNextJobSequence=${state.refining.nextJobSequence}")
            appendLine("refiningRefundBuffer=${encodeMap(state.refining.refundBuffer)}")
            appendLine("refiningJobs=${encodeRefiningJobs(state.refining.jobs)}")
            appendLine("assemblyNextJobSequence=${state.assembly.nextJobSequence}")
            appendLine("installedTechnologies=${encodeIds(state.assembly.installedTechnologyIds)}")
            appendLine("assemblyJobs=${encodeAssemblyJobs(state.assembly.jobs)}")
        }
        return SavePayload(
            slotId = slotId,
            schemaVersion = FORMAT_VERSION,
            contentVersion = contentVersion,
            bytes = text.toByteArray(Charsets.UTF_8),
        )
    }

    fun decode(payload: SavePayload): ManufacturingGameState {
        require(payload.schemaVersion == FORMAT_VERSION) { "Unsupported snapshot schema" }
        val fields = payload.bytes.toString(Charsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Invalid snapshot line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        require(fields.getValue("contentVersion") == payload.contentVersion)
        return ManufacturingGameState(
            economy = EconomyState(
                inventory = decodeMap(fields.getValue("inventory")),
                deposits = decodeDeposits(fields.getValue("deposits")),
                spaceDollars = fields.getValue("spaceDollars").toLong(),
                transactionSequence = fields.getValue("transactionSequence").toLong(),
            ),
            refining = RefiningState(
                jobs = decodeRefiningJobs(fields.getValue("refiningJobs")),
                refundBuffer = decodeMap(fields.getValue("refiningRefundBuffer")),
                nextJobSequence = fields.getValue("refiningNextJobSequence").toLong(),
            ),
            assembly = AssemblyState(
                jobs = decodeAssemblyJobs(fields.getValue("assemblyJobs")),
                installedTechnologyIds = decodeIds(fields.getValue("installedTechnologies")),
                nextJobSequence = fields.getValue("assemblyNextJobSequence").toLong(),
            ),
        )
    }

    private fun encodeMap(values: Map<GameId, Long>): String = values.entries
        .sortedBy { it.key.value }
        .joinToString(",") { (id, quantity) -> "${id.value}:$quantity" }

    private fun decodeMap(value: String): Map<GameId, Long> = if (value.isBlank()) {
        emptyMap()
    } else {
        value.split(',').associate { entry ->
            val parts = entry.split(':')
            require(parts.size == 2) { "Invalid quantity entry" }
            GameId.of(parts[0]) to parts[1].toLong()
        }
    }

    private fun encodeIds(values: Set<GameId>): String = values
        .sortedBy { it.value }
        .joinToString(",") { it.value }

    private fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) {
        emptySet()
    } else {
        value.split(',').mapTo(linkedSetOf(), GameId::of)
    }

    private fun encodeDeposits(values: Map<GameId, DepositState>): String = values.entries
        .sortedBy { it.key.value }
        .joinToString(",") { (id, state) ->
            "${id.value}:${state.remainingReserve}:${state.pendingCollection}"
        }

    private fun decodeDeposits(value: String): Map<GameId, DepositState> = if (value.isBlank()) {
        emptyMap()
    } else {
        value.split(',').associate { entry ->
            val parts = entry.split(':')
            require(parts.size == 3) { "Invalid deposit entry" }
            GameId.of(parts[0]) to DepositState(parts[1].toLong(), parts[2].toLong())
        }
    }

    private fun encodeRefiningJobs(jobs: List<RefiningJob>): String = jobs.joinToString(";") { job ->
        listOf(
            job.id,
            job.recipeId.value,
            job.queuedAtEpochMillis,
            job.startsAtEpochMillis,
            job.finishesAtEpochMillis,
            job.status.name,
            job.outputResourceId.value,
            job.outputQuantity,
            encodeMap(job.reservedInputs),
        ).joinToString("|")
    }

    private fun decodeRefiningJobs(value: String): List<RefiningJob> = if (value.isBlank()) {
        emptyList()
    } else {
        value.split(';').map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 9) { "Invalid refining job" }
            RefiningJob(
                id = parts[0],
                recipeId = GameId.of(parts[1]),
                queuedAtEpochMillis = parts[2].toLong(),
                startsAtEpochMillis = parts[3].toLong(),
                finishesAtEpochMillis = parts[4].toLong(),
                status = RefiningJobStatus.valueOf(parts[5]),
                outputResourceId = GameId.of(parts[6]),
                outputQuantity = parts[7].toLong(),
                reservedInputs = decodeMap(parts[8]),
            )
        }
    }

    private fun encodeAssemblyJobs(jobs: List<AssemblyJob>): String = jobs.joinToString(";") { job ->
        listOf(
            job.id,
            job.recipeId.value,
            job.queuedAtEpochMillis,
            job.startsAtEpochMillis,
            job.finishesAtEpochMillis,
            job.status.name,
            job.outputResourceId.value,
            job.outputQuantity,
            job.outputKind.name,
            encodeMap(job.reservedInputs),
        ).joinToString("|")
    }

    private fun decodeAssemblyJobs(value: String): List<AssemblyJob> = if (value.isBlank()) {
        emptyList()
    } else {
        value.split(';').map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 10) { "Invalid assembly job" }
            AssemblyJob(
                id = parts[0],
                recipeId = GameId.of(parts[1]),
                queuedAtEpochMillis = parts[2].toLong(),
                startsAtEpochMillis = parts[3].toLong(),
                finishesAtEpochMillis = parts[4].toLong(),
                status = AssemblyJobStatus.valueOf(parts[5]),
                outputResourceId = GameId.of(parts[6]),
                outputQuantity = parts[7].toLong(),
                outputKind = AssemblyOutputKind.valueOf(parts[8]),
                reservedInputs = decodeMap(parts[9]),
            )
        }
    }

    companion object {
        const val FORMAT_VERSION = 2
        const val DEFAULT_SLOT = "primary"
    }
}
