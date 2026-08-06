package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.cryos.CryosIxState
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId

class CryosIxStateCodec {
    fun encode(
        state: CryosIxState,
        contentVersion: String,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        require(contentVersion.isNotBlank() && savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("baseInstalled=${state.baseInstalled}")
            appendLine("energy=${state.energy}")
            appendLine("heat=${state.heat}")
            appendLine("coldExposure=${state.coldExposure}")
            appendLine("thermalNodes=${state.thermalNodes}")
            appendLine("inventory=${KeyValueSaveCodecSupport.encodeLongMap(state.inventory)}")
            appendLine("technologies=${KeyValueSaveCodecSupport.encodeIds(state.installedTechnologyIds)}")
            appendLine("modules=${KeyValueSaveCodecSupport.encodeIds(state.craftedModuleIds)}")
            appendLine("sectors=${KeyValueSaveCodecSupport.encodeIds(state.unlockedSectorIds)}")
            appendLine("mainMissions=${KeyValueSaveCodecSupport.encodeIds(state.completedMainMissionIds)}")
            appendLine("secondaryMissions=${KeyValueSaveCodecSupport.encodeIds(state.completedSecondaryMissionIds)}")
            appendLine("events=${KeyValueSaveCodecSupport.encodeIds(state.resolvedEventIds)}")
            appendLine("narrative=${KeyValueSaveCodecSupport.encodeIds(state.narrativeDiscoveryIds)}")
            appendLine("codex=${KeyValueSaveCodecSupport.encodeIds(state.discoveredCodexEntryIds)}")
            appendLine("frontierUnlocked=${state.frontierUnlocked}")
            appendLine("veteranRobotId=${state.veteranRobotId?.value.orEmpty()}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(slotId, FORMAT_VERSION, contentVersion, text.toByteArray(), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): CryosIxState {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = KeyValueSaveCodecSupport.fields(payload)
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        return CryosIxState(
            baseInstalled = fields.getValue("baseInstalled").toBooleanStrict(),
            energy = fields.getValue("energy").toLong(),
            heat = fields.getValue("heat").toLong(),
            coldExposure = fields.getValue("coldExposure").toLong(),
            thermalNodes = fields.getValue("thermalNodes").toInt(),
            inventory = KeyValueSaveCodecSupport.decodeLongMap(fields.getValue("inventory")),
            installedTechnologyIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("technologies")),
            craftedModuleIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("modules")),
            unlockedSectorIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("sectors")),
            completedMainMissionIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("mainMissions")),
            completedSecondaryMissionIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("secondaryMissions")),
            resolvedEventIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("events")),
            narrativeDiscoveryIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("narrative")),
            discoveredCodexEntryIds = KeyValueSaveCodecSupport.decodeIds(fields.getValue("codex")),
            frontierUnlocked = fields.getValue("frontierUnlocked").toBooleanStrict(),
            veteranRobotId = fields.getValue("veteranRobotId").takeIf(String::isNotBlank)?.let(GameId::of),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    companion object {
        const val SLOT_ID = "cryos_ix"
        const val FORMAT_VERSION = 1
    }
}
