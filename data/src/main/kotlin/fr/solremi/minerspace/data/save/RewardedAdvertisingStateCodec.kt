package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.ads.*
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId
import java.util.Base64

class RewardedAdvertisingStateCodec {
    fun encode(
        state: RewardedAdvertisingState,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        require(savedAtEpochMillis >= 0L)
        val e = state.entitlements
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("dayIndex=${state.dayIndex}")
            appendLine("committedToday=${state.committedToday}")
            appendLine("committedByOffer=${gameIdIntMap(state.committedByOffer)}")
            appendLine("lastCommitted=${gameIdLongMap(state.lastCommittedAtByOffer)}")
            appendLine("committedRequests=${texts(state.committedRequestIds)}")
            appendLine("scopes=${encodeScopes(state.scopeCommittedByOffer)}")
            appendLine("pending=${state.pendingRewards.values.sortedBy { it.requestId }.joinToString(";") { encodeText(encodePending(it)) }}")
            appendLine("timeRelayTokens=${e.timeRelayTokens}")
            appendLine("offlineDoubleTokens=${e.offlineDoubleTokens}")
            appendLine("standardMaterialMinutes=${e.standardMaterialMinutes}")
            appendLine("premiumContractTokens=${e.premiumContractTokens}")
            appendLine("analysisTokens=${e.analysisTokens}")
            appendLine("meteorRecoveryTokens=${e.meteorRecoveryTokens}")
            appendLine("meteorExtensionSeconds=${e.meteorExtensionSeconds}")
            appendLine("orbitalBoostPercent=${e.orbitalBoostPercent}")
            appendLine("orbitalBoostUntil=${e.orbitalBoostUntilEpochMillis}")
            appendLine("transactionSequence=${state.transactionSequence}")
        }
        return SavePayload(slotId, FORMAT_VERSION, CONTENT_VERSION, text.toByteArray(), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): RewardedAdvertisingState {
        require(payload.slotId == SLOT_ID && payload.schemaVersion == FORMAT_VERSION)
        val f = payload.bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).associate { line ->
            val p = line.indexOf('='); require(p > 0); line.substring(0, p) to line.substring(p + 1)
        }
        require(f.getValue("format").toInt() == FORMAT_VERSION)
        val pending = f.getValue("pending").takeIf(String::isNotBlank)?.split(';')?.associate { encoded ->
            val value = decodePending(decodeText(encoded)); value.requestId to value
        }.orEmpty()
        return RewardedAdvertisingState(
            dayIndex = f.getValue("dayIndex").toLong(),
            committedToday = f.getValue("committedToday").toInt(),
            committedByOffer = decodeGameIdIntMap(f.getValue("committedByOffer")),
            lastCommittedAtByOffer = decodeGameIdLongMap(f.getValue("lastCommitted")),
            committedRequestIds = decodeTexts(f.getValue("committedRequests")),
            scopeCommittedByOffer = decodeScopes(f.getValue("scopes")),
            pendingRewards = pending,
            entitlements = RewardEntitlements(
                timeRelayTokens = f.getValue("timeRelayTokens").toInt(),
                offlineDoubleTokens = f.getValue("offlineDoubleTokens").toInt(),
                standardMaterialMinutes = f.getValue("standardMaterialMinutes").toLong(),
                premiumContractTokens = f.getValue("premiumContractTokens").toInt(),
                analysisTokens = f.getValue("analysisTokens").toInt(),
                meteorRecoveryTokens = f.getValue("meteorRecoveryTokens").toInt(),
                meteorExtensionSeconds = f.getValue("meteorExtensionSeconds").toLong(),
                orbitalBoostPercent = f.getValue("orbitalBoostPercent").toInt(),
                orbitalBoostUntilEpochMillis = f.getValue("orbitalBoostUntil").toLong(),
            ),
            transactionSequence = f.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodePending(value: PendingAdReward) = listOf(
        encodeText(value.requestId), value.offerId.value, encodeText(value.scopeId.orEmpty()), value.status.name,
        value.preparedAtEpochMillis, value.sdkRewardedAtEpochMillis,
    ).joinToString("|")

    private fun decodePending(value: String): PendingAdReward {
        val p = value.split('|'); require(p.size == 6)
        return PendingAdReward(
            requestId = decodeText(p[0]),
            offerId = GameId.of(p[1]),
            scopeId = decodeText(p[2]).takeIf(String::isNotBlank),
            status = PendingRewardStatus.valueOf(p[3]),
            preparedAtEpochMillis = p[4].toLong(),
            sdkRewardedAtEpochMillis = p[5].toLong(),
        )
    }

    private fun encodeScopes(values: Map<String, Set<GameId>>) = values.entries.sortedBy { it.key }.joinToString(";") { (key, ids) ->
        "${encodeText(key)}:${ids.sortedBy { it.value }.joinToString(",") { it.value }}"
    }
    private fun decodeScopes(value: String): Map<String, Set<GameId>> = if (value.isBlank()) emptyMap() else value.split(';').associate { encoded ->
        val p = encoded.indexOf(':'); require(p > 0)
        decodeText(encoded.substring(0, p)) to decodeIds(encoded.substring(p + 1))
    }
    private fun gameIdIntMap(values: Map<GameId, Int>) = values.entries.sortedBy { it.key.value }.joinToString(",") { "${it.key.value}:${it.value}" }
    private fun gameIdLongMap(values: Map<GameId, Long>) = values.entries.sortedBy { it.key.value }.joinToString(",") { "${it.key.value}:${it.value}" }
    private fun decodeGameIdIntMap(value: String) = if (value.isBlank()) emptyMap() else value.split(',').associate { val p=it.split(':'); GameId.of(p[0]) to p[1].toInt() }
    private fun decodeGameIdLongMap(value: String) = if (value.isBlank()) emptyMap() else value.split(',').associate { val p=it.split(':'); GameId.of(p[0]) to p[1].toLong() }
    private fun texts(values: Set<String>) = values.sorted().joinToString(",") { encodeText(it) }
    private fun decodeTexts(value: String) = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), ::decodeText)
    private fun decodeIds(value: String) = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)
    private fun encodeText(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun decodeText(value: String) = if (value.isBlank()) "" else Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

    companion object {
        const val SLOT_ID = "rewarded_ads"
        const val FORMAT_VERSION = 1
        const val CONTENT_VERSION = "1.0.0"
    }
}
