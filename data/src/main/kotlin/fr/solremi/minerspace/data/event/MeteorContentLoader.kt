package fr.solremi.minerspace.data.event

import fr.solremi.minerspace.domain.event.MeteorEventDefinition
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class MeteorContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): MeteorEventDefinition {
        val content = repository.readText(path) ?: error("Missing meteor event content: $path")
        return parse(content)
    }

    fun parse(content: String): MeteorEventDefinition = MeteorEventDefinition(
        schemaVersion = content.requireLong("schemaVersion").toIntExact("schemaVersion"),
        contentVersion = content.requireString("contentVersion"),
        durationMillis = Math.multiplyExact(content.requireLong("durationSeconds"), 1_000L),
        spawnIntervalMillis = content.requireLong("spawnIntervalMillis"),
        maxActiveFragments = content.requireLong("maxActiveFragments").toIntExact("maxActiveFragments"),
        fragmentLifetimeMillis = content.requireLong("fragmentLifetimeMillis"),
        rareSpawnAtMillis = Math.multiplyExact(content.requireLong("rareSpawnAtSeconds"), 1_000L),
        standardResourceId = GameId.of(content.requireString("standardResourceId")),
        rareResourceId = GameId.of(content.requireString("rareResourceId")),
        standardRewardPerFragment = content.requireLong("standardRewardPerFragment"),
        rareRewardQuantity = content.requireLong("rareRewardQuantity"),
        captureRadiusMillionths = content.requireLong("captureRadiusMillionths").toIntExact("captureRadiusMillionths"),
        assistedCaptureRadiusMillionths = content.requireLong("assistedCaptureRadiusMillionths").toIntExact("assistedCaptureRadiusMillionths"),
        assistAutoCollectIntervalMillis = content.requireLong("assistAutoCollectIntervalMillis"),
    )

    private fun String.requireString(key: String): String {
        val match = Regex("\"${Regex.escape(key)}\"\s*:\s*\"([^\"]+)\"").find(this)
            ?: error("Missing string: $key")
        return match.groupValues[1]
    }

    private fun String.requireLong(key: String): Long {
        val match = Regex("\"${Regex.escape(key)}\"\s*:\s*(-?[0-9]+)").find(this)
            ?: error("Missing integer: $key")
        return match.groupValues[1].toLong()
    }

    private fun Long.toIntExact(label: String): Int {
        require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label outside Int range" }
        return toInt()
    }

    private companion object {
        const val DEFAULT_PATH = "data/meteor-event.json"
    }
}
