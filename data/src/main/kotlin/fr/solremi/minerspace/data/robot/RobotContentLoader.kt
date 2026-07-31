package fr.solremi.minerspace.data.robot

import fr.solremi.minerspace.domain.robot.MasteryTier
import fr.solremi.minerspace.domain.robot.RenderQuality
import fr.solremi.minerspace.domain.robot.RobotDefinitions
import fr.solremi.minerspace.domain.robot.RobotFamily
import fr.solremi.minerspace.domain.robot.RobotFamilyDefinition
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.domain.services.ContentRepository

class RobotContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): RobotDefinitions {
        val content = repository.readText(path) ?: error("Missing robot content: $path")
        return parse(content)
    }

    fun parse(content: String): RobotDefinitions {
        val root = JsonParser(content).parse().asObject("root")
        val families = linkedMapOf<RobotFamily, RobotFamilyDefinition>()
        root.requireArray("families").values.forEachIndexed { index, value ->
            val item = value.asObject("families[$index]")
            val family = RobotFamily.valueOf(item.requireString("family"))
            val definition = RobotFamilyDefinition(
                family = family,
                nameKey = item.requireString("nameKey"),
                defaultName = item.requireString("defaultName"),
                serialPrefix = item.requireString("serialPrefix"),
                defaultTrait = RobotTrait.valueOf(item.requireString("defaultTrait")),
                maxLevel = item.requireLong("maxLevel").toIntExact("maxLevel"),
                baseLogisticsPerSecond = item.requireLong("baseLogisticsPerSecond"),
                upgradeCostsSpaceDollars = item.requireLongList("upgradeCostsSpaceDollars"),
            )
            require(families.put(family, definition) == null) { "Duplicate robot family: $family" }
        }
        val mastery = root.requireObject("masteryThresholds").values.map { (key, value) ->
            MasteryTier.valueOf(key) to value.asLong("masteryThresholds.$key")
        }.toMap()
        val visible = root.requireObject("visibleUnitsByQuality").values.map { (key, value) ->
            RenderQuality.valueOf(key) to value.asLong("visibleUnitsByQuality.$key").toIntExact(key)
        }.toMap()
        return RobotDefinitions(
            schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion"),
            contentVersion = root.requireString("contentVersion"),
            families = families,
            masteryThresholds = mastery,
            visibleUnitsByQuality = visible,
        )
    }

    private companion object {
        const val DEFAULT_PATH = "data/robots.json"
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

private fun JsonValue.asLong(location: String): Long =
    (this as? JsonValue.NumberValue)?.value ?: error("Expected integer at $location")

private fun JsonValue.ObjectValue.requireString(key: String): String =
    (values[key] as? JsonValue.StringValue)?.value ?: error("Missing string: $key")

private fun JsonValue.ObjectValue.requireLong(key: String): Long =
    (values[key] as? JsonValue.NumberValue)?.value ?: error("Missing integer: $key")

private fun JsonValue.ObjectValue.requireObject(key: String): JsonValue.ObjectValue =
    values[key] as? JsonValue.ObjectValue ?: error("Missing object: $key")

private fun JsonValue.ObjectValue.requireArray(key: String): JsonValue.ArrayValue =
    values[key] as? JsonValue.ArrayValue ?: error("Missing array: $key")

private fun JsonValue.ObjectValue.requireLongList(key: String): List<Long> =
    requireArray(key).values.mapIndexed { index, value -> value.asLong("$key[$index]") }

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
        if (peek('}')) {
            index++
            return JsonValue.ObjectValue(values)
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            require(values.put(key, parseValue()) == null) { "Duplicate JSON key: $key" }
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
            "Robot JSON accepts integer values only"
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
