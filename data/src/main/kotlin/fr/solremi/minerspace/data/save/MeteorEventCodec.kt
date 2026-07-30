package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.event.MeteorEventPhase
import fr.solremi.minerspace.domain.event.MeteorEventState
import fr.solremi.minerspace.domain.event.MeteorFragment
import fr.solremi.minerspace.domain.event.MeteorFragmentKind
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId

class MeteorEventCodec {
    fun encode(
        state: MeteorEventState,
        contentVersion: String,
        savedAtEpochMillis: Long = System.currentTimeMillis().coerceAtLeast(0L),
    ): SavePayload {
        require(contentVersion.isNotBlank())
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("contentVersion=$contentVersion")
            appendLine("eventId=${state.eventId}")
            appendLine("seed=${state.seed}")
            appendLine("phase=${state.phase.name}")
            appendLine("elapsedActiveMillis=${state.elapsedActiveMillis}")
            appendLine("nextSpawnIndex=${state.nextSpawnIndex}")
            appendLine("rareSpawned=${state.rareSpawned}")
            appendLine("fragments=${encodeFragments(state.fragments)}")
            appendLine("standardCollected=${state.standardCollected}")
            appendLine("rareCollected=${state.rareCollected}")
            appendLine("assistanceEnabled=${state.assistanceEnabled}")
            appendLine("lastAssistAtMillis=${state.lastAssistAtMillis}")
            appendLine("expectedStandardInventory=${state.expectedStandardInventory ?: -1L}")
            appendLine("expectedRareInventory=${state.expectedRareInventory ?: -1L}")
            appendLine("codex=${encodeIds(state.codexEntryIds)}")
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

    fun decode(payload: SavePayload): MeteorEventState {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = payload.bytes.toString(Charsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Invalid meteor snapshot line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        require(fields.getValue("contentVersion") == payload.contentVersion)
        return MeteorEventState(
            eventId = fields.getValue("eventId"),
            seed = fields.getValue("seed").toLong(),
            phase = MeteorEventPhase.valueOf(fields.getValue("phase")),
            elapsedActiveMillis = fields.getValue("elapsedActiveMillis").toLong(),
            nextSpawnIndex = fields.getValue("nextSpawnIndex").toLong(),
            rareSpawned = fields.getValue("rareSpawned").toBooleanStrict(),
            fragments = decodeFragments(fields.getValue("fragments")),
            standardCollected = fields.getValue("standardCollected").toLong(),
            rareCollected = fields.getValue("rareCollected").toLong(),
            assistanceEnabled = fields.getValue("assistanceEnabled").toBooleanStrict(),
            lastAssistAtMillis = fields.getValue("lastAssistAtMillis").toLong(),
            expectedStandardInventory = fields.getValue("expectedStandardInventory").toLong().takeIf { it >= 0L },
            expectedRareInventory = fields.getValue("expectedRareInventory").toLong().takeIf { it >= 0L },
            codexEntryIds = decodeIds(fields.getValue("codex")),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeFragments(fragments: List<MeteorFragment>): String = fragments.joinToString(";") { fragment ->
        listOf(
            fragment.id,
            fragment.kind.name,
            fragment.spawnXMillionths,
            fragment.spawnYMillionths,
            fragment.velocityXMillionthsPerSecond,
            fragment.velocityYMillionthsPerSecond,
            fragment.spawnedAtActiveMillis,
        ).joinToString("|")
    }

    private fun decodeFragments(value: String): List<MeteorFragment> = if (value.isBlank()) {
        emptyList()
    } else {
        value.split(';').map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 7) { "Invalid meteor fragment" }
            MeteorFragment(
                id = parts[0],
                kind = MeteorFragmentKind.valueOf(parts[1]),
                spawnXMillionths = parts[2].toInt(),
                spawnYMillionths = parts[3].toInt(),
                velocityXMillionthsPerSecond = parts[4].toInt(),
                velocityYMillionthsPerSecond = parts[5].toInt(),
                spawnedAtActiveMillis = parts[6].toLong(),
            )
        }
    }

    private fun encodeIds(values: Set<GameId>): String = values.sortedBy { it.value }.joinToString(",") { it.value }
    private fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)

    companion object {
        const val FORMAT_VERSION = 1
        const val SLOT_ID = "meteor_event"
    }
}
