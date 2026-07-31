package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.frontier.*
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId
import java.util.Base64

class FrontierStateCodec {
    fun encode(state: FrontierState, savedAtEpochMillis: Long, slotId: String = SLOT_ID): SavePayload {
        require(savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("seed=${state.seed}")
            appendLine("nextGenerationIndex=${state.nextGenerationIndex}")
            appendLine("activeWorldId=${state.activeWorldId?.value.orEmpty()}")
            appendLine("lastGeneratedSignature=${encodeText(state.lastGeneratedSignature.orEmpty())}")
            appendLine("permanentBonusIds=${ids(state.permanentBonusIds)}")
            appendLine("cosmeticIds=${ids(state.cosmeticIds)}")
            appendLine("collectionIds=${ids(state.collectionIds)}")
            appendLine("completedWorldCount=${state.completedWorldCount}")
            appendLine("transactionSequence=${state.transactionSequence}")
            appendLine("worlds=${state.worlds.values.sortedBy { it.definition.generationIndex }.joinToString(";") { encodeText(encodeWorld(it)) }}")
        }
        return SavePayload(slotId, FORMAT_VERSION, CONTENT_VERSION, text.toByteArray(Charsets.UTF_8), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): FrontierState {
        require(payload.slotId == SLOT_ID && payload.schemaVersion == FORMAT_VERSION)
        val fields = payload.bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).associate { line ->
            val separator = line.indexOf('='); require(separator > 0)
            line.substring(0, separator) to line.substring(separator + 1)
        }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        val worlds = fields.getValue("worlds").takeIf(String::isNotBlank)?.split(';')?.associate { encoded ->
            val world = decodeWorld(decodeText(encoded)); world.definition.id to world
        }.orEmpty()
        return FrontierState(
            seed = fields.getValue("seed").toLong(),
            nextGenerationIndex = fields.getValue("nextGenerationIndex").toInt(),
            worlds = worlds,
            activeWorldId = fields.getValue("activeWorldId").takeIf(String::isNotBlank)?.let(GameId::of),
            lastGeneratedSignature = decodeText(fields.getValue("lastGeneratedSignature")).takeIf(String::isNotBlank),
            permanentBonusIds = decodeIds(fields.getValue("permanentBonusIds")),
            cosmeticIds = decodeIds(fields.getValue("cosmeticIds")),
            collectionIds = decodeIds(fields.getValue("collectionIds")),
            completedWorldCount = fields.getValue("completedWorldCount").toInt(),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeWorld(world: FrontierWorldProgress): String {
        val definition = world.definition
        val sectors = definition.sectors.joinToString(",") { sector ->
            listOf(sector.id.value, sector.templateId.value, sector.requiredSectorId?.value.orEmpty()).joinToString("~")
        }
        return listOf(
            definition.id.value, definition.seed, definition.generationIndex, definition.family.name,
            definition.difficulty.name, ids(definition.modifierIds), definition.objectiveId.value, sectors,
            definition.targetProgress, definition.rewardKind.name, definition.rewardAmount, definition.estimatedDays,
            world.progress, world.actionCount, world.status.name, world.startedAtEpochMillis,
            world.updatedAtEpochMillis, world.completedAtEpochMillis ?: -1L,
        ).joinToString("|")
    }

    private fun decodeWorld(value: String): FrontierWorldProgress {
        val p = value.split('|'); require(p.size == 18)
        val sectors = p[7].split(',').filter(String::isNotBlank).map { encoded ->
            val s = encoded.split('~'); require(s.size == 3)
            GeneratedFrontierSector(GameId.of(s[0]), GameId.of(s[1]), s[2].takeIf(String::isNotBlank)?.let(GameId::of))
        }
        val definition = FrontierWorldDefinition(
            id = GameId.of(p[0]), seed = p[1].toLong(), generationIndex = p[2].toInt(),
            family = FrontierVisualFamily.valueOf(p[3]), difficulty = FrontierDifficulty.valueOf(p[4]),
            modifierIds = decodeIds(p[5]), objectiveId = GameId.of(p[6]), sectors = sectors,
            targetProgress = p[8].toLong(), rewardKind = FrontierRewardKind.valueOf(p[9]),
            rewardAmount = p[10].toLong(), estimatedDays = p[11].toInt(),
        )
        val completedAt = p[17].toLong().takeIf { it >= 0L }
        return FrontierWorldProgress(
            definition, p[12].toLong(), p[13].toInt(), FrontierWorldStatus.valueOf(p[14]),
            p[15].toLong(), p[16].toLong(), completedAt,
        )
    }

    private fun ids(values: Set<GameId>) = values.sortedBy { it.value }.joinToString(",") { it.value }
    private fun decodeIds(value: String) = if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)
    private fun encodeText(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decodeText(value: String) = if (value.isBlank()) "" else Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

    companion object {
        const val SLOT_ID = "frontier"
        const val FORMAT_VERSION = 1
        const val CONTENT_VERSION = "1.0.0"
    }
}
