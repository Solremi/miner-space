package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.cryos.CryosIxState
import fr.solremi.minerspace.domain.prestige.*
import fr.solremi.minerspace.domain.robot.RobotStatistics
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId
import java.util.Base64

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
            appendLine("codex=${encodeIds(state.permanentCodexEntryIds)}")
            appendLine("archives=${encodeIds(state.permanentArchiveIds)}")
            appendLine("bonuses=${encodeIds(state.permanentBonusIds)}")
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
        val fields = fields(payload)
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        return PrestigeState(
            activePlanet = PlanetId.valueOf(fields.getValue("activePlanet")),
            stellarCores = fields.getValue("stellarCores").toLong(),
            completedTransfers = fields.getValue("completedTransfers").toInt(),
            permanentCodexEntryIds = decodeIds(fields.getValue("codex")),
            permanentArchiveIds = decodeIds(fields.getValue("archives")),
            permanentBonusIds = decodeIds(fields.getValue("bonuses")),
            veteranRobot = fields.getValue("veteran").takeIf(String::isNotBlank)?.let(::decodeVeteran),
            pendingTransfer = fields.getValue("pending").takeIf(String::isNotBlank)?.let(::decodePending),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun fields(payload: SavePayload): Map<String, String> = payload.bytes.toString(Charsets.UTF_8)
        .lineSequence().filter { it.isNotBlank() }.associate { line ->
            val separator = line.indexOf('=')
            require(separator > 0)
            line.substring(0, separator) to line.substring(separator + 1)
        }

    private fun encodePending(value: PendingPlanetTransfer): String = listOf(
        encodeText(value.transferId),
        value.sourcePlanet.name,
        value.destinationPlanet.name,
        value.expectedStellarCores,
        encodeIds(value.preservedCodexEntryIds),
        encodeIds(value.preservedArchiveIds),
        encodeIds(value.preservedBonusIds),
        encodeVeteran(value.veteranRobot),
        value.preparedAtEpochMillis,
    ).joinToString("~")

    private fun decodePending(value: String): PendingPlanetTransfer {
        val parts = value.split('~')
        require(parts.size == 9)
        return PendingPlanetTransfer(
            transferId = decodeText(parts[0]),
            sourcePlanet = PlanetId.valueOf(parts[1]),
            destinationPlanet = PlanetId.valueOf(parts[2]),
            expectedStellarCores = parts[3].toLong(),
            preservedCodexEntryIds = decodeIds(parts[4]),
            preservedArchiveIds = decodeIds(parts[5]),
            preservedBonusIds = decodeIds(parts[6]),
            veteranRobot = decodeVeteran(parts[7]),
            preparedAtEpochMillis = parts[8].toLong(),
        )
    }

    private fun encodeVeteran(value: VeteranRobotSnapshot): String {
        val stats = value.statistics
        return listOf(
            value.id.value,
            encodeText(value.displayName),
            encodeText(value.serialNumber),
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
            displayName = decodeText(parts[1]),
            serialNumber = decodeText(parts[2]),
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

    private fun encodeIds(values: Set<GameId>): String = values.sortedBy { it.value }.joinToString(",") { it.value }
    private fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)
    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun decodeText(value: String): String = Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

    companion object {
        const val SLOT_ID = "prestige"
        const val FORMAT_VERSION = 1
        const val CONTENT_VERSION = "1.0.0"
    }
}

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
            appendLine("inventory=${encodeMap(state.inventory)}")
            appendLine("technologies=${encodeIds(state.installedTechnologyIds)}")
            appendLine("modules=${encodeIds(state.craftedModuleIds)}")
            appendLine("sectors=${encodeIds(state.unlockedSectorIds)}")
            appendLine("mainMissions=${encodeIds(state.completedMainMissionIds)}")
            appendLine("secondaryMissions=${encodeIds(state.completedSecondaryMissionIds)}")
            appendLine("events=${encodeIds(state.resolvedEventIds)}")
            appendLine("narrative=${encodeIds(state.narrativeDiscoveryIds)}")
            appendLine("codex=${encodeIds(state.discoveredCodexEntryIds)}")
            appendLine("frontierUnlocked=${state.frontierUnlocked}")
            appendLine("veteranRobotId=${state.veteranRobotId?.value.orEmpty()}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(slotId, FORMAT_VERSION, contentVersion, text.toByteArray(), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): CryosIxState {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = payload.bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.associate { line ->
            val separator = line.indexOf('=')
            require(separator > 0)
            line.substring(0, separator) to line.substring(separator + 1)
        }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        return CryosIxState(
            baseInstalled = fields.getValue("baseInstalled").toBooleanStrict(),
            energy = fields.getValue("energy").toLong(),
            heat = fields.getValue("heat").toLong(),
            coldExposure = fields.getValue("coldExposure").toLong(),
            thermalNodes = fields.getValue("thermalNodes").toInt(),
            inventory = decodeMap(fields.getValue("inventory")),
            installedTechnologyIds = decodeIds(fields.getValue("technologies")),
            craftedModuleIds = decodeIds(fields.getValue("modules")),
            unlockedSectorIds = decodeIds(fields.getValue("sectors")),
            completedMainMissionIds = decodeIds(fields.getValue("mainMissions")),
            completedSecondaryMissionIds = decodeIds(fields.getValue("secondaryMissions")),
            resolvedEventIds = decodeIds(fields.getValue("events")),
            narrativeDiscoveryIds = decodeIds(fields.getValue("narrative")),
            discoveredCodexEntryIds = decodeIds(fields.getValue("codex")),
            frontierUnlocked = fields.getValue("frontierUnlocked").toBooleanStrict(),
            veteranRobotId = fields.getValue("veteranRobotId").takeIf(String::isNotBlank)?.let(GameId::of),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeIds(values: Set<GameId>): String = values.sortedBy { it.value }.joinToString(",") { it.value }
    private fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)
    private fun encodeMap(values: Map<GameId, Long>): String = values.entries.sortedBy { it.key.value }.joinToString(",") { "${it.key.value}:${it.value}" }
    private fun decodeMap(value: String): Map<GameId, Long> = if (value.isBlank()) emptyMap() else value.split(',').associate { encoded ->
        val parts = encoded.split(':')
        require(parts.size == 2)
        GameId.of(parts[0]) to parts[1].toLong()
    }

    companion object {
        const val SLOT_ID = "cryos_ix"
        const val FORMAT_VERSION = 1
    }
}
