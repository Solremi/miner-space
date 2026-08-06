package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.shared.GameId
import java.util.Base64

internal object KeyValueSaveCodecSupport {
    fun fields(payload: SavePayload): Map<String, String> = payload.bytes.toString(Charsets.UTF_8)
        .lineSequence()
        .filter { it.isNotBlank() }
        .associate { line ->
            val separator = line.indexOf('=')
            require(separator > 0)
            line.substring(0, separator) to line.substring(separator + 1)
        }

    fun encodeIds(values: Set<GameId>): String =
        values.sortedBy { it.value }.joinToString(",") { it.value }

    fun decodeIds(value: String): Set<GameId> =
        if (value.isBlank()) emptySet() else value.split(',').mapTo(linkedSetOf(), GameId::of)

    fun encodeLongMap(values: Map<GameId, Long>): String =
        values.entries.sortedBy { it.key.value }.joinToString(",") { "${it.key.value}:${it.value}" }

    fun decodeLongMap(value: String): Map<GameId, Long> =
        if (value.isBlank()) emptyMap() else value.split(',').associate { encoded ->
            val parts = encoded.split(':')
            require(parts.size == 2)
            GameId.of(parts[0]) to parts[1].toLong()
        }

    fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    fun decodeText(value: String): String =
        Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
}
