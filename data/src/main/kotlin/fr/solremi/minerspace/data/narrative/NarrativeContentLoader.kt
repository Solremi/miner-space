package fr.solremi.minerspace.data.narrative

import fr.solremi.minerspace.domain.narrative.NarrativeChapterDefinition
import fr.solremi.minerspace.domain.narrative.NarrativeChapterKind
import fr.solremi.minerspace.domain.narrative.NarrativeDefinitions
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class NarrativeContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): NarrativeDefinitions =
        parse(repository.readText(path) ?: error("Missing narrative content: $path"))

    fun parse(content: String): NarrativeDefinitions {
        val root = JsonParser(content).parse().asObject("root")
        val chaptersArray = root.requireArray("chapters")
        val chapters = chaptersArray.values.mapIndexed { index, value ->
            val item = value.asObject("chapters[$index]")
            val id = GameId.of(item.requireString("id"))
            id to NarrativeChapterDefinition(
                id = id,
                kind = NarrativeChapterKind.valueOf(item.requireString("kind")),
                title = item.requireString("title"),
                transmission = item.requireString("transmission"),
                archiveSummary = item.requireString("archiveSummary"),
                requiredUnlockedSectors = item.requireLong("requiredUnlockedSectors").toIntExact("requiredUnlockedSectors"),
                requiredTechnologies = item.requireLong("requiredTechnologies").toIntExact("requiredTechnologies"),
                requiredResolvedChapterIds = item.requireStringSet("requiredResolvedChapterIds"),
                anomalyChancePercent = item.requireLong("anomalyChancePercent").toIntExact("anomalyChancePercent"),
                pityAttempts = item.requireLong("pityAttempts").toIntExact("pityAttempts"),
                deterministicSeed = item.requireLong("deterministicSeed").toIntExact("deterministicSeed"),
                rareResourceId = item.optionalString("rareResourceId")?.let(GameId::of),
                grantsVeteranRobot = item.requireBoolean("grantsVeteranRobot"),
            )
        }.toMap(linkedMapOf())
        require(chapters.size == chaptersArray.values.size) { "Duplicate narrative chapter" }
        return NarrativeDefinitions(
            schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion"),
            contentVersion = root.requireString("contentVersion"),
            veteranMasteryPoints = root.requireLong("veteranMasteryPoints"),
            veteranRobotId = GameId.of(root.requireString("veteranRobotId")),
            chapters = chapters,
        )
    }

    private companion object { const val DEFAULT_PATH = "data/narrative.json" }
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

private fun JsonValue.ObjectValue.optionalString(key: String): String? = when (val value = values[key]) {
    null, JsonValue.NullValue -> null
    is JsonValue.StringValue -> value.value
    else -> error("Expected string or null: $key")
}

private fun JsonValue.ObjectValue.requireLong(key: String): Long =
    (values[key] as? JsonValue.NumberValue)?.value ?: error("Missing integer: $key")

private fun JsonValue.ObjectValue.requireBoolean(key: String): Boolean =
    (values[key] as? JsonValue.BooleanValue)?.value ?: error("Missing boolean: $key")

private fun JsonValue.ObjectValue.requireArray(key: String): JsonValue.ArrayValue =
    values[key] as? JsonValue.ArrayValue ?: error("Missing array: $key")

private fun JsonValue.ObjectValue.requireStringSet(key: String): Set<GameId> =
    requireArray(key).values.mapIndexedTo(linkedSetOf()) { index, value ->
        val string = (value as? JsonValue.StringValue)?.value ?: error("Expected string at $key[$index]")
        GameId.of(string)
    }

private fun Long.toIntExact(label: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label outside Int range" }
    return toInt()
}

private class JsonParser(private val source: String) {
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
        if (peek('}')) { index++; return JsonValue.ObjectValue(values) }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace(); expect(':')
            require(values.put(key, parseValue()) == null) { "Duplicate JSON key: $key" }
            skipWhitespace()
            when {
                peek(',') -> index++
                peek('}') -> { index++; return JsonValue.ObjectValue(values) }
                else -> error("Expected ',' or '}' at index $index")
            }
        }
    }

    private fun parseArray(): JsonValue.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<JsonValue>()
        if (peek(']')) { index++; return JsonValue.ArrayValue(values) }
        while (true) {
            values += parseValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek(']') -> { index++; return JsonValue.ArrayValue(values) }
                else -> error("Expected ',' or ']' at index $index")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val output = StringBuilder()
        while (index < source.length) {
            when (val char = source[index++]) {
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
        require(index + 4 <= source.length) { "Incomplete unicode escape" }
        val value = source.substring(index, index + 4).toInt(16)
        index += 4
        return value.toChar()
    }

    private fun parseLong(): Long {
        val start = index
        if (peek('-')) index++
        require(index < source.length && source[index].isDigit()) { "Invalid JSON number" }
        if (source[index] == '0') index++ else while (index < source.length && source[index].isDigit()) index++
        require(index >= source.length || source[index] !in listOf('.', 'e', 'E')) { "Decimal numbers are not supported" }
        return source.substring(start, index).toLong()
    }

    private fun parseLiteral(text: String, value: JsonValue): JsonValue {
        require(source.startsWith(text, index)) { "Invalid JSON literal" }
        index += text.length
        return value
    }

    private fun skipWhitespace() { while (index < source.length && source[index].isWhitespace()) index++ }
    private fun expect(char: Char) { require(index < source.length && source[index] == char) { "Expected '$char' at index $index" }; index++ }
    private fun peek(char: Char): Boolean = index < source.length && source[index] == char
}
