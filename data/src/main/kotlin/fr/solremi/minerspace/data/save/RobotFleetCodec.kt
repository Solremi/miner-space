package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.robot.AutomationPriority
import fr.solremi.minerspace.domain.robot.RenderQuality
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.robot.RobotFamily
import fr.solremi.minerspace.domain.robot.RobotInstance
import fr.solremi.minerspace.domain.robot.RobotStatistics
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId
import java.util.Base64

class RobotFleetCodec {
    fun encode(
        state: RobotAutomationState,
        contentVersion: String,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        require(contentVersion.isNotBlank())
        require(savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("contentVersion=$contentVersion")
            appendLine("lastLogisticsEpochMillis=${state.lastLogisticsEpochMillis}")
            appendLine("priorityCursor=${state.priorityCursor}")
            appendLine("renderQuality=${state.renderQuality.name}")
            appendLine("transactionSequence=${state.transactionSequence}")
            appendLine("robots=${encodeRobots(state.robots.values)}")
        }
        return SavePayload(
            slotId = slotId,
            schemaVersion = FORMAT_VERSION,
            contentVersion = contentVersion,
            bytes = text.toByteArray(Charsets.UTF_8),
            savedAtEpochMillis = savedAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): RobotAutomationState {
        require(payload.schemaVersion == FORMAT_VERSION) { "Unsupported robot save schema" }
        val fields = payload.bytes.toString(Charsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Invalid robot snapshot line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        require(fields.getValue("contentVersion") == payload.contentVersion)
        val robots = decodeRobots(fields.getValue("robots"))
        return RobotAutomationState(
            robots = robots.associateByTo(linkedMapOf(), RobotInstance::id),
            lastLogisticsEpochMillis = fields.getValue("lastLogisticsEpochMillis").toLong(),
            priorityCursor = fields.getValue("priorityCursor").toInt(),
            renderQuality = RenderQuality.valueOf(fields.getValue("renderQuality")),
            transactionSequence = fields.getValue("transactionSequence").toLong(),
        )
    }

    private fun encodeRobots(robots: Collection<RobotInstance>): String = robots
        .sortedBy { it.id.value }
        .joinToString(";") { robot ->
            val stats = robot.statistics
            listOf(
                robot.id.value,
                robot.family.name,
                encodeText(robot.displayName),
                encodeText(robot.serialNumber),
                robot.level,
                robot.trait.name,
                robot.masteryPoints,
                robot.priority.name,
                stats.extracted,
                stats.refined,
                stats.assembled,
                stats.transported,
                stats.activeSeconds,
            ).joinToString("|")
        }

    private fun decodeRobots(value: String): List<RobotInstance> = if (value.isBlank()) {
        emptyList()
    } else {
        value.split(';').map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 13) { "Invalid robot entry" }
            RobotInstance(
                id = GameId.of(parts[0]),
                family = RobotFamily.valueOf(parts[1]),
                displayName = decodeText(parts[2]),
                serialNumber = decodeText(parts[3]),
                level = parts[4].toInt(),
                trait = RobotTrait.valueOf(parts[5]),
                masteryPoints = parts[6].toLong(),
                priority = AutomationPriority.valueOf(parts[7]),
                statistics = RobotStatistics(
                    extracted = parts[8].toLong(),
                    refined = parts[9].toLong(),
                    assembled = parts[10].toLong(),
                    transported = parts[11].toLong(),
                    activeSeconds = parts[12].toLong(),
                ),
            )
        }
    }

    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeText(value: String): String = Base64.getUrlDecoder().decode(value)
        .toString(Charsets.UTF_8)

    companion object {
        const val FORMAT_VERSION = 1
        const val SLOT_ID = "robots"
    }
}
