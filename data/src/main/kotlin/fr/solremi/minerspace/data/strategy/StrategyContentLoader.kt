package fr.solremi.minerspace.data.strategy

import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.domain.strategy.*
import fr.solremi.minerspace.shared.GameId

class StrategyContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): StrategyDefinitions =
        parse(repository.readText(path) ?: error("Missing strategy content: $path"))

    fun parse(content: String): StrategyDefinitions {
        val root = Parser(content).parse().obj("root")
        val specializations = root.array("specializations").mapIndexed { index, value ->
            val item = value.obj("specializations[$index]")
            val id = SpecializationId.valueOf(item.string("id"))
            id to SpecializationDefinition(
                id = id,
                nameKey = item.string("nameKey"),
                bonuses = item.bonuses(),
                changeCostSpaceDollars = item.long("changeCostSpaceDollars"),
                cooldownSeconds = item.long("cooldownSeconds"),
            )
        }.toMap(linkedMapOf())
        val modules = root.array("modules").mapIndexed { index, value ->
            val item = value.obj("modules[$index]")
            val id = GameId.of(item.string("id"))
            id to ModuleDefinition(
                id = id,
                nameKey = item.string("nameKey"),
                setId = ModuleSetId.valueOf(item.string("setId")),
                baseBonuses = item.bonuses(),
                craftInputs = item.objectValue("craftInputs").values.map { (key, amount) ->
                    GameId.of(key) to amount.num("craftInputs.$key")
                }.toMap(),
                craftCostSpaceDollars = item.long("craftCostSpaceDollars"),
                upgradeCostsSpaceDollars = item.array("upgradeCostsSpaceDollars").mapIndexed { i, v -> v.num("upgradeCostsSpaceDollars[$i]") },
                maxLevel = item.long("maxLevel").toIntExact("maxLevel"),
            )
        }.toMap(linkedMapOf())
        val synergies = root.array("synergies").mapIndexed { index, value ->
            val item = value.obj("synergies[$index]")
            SynergyDefinition(
                setId = ModuleSetId.valueOf(item.string("setId")),
                requiredPieces = item.long("requiredPieces").toIntExact("requiredPieces"),
                bonuses = item.bonuses(),
            )
        }
        return StrategyDefinitions(
            schemaVersion = root.long("schemaVersion").toIntExact("schemaVersion"),
            contentVersion = root.string("contentVersion"),
            specializations = specializations,
            modules = modules,
            synergies = synergies,
        )
    }

    private fun Obj.bonuses(): StrategyBonuses {
        val b = objectValue("bonuses")
        fun n(key: String) = (b.values[key] as? Num)?.value ?: 0L
        return StrategyBonuses(n("extraction"), n("refiningSpeed"), n("assemblySpeed"), n("logistics"), n("storage"), n("rareFind"))
    }

    companion object { const val DEFAULT_PATH = "data/specializations-modules.json" }
}

private sealed interface J
private data class Obj(val values: Map<String, J>) : J
private data class Arr(val values: List<J>) : J
private data class Str(val value: String) : J
private data class Num(val value: Long) : J
private data class Bool(val value: Boolean) : J
private data object Null : J

private fun J.obj(where: String) = this as? Obj ?: error("Expected object at $where")
private fun J.num(where: String) = (this as? Num)?.value ?: error("Expected integer at $where")
private fun Obj.string(key: String) = (values[key] as? Str)?.value ?: error("Missing string: $key")
private fun Obj.long(key: String) = (values[key] as? Num)?.value ?: error("Missing integer: $key")
private fun Obj.array(key: String) = (values[key] as? Arr)?.values ?: error("Missing array: $key")
private fun Obj.objectValue(key: String) = values[key] as? Obj ?: error("Missing object: $key")
private fun Long.toIntExact(label: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label outside Int range" }
    return toInt()
}

private class Parser(private val source: String) {
    private var i = 0
    fun parse(): J { ws(); val value = value(); ws(); require(i == source.length); return value }
    private fun value(): J {
        ws(); require(i < source.length)
        return when (source[i]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> Str(string())
            't' -> literal("true", Bool(true))
            'f' -> literal("false", Bool(false))
            'n' -> literal("null", Null)
            '-', in '0'..'9' -> Num(number())
            else -> error("Unexpected token at $i")
        }
    }
    private fun obj(): Obj {
        expect('{'); ws(); val out = linkedMapOf<String, J>()
        if (peek('}')) { i++; return Obj(out) }
        while (true) {
            ws(); val key = string(); ws(); expect(':'); require(out.put(key, value()) == null)
            ws(); when { peek(',') -> i++; peek('}') -> { i++; return Obj(out) }; else -> error("Expected object delimiter at $i") }
        }
    }
    private fun arr(): Arr {
        expect('['); ws(); val out = mutableListOf<J>()
        if (peek(']')) { i++; return Arr(out) }
        while (true) {
            out += value(); ws(); when { peek(',') -> i++; peek(']') -> { i++; return Arr(out) }; else -> error("Expected array delimiter at $i") }
        }
    }
    private fun string(): String {
        expect('"'); val out = StringBuilder()
        while (i < source.length) {
            when (val c = source[i++]) {
                '"' -> return out.toString()
                '\\' -> {
                    require(i < source.length)
                    when (val e = source[i++]) {
                        '"', '\\', '/' -> out.append(e)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> { require(i + 4 <= source.length); out.append(source.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> error("Unsupported escape")
                    }
                }
                else -> out.append(c)
            }
        }
        error("Unterminated string")
    }
    private fun number(): Long {
        val start = i
        if (peek('-')) i++
        require(i < source.length && source[i].isDigit())
        while (i < source.length && source[i].isDigit()) i++
        require(i >= source.length || source[i] !in charArrayOf('.', 'e', 'E'))
        return source.substring(start, i).toLong()
    }
    private fun <T : J> literal(text: String, value: T): T { require(source.startsWith(text, i)); i += text.length; return value }
    private fun ws() { while (i < source.length && source[i].isWhitespace()) i++ }
    private fun expect(c: Char) { require(peek(c)); i++ }
    private fun peek(c: Char) = i < source.length && source[i] == c
}
