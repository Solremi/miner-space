package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.prestige.PendingPlanetTransfer
import fr.solremi.minerspace.domain.prestige.PlanetId
import fr.solremi.minerspace.domain.prestige.PrestigeState
import fr.solremi.minerspace.domain.prestige.VeteranRobotSnapshot
import fr.solremi.minerspace.domain.robot.RobotStatistics
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId

class PrestigeStateCodec {
    fun encode(
        state: PrestigeState,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        require(savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("activePlanet=${state.activePlanet.name}")
            appendLine("stellarCores=${state.stellarCores}")
            appendLine("completedTransfers=${state.completedTransfers}")
            appendLine("codex=${KeyValueSaveCodecSupport.encodeIds(state.permanentCodexEntryIds)}")
            appendLine("archives=${KeyValueSaveCodecSupport.encodeIds(state.permanentArchiveIds)}")
            appendLine("bonuses=${KeyValueSaveCodecSupport.encodeIds(state.permanentBonusIds)}")
            appendLine("veteran=${state.veteranRobot?.let(::encodeVeteran).orEmpty()}")
            appendLine("pending=${state.pendingTransfer?.let(::encodePending).orEmpty()}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(
            slotId = slotId,
            schemaVersion = FORMAT_VERSION,
            contentVersion = CONTENT_VERSION,
            bytes = text.toByteArray(Charsets.UTF_8),
            savedAtEpochMillis = savedAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): PrestigeState {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = KeyValueSaveCodecSupport.fields(payload)
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        return PrestigeState(
            activePlanet = PlanetId.valueOf(fields.getValue("activePlanet")),
            stellarCores = fields.getValue("stellarCores").toLong(),
            completedTransfers = fields.getValue("completedTransfers").toInt(),
            permanentCodexEntryIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("codex")),
            permanentArchiveIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("archives")),
            permanentBonusIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("bonuses")),
            veteranRobot = fields.getValue("veteran").takeIf(String::isNotBlank)?.let(::decodeVeteran),
            pendingTransfer = fields.getValue("pending").takeIf(String::isNotBlank)?.let(::decodePending),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodePending(value: PendingPlanetTransfer): String = listOf(
        KeyValueSaveCodecSupport.encodeText(value.transferId),
        value.sourcePlanet.name,
        value.destinationPlanet.name,
        value.expectedStellarCores,
        KeyValueSaveCodecSupport.encodeIds(value.preservedCodexEntryIds),
        KeyValueSaveCodecSupport.encodeIds(value.preservedArchiveIds),
        KeyValueSaveCodecSupport.encodeIds(value.preservedBonusIds),
        encodeVeteran(value.veteranRobot),
        value.preparedAtEpochMillis,
    ).joinToString("~")

    private fun decodePending(value: String): PendingPlanetTransfer {
        val parts = value.split('~')
        require(parts.size == 9)
        return PendingPlanetTransfer(
            transferId = KeyValueSaveCodecSupport.decodeText(parts[0]),
            sourcePlanet = PlanetId.valueOf(parts[1]),
            destinationPlanet = PlanetId.valueOf(parts[2]),
            expectedStellarCores = parts[3].toLong(),
            preservedCodexEntryIds = KeyValueSaveCodecSupport.decodeIds(parts[4]),
            preservedArchiveIds = KeyValueSaveCodecSupport.decodeIds(parts[5]),
            preservedBonusIds = KeyValueSaveCodecSupport.decodeIds(parts[6]),
            veteranRobot = decodeVeteran(parts[7]),
            preparedAtEpochMillis = parts[8].toLong(),
        )
    }

    private fun encodeVeteran(value: VeteranRobotSnapshot): String {
        val stats = value.statistics
        return listOf(
            value.id.value,
            KeyValueSaveCodecSupport.encodeText(value.displayName),
            KeyValueSaveCodecSupport.encodeText(value.serialNumber),
            value.level,
            value.trait.name,
            value.masteryPoints,
            stats.extracted,
            stats.refined,
            stats.assembled,
            stats.transported,
            stats.activeSeconds,
        ).joinToString("|")
    }

    private fun decodeVeteran(value: String): VeteranRobotSnapshot {
        val parts = value.split('|')
        require(parts.size == 11)
        return VeteranRobotSnapshot(
            id = GameId.of(parts[0]),
            displayName = KeyValueSaveCodecSupport.decodeText(parts[1]),
            serialNumber = KeyValueSaveCodecSupport.decodeText(parts[2]),
            level = parts[3].toInt(),
            trait = RobotTrait.valueOf(parts[4]),
            masteryPoints = parts[5].toLong(),
            statistics = RobotStatistics(
                extracted = parts[6].toLong(),
                refined = parts[7].toLong(),
                assembled = parts[8].toLong(),
                transported = parts[9].toLong(),
                activeSeconds = parts[10].toLong(),
            ),
        )
    }

    companion object {
        const val SLOT_ID = "prestige"
        const val FORMAT_VERSION = 1
        const val CONTENT_VERSION = "1.0.0"
    }
}
