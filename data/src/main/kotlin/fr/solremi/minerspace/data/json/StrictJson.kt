package fr.solremi.minerspace.data.json

sealed interface StrictJsonValue {
    data class ObjectValue(val values: Map<String, StrictJsonValue>) : StrictJsonValue
    data class ArrayValue(val values: List<StrictJsonValue>) : StrictJsonValue
    data class StringValue(val value: String) : StrictJsonValue
    data class NumberValue(val value: Long) : StrictJsonValue
    data class BooleanValue(val value: Boolean) : StrictJsonValue
    data object NullValue : StrictJsonValue
}

object StrictJson {
    fun parse(source: String): StrictJsonValue = Parser(source).parse()
}

fun StrictJsonValue.requireObject(location: String): StrictJsonValue.ObjectValue =
    this as? StrictJsonValue.ObjectValue ?: error("Expected object at $location")

fun StrictJsonValue.ObjectValue.requireString(key: String): String =
    (values[key] as? StrictJsonValue.StringValue)?.value ?: error("Missing string: $key")

fun StrictJsonValue.ObjectValue.optionalString(key: String): String? =
    when (val value = values[key]) {
        null, StrictJsonValue.NullValue -> null
        is StrictJsonValue.StringValue -> value.value
        else -> error("Expected optional string: $key")
    }

fun StrictJsonValue.ObjectValue.requireLong(key: String): Long =
    (values[key] as? StrictJsonValue.NumberValue)?.value ?: error("Missing integer: $key")

fun StrictJsonValue.ObjectValue.optionalLong(key: String): Long? =
    when (val value = values[key]) {
        null, StrictJsonValue.NullValue -> null
        is StrictJsonValue.NumberValue -> value.value
        else -> error("Expected optional integer: $key")
    }

fun StrictJsonValue.ObjectValue.optionalBoolean(key: String): Boolean? =
    when (val value = values[key]) {
        null, StrictJsonValue.NullValue -> null
        is StrictJsonValue.BooleanValue -> value.value
        else -> error("Expected optional boolean: $key")
    }

fun StrictJsonValue.ObjectValue.requireArray(key: String): List<StrictJsonValue> =
    (values[key] as? StrictJsonValue.ArrayValue)?.values ?: error("Missing array: $key")

fun StrictJsonValue.ObjectValue.optionalArray(key: String): List<StrictJsonValue> =
    when (val value = values[key]) {
        null, StrictJsonValue.NullValue -> emptyList()
        is StrictJsonValue.ArrayValue -> value.values
        else -> error("Expected optional array: $key")
    }

fun StrictJsonValue.ObjectValue.requireKnownKeys(
    location: String,
    vararg allowed: String,
) {
    val unknown = values.keys - allowed.toSet()
    require(unknown.isEmpty()) { "Unknown keys at $location: $unknown" }
}

fun Long.toIntExact(label: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
        "$label is outside Int range"
    }
    return toInt()
}

private class Parser(
    private val source: String,
) {
    private var index = 0

    fun parse(): StrictJsonValue {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        require(index == source.length) { "Unexpected trailing JSON at index $index" }
        return value
    }

    private fun parseValue(): StrictJsonValue {
        skipWhitespace()
        require(index < source.length) { "Unexpected end of JSON" }
        return when (source[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> StrictJsonValue.StringValue(parseString())
            't' -> parseLiteral("true", StrictJsonValue.BooleanValue(true))
            'f' -> parseLiteral("false", StrictJsonValue.BooleanValue(false))
            'n' -> parseLiteral("null", StrictJsonValue.NullValue)
            '-', in '0'..'9' -> StrictJsonValue.NumberValue(parseLong())
            else -> error("Unexpected JSON token '${source[index]}' at index $index")
        }
    }

    private fun parseObject(): StrictJsonValue.ObjectValue {
        expect('{')
        skipWhitespace()
        val values = linkedMapOf<String, StrictJsonValue>()
        if (peek('}')) {
            index++
            return StrictJsonValue.ObjectValue(values)
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
                    return StrictJsonValue.ObjectValue(values)
                }
                else -> error("Expected ',' or '}' at index $index")
            }
        }
    }

    private fun parseArray(): StrictJsonValue.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<StrictJsonValue>()
        if (peek(']')) {
            index++
            return StrictJsonValue.ArrayValue(values)
        }
        while (true) {
            values += parseValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
                    return StrictJsonValue.ArrayValue(values)
                }
                else -> error("Expected ',' or ']' at index $index")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val output = StringBuilder()
        while (index < source.length) {
            val character = source[index++]
            when (character) {
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
                else -> {
                    require(character.code >= 0x20) { "Unescaped control character in JSON string" }
                    output.append(character)
                }
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
        require(index < source.length && source[index].isDigit()) {
            "Invalid number at index $index"
        }
        while (index < source.length && source[index].isDigit()) index++
        require(index >= source.length || source[index] !in charArrayOf('.', 'e', 'E')) {
            "Strict JSON content accepts integer values only"
        }
        return source.substring(start, index).toLong()
    }

    private fun <T : StrictJsonValue> parseLiteral(text: String, value: T): T {
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

    private fun peek(expected: Char): Boolean =
        index < source.length && source[index] == expected
}
