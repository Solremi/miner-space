package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.narrative.NarrativeState
import fr.solremi.minerspace.domain.narrative.PendingNarrativeGrant
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId
import java.util.Base64

class NarrativeStateCodec {
    fun encode(
        state: NarrativeState,
        contentVersion: String,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        require(contentVersion.isNotBlank() && savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("read=${encodeIds(state.readTransmissionIds)}")
            appendLine("resolved=${encodeIds(state.resolvedChapterIds)}")
            appendLine("attempts=${encodeAttempts(state.anomalyAttempts)}")
            appendLine("rare=${encodeIds(state.discoveredRareResourceIds)}")
            appendLine("veteran=${state.veteranRobotId?.value.orEmpty()}")
            appendLine("selected=${state.selectedChapterId?.value.orEmpty()}")
            appendLine("pending=${state.pendingGrant?.let(::encodeGrant).orEmpty()}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(slotId, FORMAT_VERSION, contentVersion, text.toByteArray(Charsets.UTF_8), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): NarrativeState {
        require(payload.schemaVersion == FORMAT_VERSION) { "Unsupported narrative save schema" }
        val fields = payload.bytes.toString(Charsets.UTF_8)
            .lineSequence().filter { it.isNotBlank() }.associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Invalid narrative snapshot line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        return NarrativeState(
            readTransmissionIds = decodeIds(fields.getValue("read")),
            resolvedChapterIds = decodeIds(fields.getValue("resolved")),
            anomalyAttempts = decodeAttempts(fields.getValue("attempts")),
            discoveredRareResourceIds = decodeIds(fields.getValue("rare")),
            veteranRobotId = fields.getValue("veteran").takeIf(String::isNotBlank)?.let(GameId::of),
            selectedChapterId = fields.getValue("selected").takeIf(String::isNotBlank)?.let(GameId::of),
            pendingGrant = fields.getValue("pending").takeIf(String::isNotBlank)?.let(::decodeGrant),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeIds(values: Set<GameId>): String = values.sortedBy { it.value }.joinToString(",") { it.value }
    private fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)
    private fun encodeAttempts(values: Map<GameId, Int>): String = values.entries.sortedBy { it.key.value }.joinToString(",") { "${it.key.value}:${it.value}" }
    private fun decodeAttempts(value: String): Map<GameId, Int> = if (value.isBlank()) emptyMap() else value.split(',').associate { encoded ->
        val parts = encoded.split(':'); require(parts.size == 2); GameId.of(parts[0]) to parts[1].toInt()
    }

    private fun encodeGrant(grant: PendingNarrativeGrant): String = listOf(
        encodeText(grant.grantId),
        grant.chapterId.value,
        grant.rareResourceId?.value.orEmpty(),
        grant.expectedRareTotal,
        grant.veteranRobotId?.value.orEmpty(),
        grant.expectedVeteranMastery,
    ).joinToString("|")

    private fun decodeGrant(value: String): PendingNarrativeGrant {
        val parts = value.split('|'); require(parts.size == 6)
        return PendingNarrativeGrant(
            grantId = decodeText(parts[0]),
            chapterId = GameId.of(parts[1]),
            rareResourceId = parts[2].takeIf(String::isNotBlank)?.let(GameId::of),
            expectedRareTotal = parts[3].toLong(),
            veteranRobotId = parts[4].takeIf(String::isNotBlank)?.let(GameId::of),
            expectedVeteranMastery = parts[5].toLong(),
        )
    }

    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decodeText(value: String): String = Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

    companion object {
        const val SLOT_ID = "narrative"
        const val FORMAT_VERSION = 1
    }
}
