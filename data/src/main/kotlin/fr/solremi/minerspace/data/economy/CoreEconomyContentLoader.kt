package fr.solremi.minerspace.data.economy

import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.MULTIPLIER_SCALE
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class CoreEconomyContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): EconomyDefinitions {
        val content = repository.readText(path)
            ?: error("Missing economy content: $path")
        return parse(content)
    }

    fun parse(content: String): EconomyDefinitions {
        val root = JsonParser(content).parse().asObject("root")
        val schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion")
        val contentVersion = root.requireString("contentVersion")
        val items = root.requireArray("items")

        val resources = linkedMapOf<GameId, ResourceDefinition>()
        val deposits = linkedMapOf<GameId, DepositDefinition>()
        items.values.forEachIndexed { index, value ->
            val item = value.asObject("items[$index]")
            when (item.requireString("type")) {
                "resource" -> {
                    val definition = ResourceDefinition(
                        id = GameId.of(item.requireString("id")),
                        nameKey = item.requireString("nameKey"),
                        unitSalePrice = item.requireLong("unitSalePrice"),
                        storageCapacity = item.requireLong("storageCapacity"),
                        sellable = item.optionalBoolean("sellable") ?: true,
                    )
                    require(resources.put(definition.id, definition) == null) {
                        "Duplicate resource id: ${definition.id}"
                    }
                }

                "deposit" -> {
                    val definition = DepositDefinition(
                        id = GameId.of(item.requireString("id")),
                        resourceId = GameId.of(item.requireString("resourceId")),
                        initialReserve = item.requireLong("initialReserve"),
                        extractionPerSecond = item.requireLong("extractionPerSecond"),
                        transportCapacity = item.requireLong("transportCapacity"),
                        productionMultiplier = item.optionalLong("productionMultiplier")
                            ?: MULTIPLIER_SCALE,
                    )
                    require(deposits.put(definition.id, definition) == null) {
                        "Duplicate deposit id: ${definition.id}"
                    }
                }

                else -> error("Unknown economy item type at index $index")
            }
        }

        return EconomyDefinitions(
            schemaVersion = schemaVersion,
            contentVersion = contentVersion,
            resources = resources,
            deposits = deposits,
        )
    }

    private companion object {
        const val DEFAULT_PATH = "data/core-economy.json"
    }
}

private sealed interface JsonValue {
    data class ObjectValue(val values: Map<String, JsonValue>) : JsonValue
    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: Long) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object NullValue : JsonValue
}

private fun JsonValue.asObject(location: String): JsonValue.ObjectValue =
    this as? JsonValue.ObjectValue ?: error("Expected object at $location")

private fun JsonValue.ObjectValue.requireString(key: String): String =
    (values[key] as? JsonValue.StringValue)?.value ?: error("Missing string: $key")

private fun JsonValue.ObjectValue.requireLong(key: String): Long =
    (values[key] as? JsonValue.NumberValue)?.value ?: error("Missing integer: $key")

private fun JsonValue.ObjectValue.optionalLong(key: String): Long? =
    (values[key] as? JsonValue.NumberValue)?.value

private fun JsonValue.ObjectValue.optionalBoolean(key: String): Boolean? =
    (values[key] as? JsonValue.BooleanValue)?.value

private fun JsonValue.ObjectValue.requireArray(key: String): JsonValue.ArrayValue =
    values[key] as? JsonValue.ArrayValue ?: error("Missing array: $key")

private fun Long.toIntExact(label: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label is outside Int range" }
    return toInt()
}

private class JsonParser(
    private val source: String,
) {
    private var index = 0

    fun parse(): JsonValue {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        require(index == source.length) { "Unexpected trailing JSON at index $index" }
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        require(index < source.length) { "Unexpected end of JSON" }
        return when (source[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.StringValue(parseString())
            't' -> parseLiteral("true", JsonValue.BooleanValue(true))
            'f' -> parseLiteral("false", JsonValue.BooleanValue(false))
            'n' -> parseLiteral("null", JsonValue.NullValue)
            '-', in '0'..'9' -> JsonValue.NumberValue(parseLong())
            else -> error("Unexpected JSON token '${source[index]}' at index $index")
        }
    }

    private fun parseObject(): JsonValue.ObjectValue {
        expect('{')
        skipWhitespace()
        val values = linkedMapOf<String, JsonValue>()
        if (peek('}')) {
            index++
            return JsonValue.ObjectValue(values)
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val previous = values.put(key, parseValue())
            require(previous == null) { "Duplicate JSON key: $key" }
            skipWhitespace()
            when {
                peek(',') -> index++
                peek('}') -> {
                    index++
                    return JsonValue.ObjectValue(values)
                }
                else -> error("Expected ',' or '}' at index $index")
            }
        }
    }

    private fun parseArray(): JsonValue.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<JsonValue>()
        if (peek(']')) {
            index++
            return JsonValue.ArrayValue(values)
        }
        while (true) {
            values += parseValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
                    return JsonValue.ArrayValue(values)
                }
                else -> error("Expected ',' or ']' at index $index")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val output = StringBuilder()
        while (index < source.length) {
            val char = source[index++]
            when (char) {
                '"' -> return output.toString()
                '\\' -> {
                    require(index < source.length) { "Unterminated escape sequence" }
                    when (val escaped = source[index++]) {
                        '"', '\\', '/' -> output.append(escaped)
                        'b' -> output.append('\b')
                        'f' -> output.append('\u000C')
                        'n' -> output.append('\n')
                        'r' -> output.append('\r')
                        't' -> output.append('\t')
                        'u' -> output.append(parseUnicodeEscape())
                        else -> error("Unsupported escape: \\$escaped")
                    }
                }
                else -> output.append(char)
            }
        }
        error("Unterminated JSON string")
    }

    private fun parseUnicodeEscape(): Char {
        require(index + 4 <= source.length) { "Invalid unicode escape" }
        val code = source.substring(index, index + 4).toInt(16)
        index += 4
        return code.toChar()
    }

    private fun parseLong(): Long {
        val start = index
        if (peek('-')) index++
        require(index < source.length && source[index].isDigit()) { "Invalid number at index $index" }
        while (index < source.length && source[index].isDigit()) index++
        require(index >= source.length || source[index] !in charArrayOf('.', 'e', 'E')) {
            "Economy JSON accepts integer values only"
        }
        return source.substring(start, index).toLong()
    }

    private fun <T : JsonValue> parseLiteral(text: String, value: T): T {
        require(source.startsWith(text, index)) { "Expected $text at index $index" }
        index += text.length
        return value
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    private fun expect(expected: Char) {
        require(peek(expected)) { "Expected '$expected' at index $index" }
        index++
    }

    private fun peek(expected: Char): Boolean = index < source.length && source[index] == expected
}
