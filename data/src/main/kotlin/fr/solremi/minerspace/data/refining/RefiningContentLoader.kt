package fr.solremi.minerspace.data.refining

import fr.solremi.minerspace.domain.refining.RecipeDefinition
import fr.solremi.minerspace.domain.refining.RefinerRobotDefinition
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class RefiningContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): RefiningDefinitions =
        parse(repository.readText(path) ?: error("Missing refining content: $path"))

    fun parse(content: String): RefiningDefinitions {
        val root = Parser(content).parse().objectValue("root")
        val schemaVersion = root.long("schemaVersion").toIntExact("schemaVersion")
        val contentVersion = root.string("contentVersion")
        val robotObject = root.objectField("robot")
        val robot = RefinerRobotDefinition(
            id = GameId.of(robotObject.string("id")),
            nameKey = robotObject.string("nameKey"),
            queueCapacity = robotObject.long("queueCapacity").toIntExact("queueCapacity"),
        )
        val recipes = linkedMapOf<GameId, RecipeDefinition>()
        root.array("recipes").values.forEachIndexed { index, value ->
            val item = value.objectValue("recipes[$index]")
            val inputs = linkedMapOf<GameId, Long>()
            item.objectField("inputs").values.forEach { (key, inputValue) ->
                val quantity = (inputValue as? Value.NumberValue)?.value
                    ?: error("Recipe input $key must be an integer")
                require(quantity > 0L) { "Recipe input $key must be positive" }
                inputs[GameId.of(key)] = quantity
            }
            val definition = RecipeDefinition(
                id = GameId.of(item.string("id")),
                nameKey = item.string("nameKey"),
                inputs = inputs,
                outputResourceId = GameId.of(item.string("outputResourceId")),
                outputQuantity = item.long("outputQuantity"),
                durationSeconds = item.long("durationSeconds"),
            )
            require(recipes.put(definition.id, definition) == null) {
                "Duplicate recipe id: ${definition.id}"
            }
        }
        return RefiningDefinitions(schemaVersion, contentVersion, robot, recipes)
    }

    private companion object {
        const val DEFAULT_PATH = "data/refining.json"
    }
}

private sealed interface Value {
    data class ObjectValue(val values: Map<String, Value>) : Value
    data class ArrayValue(val values: List<Value>) : Value
    data class StringValue(val value: String) : Value
    data class NumberValue(val value: Long) : Value
    data class BooleanValue(val value: Boolean) : Value
    data object NullValue : Value
}

private fun Value.objectValue(location: String): Value.ObjectValue =
    this as? Value.ObjectValue ?: error("Expected object at $location")

private fun Value.ObjectValue.string(key: String): String =
    (values[key] as? Value.StringValue)?.value ?: error("Missing string: $key")

private fun Value.ObjectValue.long(key: String): Long =
    (values[key] as? Value.NumberValue)?.value ?: error("Missing integer: $key")

private fun Value.ObjectValue.objectField(key: String): Value.ObjectValue =
    values[key] as? Value.ObjectValue ?: error("Missing object: $key")

private fun Value.ObjectValue.array(key: String): Value.ArrayValue =
    values[key] as? Value.ArrayValue ?: error("Missing array: $key")

private fun Long.toIntExact(label: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label is outside Int range" }
    return toInt()
}

private class Parser(private val source: String) {
    private var index = 0

    fun parse(): Value {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        require(index == source.length) { "Unexpected trailing JSON at index $index" }
        return value
    }

    private fun parseValue(): Value {
        skipWhitespace()
        require(index < source.length) { "Unexpected end of JSON" }
        return when (source[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> Value.StringValue(parseString())
            't' -> literal("true", Value.BooleanValue(true))
            'f' -> literal("false", Value.BooleanValue(false))
            'n' -> literal("null", Value.NullValue)
            '-', in '0'..'9' -> Value.NumberValue(parseLong())
            else -> error("Unexpected token '${source[index]}' at $index")
        }
    }

    private fun parseObject(): Value.ObjectValue {
        expect('{')
        skipWhitespace()
        val values = linkedMapOf<String, Value>()
        if (peek('}')) {
            index++
            return Value.ObjectValue(values)
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
                    return Value.ObjectValue(values)
                }
                else -> error("Expected ',' or '}' at $index")
            }
        }
    }

    private fun parseArray(): Value.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<Value>()
        if (peek(']')) {
            index++
            return Value.ArrayValue(values)
        }
        while (true) {
            values += parseValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
                    return Value.ArrayValue(values)
                }
                else -> error("Expected ',' or ']' at $index")
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
                    require(index < source.length) { "Unterminated escape" }
                    when (val escaped = source[index++]) {
                        '"', '\\', '/' -> output.append(escaped)
                        'b' -> output.append('\b')
                        'f' -> output.append('\u000C')
                        'n' -> output.append('\n')
                        'r' -> output.append('\r')
                        't' -> output.append('\t')
                        'u' -> output.append(parseUnicode())
                        else -> error("Unsupported escape: \\$escaped")
                    }
                }
                else -> output.append(char)
            }
        }
        error("Unterminated JSON string")
    }

    private fun parseUnicode(): Char {
        require(index + 4 <= source.length) { "Invalid unicode escape" }
        val value = source.substring(index, index + 4).toInt(16)
        index += 4
        return value.toChar()
    }

    private fun parseLong(): Long {
        val start = index
        if (peek('-')) index++
        require(index < source.length && source[index].isDigit()) { "Invalid number at $index" }
        while (index < source.length && source[index].isDigit()) index++
        require(index >= source.length || source[index] !in charArrayOf('.', 'e', 'E')) {
            "Refining JSON accepts integer values only"
        }
        return source.substring(start, index).toLong()
    }

    private fun <T : Value> literal(text: String, value: T): T {
        require(source.startsWith(text, index)) { "Expected $text at $index" }
        index += text.length
        return value
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    private fun expect(expected: Char) {
        require(peek(expected)) { "Expected '$expected' at $index" }
        index++
    }

    private fun peek(expected: Char): Boolean = index < source.length && source[index] == expected
}
