package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.shared.GameId

class VersionedFieldWriter {
    private val fields = linkedMapOf<String, String>()

    fun put(key: String, value: Any?): VersionedFieldWriter = apply {
        require(KEY_PATTERN.matches(key)) { "Invalid save field key: $key" }
        require(fields.put(key, value?.toString().orEmpty()) == null) { "Duplicate save field: $key" }
    }

    fun encode(): ByteArray = buildString {
        fields.forEach { (key, value) -> appendLine("$key=$value") }
    }.toByteArray(Charsets.UTF_8)

    private companion object {
        val KEY_PATTERN = Regex("[A-Za-z][A-Za-z0-9]*")
    }
}

class VersionedFieldReader private constructor(
    private val fields: Map<String, String>,
    private val label: String,
) {
    fun string(key: String): String = fields[key]
        ?: error("Missing $label field: $key")

    fun optionalString(key: String): String? = fields[key]

    fun int(key: String): Int = string(key).toIntOrNull()
        ?: error("Invalid integer in $label field: $key")

    fun long(key: String): Long = string(key).toLongOrNull()
        ?: error("Invalid long in $label field: $key")

    fun boolean(key: String): Boolean = runCatching { string(key).toBooleanStrict() }
        .getOrElse { error("Invalid boolean in $label field: $key") }

    inline fun <reified T : Enum<T>> enum(key: String): T = runCatching {
        enumValueOf<T>(string(key))
    }.getOrElse { error("Invalid ${T::class.simpleName} in $label field: $key") }

    fun requireOnly(vararg allowed: String) {
        val unknown = fields.keys - allowed.toSet()
        require(unknown.isEmpty()) { "Unknown $label fields: $unknown" }
    }

    companion object {
        fun decode(bytes: ByteArray, label: String): VersionedFieldReader {
            val fields = linkedMapOf<String, String>()
            bytes.toString(Charsets.UTF_8)
                .lineSequence()
                .filter { it.isNotBlank() }
                .forEachIndexed { index, line ->
                    val separator = line.indexOf('=')
                    require(separator > 0) { "Invalid $label line ${index + 1}" }
                    val key = line.substring(0, separator)
                    val value = line.substring(separator + 1)
                    require(fields.put(key, value) == null) { "Duplicate $label field: $key" }
                }
            return VersionedFieldReader(fields, label)
        }
    }
}

object SaveFieldCollections {
    fun encodeIds(values: Collection<GameId>): String = values
        .sortedBy { it.value }
        .joinToString(",") { it.value }

    fun decodeIds(value: String): Set<GameId> = if (value.isBlank()) {
        emptySet()
    } else {
        value.split(',').mapTo(linkedSetOf(), GameId::of)
    }

    fun encodeQuantities(values: Map<GameId, Long>): String = values.entries
        .sortedBy { it.key.value }
        .joinToString(",") { (id, quantity) -> "${id.value}:$quantity" }

    fun decodeQuantities(value: String): Map<GameId, Long> = if (value.isBlank()) {
        emptyMap()
    } else {
        value.split(',').associate { entry ->
            val separator = entry.indexOf(':')
            require(separator > 0 && separator < entry.lastIndex) { "Invalid quantity entry: $entry" }
            val id = GameId.of(entry.substring(0, separator))
            val quantity = entry.substring(separator + 1).toLongOrNull()
                ?: error("Invalid quantity for $id")
            id to quantity
        }
    }
}
