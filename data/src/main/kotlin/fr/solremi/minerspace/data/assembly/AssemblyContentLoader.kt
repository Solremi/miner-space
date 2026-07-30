package fr.solremi.minerspace.data.assembly

import fr.solremi.minerspace.domain.assembly.AssemblerRobotDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.domain.assembly.AssemblyRecipeDefinition
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class AssemblyContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): AssemblyDefinitions {
        val content = repository.readText(path) ?: error("Missing assembly content: $path")
        return parse(content)
    }

    fun parse(content: String): AssemblyDefinitions {
        val root = JsonParser(content).parse().asObject("root")
        val robotObject = root.requireObject("robot")
        val robot = AssemblerRobotDefinition(
            id = GameId.of(robotObject.requireString("id")),
            nameKey = robotObject.requireString("nameKey"),
            queueCapacity = robotObject.requireLong("queueCapacity").toIntExact("queueCapacity"),
        )
        val recipes = linkedMapOf<GameId, AssemblyRecipeDefinition>()
        root.requireArray("recipes").values.forEachIndexed { index, value ->
            val item = value.asObject("recipes[$index]")
            val definition = AssemblyRecipeDefinition(
                id = GameId.of(item.requireString("id")),
                nameKey = item.requireString("nameKey"),
                inputs = item.requireQuantityMap("inputs"),
                outputResourceId = GameId.of(item.requireString("outputResourceId")),
                outputQuantity = item.requireLong("outputQuantity"),
                durationSeconds = item.requireLong("durationSeconds"),
                outputKind = AssemblyOutputKind.valueOf(item.requireString("outputKind")),
                requiredTechnologyIds = item.optionalStringSet("requiredTechnologyIds"),
            )
            require(recipes.put(definition.id, definition) == null) {
                "Duplicate assembly recipe id: ${definition.id}"
            }
        }
        val technologies = linkedMapOf<GameId, TechnologyDefinition>()
        root.requireArray("technologies").values.forEachIndexed { index, value ->
            val item = value.asObject("technologies[$index]")
            val definition = TechnologyDefinition(
                id = GameId.of(item.requireString("id")),
                nameKey = item.requireString("nameKey"),
                itemResourceId = GameId.of(item.requireString("itemResourceId")),
                requiredTechnologyIds = item.optionalStringSet("requiredTechnologyIds"),
                extractionBonusMillionths = item.requireLong("extractionBonusMillionths"),
            )
            require(technologies.put(definition.id, definition) == null) {
                "Duplicate technology id: ${definition.id}"
            }
        }
        return AssemblyDefinitions(
            schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion"),
            contentVersion = root.requireString("contentVersion"),
            robot = robot,
            recipes = recipes,
            technologies = technologies,
        )
    }

    private companion object {
        const val DEFAULT_PATH = "data/assembly.json"
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

private fun JsonValue.ObjectValue.requireObject(key: String): JsonValue.ObjectValue =
    values[key] as? JsonValue.ObjectValue ?: error("Missing object: $key")

private fun JsonValue.ObjectValue.requireArray(key: String): JsonValue.ArrayValue =
    values[key] as? JsonValue.ArrayValue ?: error("Missing array: $key")

private fun JsonValue.ObjectValue.requireQuantityMap(key: String): Map<GameId, Long> =
    requireObject(key).values.map { (id, value) ->
        GameId.of(id) to ((value as? JsonValue.NumberValue)?.value ?: error("Invalid quantity: $id"))
    }.toMap()

private fun JsonValue.ObjectValue.optionalStringSet(key: String): Set<GameId> {
    val array = values[key] as? JsonValue.ArrayValue ?: return emptySet()
    return array.values.mapTo(linkedSetOf()) { value ->
        GameId.of((value as? JsonValue.StringValue)?.value ?: error("Invalid id in $key"))
    }
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
            "Assembly JSON accepts integer values only"
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
