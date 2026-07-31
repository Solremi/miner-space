package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.strategy.*
import fr.solremi.minerspace.shared.GameId

class StrategyStateCodec {
    fun encode(
        state: StrategyState,
        contentVersion: String,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("activeSpecialization=${state.activeSpecialization?.name.orEmpty()}")
            appendLine("trialUsed=${state.trialUsed}")
            appendLine("specializationChangedAt=${state.specializationChangedAtEpochMillis}")
            appendLine("nextModuleSequence=${state.nextModuleSequence}")
            appendLine("transactionSequence=${state.transactionSequence}")
            appendLine("modules=${state.modules.values.sortedBy { it.instanceId }.joinToString(";") { encodeModule(it) }}")
        }
        return SavePayload(slotId, FORMAT_VERSION, contentVersion, text.toByteArray(), savedAtEpochMillis)
    }

    fun decode(payload: SavePayload): StrategyState {
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = payload.bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.associate { line ->
            val p = line.indexOf('='); require(p > 0); line.substring(0, p) to line.substring(p + 1)
        }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        val modules = fields.getValue("modules").takeIf { it.isNotBlank() }?.split(';')?.associate { encoded ->
            val p = encoded.split('|'); require(p.size == 4)
            val instance = OwnedModule(
                instanceId = p[0],
                definitionId = GameId.of(p[1]),
                level = p[2].toInt(),
                equippedRobotId = p[3].takeIf(String::isNotBlank)?.let(GameId::of),
            )
            instance.instanceId to instance
        }.orEmpty()
        return StrategyState(
            activeSpecialization = fields.getValue("activeSpecialization").takeIf(String::isNotBlank)?.let(SpecializationId::valueOf),
            trialUsed = fields.getValue("trialUsed").toBooleanStrict(),
            specializationChangedAtEpochMillis = fields.getValue("specializationChangedAt").toLong(),
            modules = modules,
            nextModuleSequence = fields.getValue("nextModuleSequence").toLong(),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeModule(module: OwnedModule) = listOf(
        module.instanceId,
        module.definitionId.value,
        module.level,
        module.equippedRobotId?.value.orEmpty(),
    ).joinToString("|")

    companion object {
        const val SLOT_ID = "strategy"
        const val FORMAT_VERSION = 1
    }
}
