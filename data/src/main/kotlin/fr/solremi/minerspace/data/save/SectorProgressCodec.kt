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
        require(contentVersion.isNotBlank())
        require(savedAtEpochMillis >= 0L)
        val bytes = VersionedFieldWriter()
            .put("format", FORMAT_VERSION)
            .put("contentVersion", contentVersion)
            .put("revealed", SaveFieldCollections.encodeIds(state.revealedSectorIds))
            .put("unlocked", SaveFieldCollections.encodeIds(state.unlockedSectorIds))
            .put("rareDeposits", SaveFieldCollections.encodeIds(state.discoveredRareDepositIds))
            .put("spentSpaceDollars", state.spentSpaceDollars)
            .put("spentComponents", SaveFieldCollections.encodeQuantities(state.spentComponents))
            .put("activeMission", state.activeMissionSectorId?.value)
            .put("transactionSequence", state.transactionSequence)
            .encode()
        return SavePayload(
            slotId = SLOT_ID,
            schemaVersion = FORMAT_VERSION,
            contentVersion = contentVersion,
            bytes = bytes,
            savedAtEpochMillis = savedAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): ExplorationState {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = VersionedFieldReader.decode(payload.bytes, "exploration snapshot")
        fields.requireOnly(
            "format",
            "contentVersion",
            "revealed",
            "unlocked",
            "rareDeposits",
            "spentSpaceDollars",
            "spentComponents",
            "activeMission",
            "transactionSequence",
        )
        require(fields.int("format") == FORMAT_VERSION)
        require(fields.string("contentVersion") == payload.contentVersion)
        return ExplorationState(
            revealedSectorIds = SaveFieldCollections.decodeIds(fields.string("revealed")),
            unlockedSectorIds = SaveFieldCollections.decodeIds(fields.string("unlocked")),
            discoveredRareDepositIds = SaveFieldCollections.decodeIds(fields.string("rareDeposits")),
            spentSpaceDollars = fields.long("spentSpaceDollars"),
            spentComponents = SaveFieldCollections.decodeQuantities(fields.string("spentComponents")),
            activeMissionSectorId = fields.string("activeMission")
                .takeIf { it.isNotBlank() }
                ?.let(GameId::of),
            transactionSequence = fields.long("transactionSequence"),
        )
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val SLOT_ID = "exploration"
    }
}
