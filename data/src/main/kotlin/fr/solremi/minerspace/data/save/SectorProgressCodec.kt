package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.exploration.ExplorationState
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId

class SectorProgressCodec {
    fun encode(
        state: ExplorationState,
        contentVersion: String,
        savedAtEpochMillis: Long = System.currentTimeMillis().coerceAtLeast(0L),
    ): SavePayload {
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("contentVersion=$contentVersion")
            appendLine("revealed=${encodeIds(state.revealedSectorIds)}")
            appendLine("unlocked=${encodeIds(state.unlockedSectorIds)}")
            appendLine("rareDeposits=${encodeIds(state.discoveredRareDepositIds)}")
            appendLine("spentSpaceDollars=${state.spentSpaceDollars}")
            appendLine("spentComponents=${encodeMap(state.spentComponents)}")
            appendLine("activeMission=${state.activeMissionSectorId?.value.orEmpty()}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(
            slotId = SLOT_ID,
            schemaVersion = FORMAT_VERSION,
            contentVersion = contentVersion,
            bytes = text.toByteArray(Charsets.UTF_8),
            savedAtEpochMillis = savedAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): ExplorationState {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = payload.bytes.toString(Charsets.UTF_8).lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0)
                line.substring(0, separator) to line.substring(separator + 1)
            }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        require(fields.getValue("contentVersion") == payload.contentVersion)
        return ExplorationState(
            revealedSectorIds = decodeIds(fields.getValue("revealed")),
            unlockedSectorIds = decodeIds(fields.getValue("unlocked")),
            discoveredRareDepositIds = decodeIds(fields.getValue("rareDeposits")),
            spentSpaceDollars = fields.getValue("spentSpaceDollars").toLong(),
            spentComponents = decodeMap(fields.getValue("spentComponents")),
            activeMissionSectorId = fields.getValue("activeMission").takeIf { it.isNotBlank() }?.let(GameId::of),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeIds(values: Set<GameId>): String = values.sortedBy { it.value }.joinToString(",") { it.value }
    private fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)
    private fun encodeMap(values: Map<GameId, Long>): String = values.entries.sortedBy { it.key.value }.joinToString(",") { "${it.key.value}:${it.value}" }
    private fun decodeMap(value: String): Map<GameId, Long> = if (value.isBlank()) emptyMap() else value.split(',').associate { entry ->
        val parts = entry.split(':'); require(parts.size == 2); GameId.of(parts[0]) to parts[1].toLong()
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val SLOT_ID = "exploration"
    }
}
