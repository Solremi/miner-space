package fr.solremi.minerspace.data.exploration

import fr.solremi.minerspace.domain.exploration.ExplorationDefinitions
import fr.solremi.minerspace.domain.exploration.SectorBounds
import fr.solremi.minerspace.domain.exploration.SectorDefinition
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class SectorContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): ExplorationDefinitions =
        parse(repository.readText(path) ?: error("Missing sector content: $path"))

    fun parse(content: String): ExplorationDefinitions {
        val root = JsonParser(content).parse().asObject("root")
        val sectors = linkedMapOf<GameId, SectorDefinition>()
        root.requireArray("sectors").values.forEachIndexed { index, value ->
            val item = value.asObject("sectors[$index]")
            val bounds = item.requireObject("bounds")
            val definition = SectorDefinition(
                id = GameId.of(item.requireString("id")),
                nameKey = item.requireString("nameKey"),
                strategicReason = item.requireString("strategicReason"),
                bounds = SectorBounds(
                    x = bounds.requireLong("x").toIntExact("x"),
                    y = bounds.requireLong("y").toIntExact("y"),
                    width = bounds.requireLong("width").toIntExact("width"),
                    height = bounds.requireLong("height").toIntExact("height"),
                ),
                unlockCostSpaceDollars = item.requireLong("unlockCostSpaceDollars"),
                scannerLevelRequired = item.requireLong("scannerLevelRequired").toIntExact("scannerLevelRequired"),
                requiredSectorIds = item.optionalStringSet("requiredSectorIds"),
                requiredTechnologyIds = item.optionalStringSet("requiredTechnologyIds"),
                requiredComponents = item.optionalQuantityMap("requiredComponents"),
                rareDepositId = item.optionalString("rareDepositId")?.let(GameId::of),
                missionTarget = item.optionalBoolean("missionTarget"),
                initiallyUnlocked = item.optionalBoolean("initiallyUnlocked"),
            )
            require(sectors.put(definition.id, definition) == null) { "Duplicate sector id: ${definition.id}" }
        }
        return ExplorationDefinitions(
            schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion"),
            contentVersion = root.requireString("contentVersion"),
            sectors = sectors,
        )
    }

    private companion object { const val DEFAULT_PATH = "data/sectors.json" }
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
private fun JsonValue.ObjectValue.optionalString(key: String): String? =
    (values[key] as? JsonValue.StringValue)?.value
private fun JsonValue.ObjectValue.requireLong(key: String): Long =
    (values[key] as? JsonValue.NumberValue)?.value ?: error("Missing integer: $key")
private fun JsonValue.ObjectValue.optionalBoolean(key: String): Boolean =
    (values[key] as? JsonValue.BooleanValue)?.value ?: false
private fun JsonValue.ObjectValue.requireObject(key: String): JsonValue.ObjectValue =
    values[key] as? JsonValue.ObjectValue ?: error("Missing object: $key")
private fun JsonValue.ObjectValue.requireArray(key: String): JsonValue.ArrayValue =
    values[key] as? JsonValue.ArrayValue ?: error("Missing array: $key")
private fun JsonValue.ObjectValue.optionalStringSet(key: String): Set<GameId> {
    val array = values[key] as? JsonValue.ArrayValue ?: return emptySet()
    return array.values.mapTo(linkedSetOf()) { GameId.of((it as? JsonValue.StringValue)?.value ?: error("Invalid id")) }
}
private fun JsonValue.ObjectValue.optionalQuantityMap(key: String): Map<GameId, Long> {
    val objectValue = values[key] as? JsonValue.ObjectValue ?: return emptyMap()
    return objectValue.values.map { (id, value) ->
        GameId.of(id) to ((value as? JsonValue.NumberValue)?.value ?: error("Invalid quantity: $id"))
    }.toMap()
}
private fun Long.toIntExact(label: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label outside Int range" }
    return toInt()
}

private class JsonParser(private val source: String) {
    private var index = 0
    fun parse(): JsonValue {
        skipWhitespace(); val value = parseValue(); skipWhitespace()
        require(index == source.length) { "Unexpected trailing JSON at index $index" }
        return value
    }
    private fun parseValue(): JsonValue {
        skipWhitespace(); require(index < source.length)
        return when (source[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.StringValue(parseString())
            't' -> parseLiteral("true", JsonValue.BooleanValue(true))
            'f' -> parseLiteral("false", JsonValue.BooleanValue(false))
            'n' -> parseLiteral("null", JsonValue.NullValue)
            '-', in '0'..'9' -> JsonValue.NumberValue(parseLong())
            else -> error("Unexpected token at $index")
        }
    }
    private fun parseObject(): JsonValue.ObjectValue {
        expect('{'); skipWhitespace(); val values = linkedMapOf<String, JsonValue>()
        if (peek('}')) { index++; return JsonValue.ObjectValue(values) }
        while (true) {
            skipWhitespace(); val key = parseString(); skipWhitespace(); expect(':')
            require(values.put(key, parseValue()) == null) { "Duplicate JSON key: $key" }
            skipWhitespace()
            when { peek(',') -> index++; peek('}') -> { index++; return JsonValue.ObjectValue(values) }; else -> error("Expected ',' or '}'") }
        }
    }
    private fun parseArray(): JsonValue.ArrayValue {
        expect('['); skipWhitespace(); val values = mutableListOf<JsonValue>()
        if (peek(']')) { index++; return JsonValue.ArrayValue(values) }
        while (true) {
            values += parseValue(); skipWhitespace()
            when { peek(',') -> index++; peek(']') -> { index++; return JsonValue.ArrayValue(values) }; else -> error("Expected ',' or ']'") }
        }
    }
    private fun parseString(): String {
        expect('"'); val output = StringBuilder()
        while (index < source.length) {
            when (val char = source[index++]) {
                '"' -> return output.toString()
                '\\' -> {
                    require(index < source.length)
                    when (val escaped = source[index++]) {
                        '"', '\\', '/' -> output.append(escaped)
                        'b' -> output.append('\b'); 'f' -> output.append('\u000C'); 'n' -> output.append('\n')
                        'r' -> output.append('\r'); 't' -> output.append('\t')
                        'u' -> output.append(parseUnicodeEscape())
                        else -> error("Unsupported escape: $escaped")
                    }
                }
                else -> output.append(char)
            }
        }
        error("Unterminated JSON string")
    }
    private fun parseUnicodeEscape(): Char {
        require(index + 4 <= source.length); val code = source.substring(index, index + 4).toInt(16); index += 4; return code.toChar()
    }
    private fun parseLong(): Long {
        val start = index; if (peek('-')) index++
        require(index < source.length && source[index].isDigit())
        while (index < source.length && source[index].isDigit()) index++
        require(index >= source.length || source[index] !in charArrayOf('.', 'e', 'E')) { "Sector JSON accepts integers only" }
        return source.substring(start, index).toLong()
    }
    private fun <T : JsonValue> parseLiteral(text: String, value: T): T {
        require(source.startsWith(text, index)); index += text.length; return value
    }
    private fun skipWhitespace() { while (index < source.length && source[index].isWhitespace()) index++ }
    private fun expect(expected: Char) { require(peek(expected)); index++ }
    private fun peek(expected: Char): Boolean = index < source.length && source[index] == expected
}
