package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.progression.ContractTier
import fr.solremi.minerspace.domain.progression.ProgressionState
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId

class ProgressionStateCodec {
    fun encode(state: ProgressionState, contentVersion: String, savedAtEpochMillis: Long, slotId: String = SLOT_ID): SavePayload {
        require(contentVersion.isNotBlank() && savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("tutorialStepIndex=${state.tutorialStepIndex}")
            appendLine("completedTutorial=${ids(state.completedTutorialIds)}")
            appendLine("claimedMissions=${ids(state.claimedMissionIds)}")
            appendLine("contractCycles=${ContractTier.entries.joinToString(",") { "${it.name}:${state.contractCycles[it] ?: 0}" }}")
            appendLine("completedContracts=${state.completedContractCount}")
            appendLine("discoveredCodex=${ids(state.discoveredCodexEntryIds)}")
            appendLine("claimedCollections=${ids(state.claimedCollectionIds)}")
            appendLine("selectedObjective=${state.selectedObjectiveId?.value.orEmpty()}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(slotId, FORMAT_VERSION, contentVersion, text.toByteArray(), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): ProgressionState {
        require(payload.slotId == SLOT_ID && payload.schemaVersion == FORMAT_VERSION)
        val f = payload.bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).associate { line -> val p=line.indexOf('=');require(p>0);line.substring(0,p) to line.substring(p+1) }
        require(f.getValue("format").toInt() == FORMAT_VERSION)
        return ProgressionState(
            tutorialStepIndex = f.getValue("tutorialStepIndex").toInt(),
            completedTutorialIds = decodeIds(f.getValue("completedTutorial")),
            claimedMissionIds = decodeIds(f.getValue("claimedMissions")),
            contractCycles = f.getValue("contractCycles").split(',').associate { val p=it.split(':');ContractTier.valueOf(p[0]) to p[1].toInt() },
            completedContractCount = f.getValue("completedContracts").toLong(),
            discoveredCodexEntryIds = decodeIds(f.getValue("discoveredCodex")),
            claimedCollectionIds = decodeIds(f.getValue("claimedCollections")),
            selectedObjectiveId = f.getValue("selectedObjective").takeIf(String::isNotBlank)?.let(GameId::of),
            transactionSequence = f.getValue("transactionSequence").toLong(),
        )
    }

    private fun ids(values:Set<GameId>)=values.sortedBy{it.value}.joinToString(","){it.value}
    private fun decodeIds(value:String)=if(value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(),GameId::of)
    companion object { const val SLOT_ID="progression"; const val FORMAT_VERSION=1 }
}
