package fr.solremi.minerspace.data.progression

import fr.solremi.minerspace.domain.progression.*
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class ProgressionContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): ProgressionDefinitions =
        parse(repository.readText(path) ?: error("Missing progression content: $path"))

    fun parse(content: String): ProgressionDefinitions {
        val root = Parser(content).parse().obj("root")
        val tutorials = root.array("tutorial").mapIndexed { index, value ->
            val item = value.obj("tutorial[$index]")
            TutorialStepDefinition(GameId.of(item.string("id")), item.string("phaseLabel"), item.string("titleKey"), item.string("actionKey"), ProgressMetric.valueOf(item.string("metric")), item.long("target"))
        }
        val missions = root.array("missions").mapIndexed { index, value ->
            val item = value.obj("missions[$index]"); val id = GameId.of(item.string("id"))
            id to MissionDefinition(id, MissionKind.valueOf(item.string("kind")), item.string("titleKey"), ProgressMetric.valueOf(item.string("metric")), item.long("target"), item.long("rewardSpaceDollars"), item.ids("requiredMissionIds"))
        }.toMap(linkedMapOf())
        val contracts = root.array("contracts").mapIndexed { index, value ->
            val item = value.obj("contracts[$index]")
            ContractDefinition(GameId.of(item.string("id")), ContractTier.valueOf(item.string("tier")), item.string("titleKey"), GameId.of(item.string("resourceId")), item.long("quantity"), item.long("rewardSpaceDollars"), item.ids("requiredMissionIds"))
        }
        val entries = root.array("codexEntries").mapIndexed { index, value ->
            val item = value.obj("codexEntries[$index]"); val id = GameId.of(item.string("id"))
            id to CodexEntryDefinition(id, CodexCategory.valueOf(item.string("category")), item.string("titleKey"), ProgressMetric.valueOf(item.string("metric")), item.long("target"), item.ids("requiredMissionIds"), item.optionalString("collectionId")?.let(GameId::of))
        }.toMap(linkedMapOf())
        val collections = root.array("collections").mapIndexed { index, value ->
            val item = value.obj("collections[$index]"); val id = GameId.of(item.string("id"))
            id to CollectionDefinition(id, item.string("titleKey"), item.ids("entryIds"), item.long("rewardSpaceDollars"))
        }.toMap(linkedMapOf())
        return ProgressionDefinitions(root.long("schemaVersion").toIntExact("schemaVersion"), root.string("contentVersion"), tutorials, missions, contracts, entries, collections)
    }

    companion object { const val DEFAULT_PATH = "data/progression.json" }
}

private sealed interface J {
    data class O(val values: Map<String, J>) : J
    data class A(val values: List<J>) : J
    data class S(val value: String) : J
    data class N(val value: Long) : J
    data class B(val value: Boolean) : J
    data object Z : J
}
private fun J.obj(where: String) = this as? J.O ?: error("Expected object at $where")
private fun J.O.string(key: String) = (values[key] as? J.S)?.value ?: error("Missing string: $key")
private fun J.O.optionalString(key: String) = (values[key] as? J.S)?.value
private fun J.O.long(key: String) = (values[key] as? J.N)?.value ?: error("Missing integer: $key")
private fun J.O.array(key: String) = (values[key] as? J.A)?.values ?: error("Missing array: $key")
private fun J.O.ids(key: String): Set<GameId> = (values[key] as? J.A)?.values?.mapTo(linkedSetOf()) { GameId.of((it as? J.S)?.value ?: error("Invalid id in $key")) } ?: emptySet()
private fun Long.toIntExact(label: String): Int { require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$label outside Int range" }; return toInt() }

private class Parser(private val source: String) {
    private var i = 0
    fun parse(): J { ws(); val value = value(); ws(); require(i == source.length); return value }
    private fun value(): J { ws(); require(i < source.length); return when (source[i]) {
        '{' -> obj(); '[' -> array(); '"' -> J.S(string()); 't' -> literal("true", J.B(true)); 'f' -> literal("false", J.B(false)); 'n' -> literal("null", J.Z); '-', in '0'..'9' -> J.N(number()); else -> error("Unexpected token at $i")
    } }
    private fun obj(): J.O { expect('{'); ws(); val map = linkedMapOf<String,J>(); if (peek('}')) { i++; return J.O(map) }; while (true) { ws(); val key=string(); ws(); expect(':'); require(map.put(key,value())==null); ws(); when { peek(',')->i++; peek('}')->{i++;return J.O(map)}; else->error("Expected object separator at $i") } } }
    private fun array(): J.A { expect('['); ws(); val list=mutableListOf<J>(); if(peek(']')){i++;return J.A(list)}; while(true){list+=value();ws();when{peek(',')->i++;peek(']')->{i++;return J.A(list)};else->error("Expected array separator at $i")}} }
    private fun string(): String { expect('"'); val out=StringBuilder(); while(i<source.length){val c=source[i++];when(c){'"'->return out.toString();'\\'->{require(i<source.length);when(val e=source[i++]){'"','\\','/'->out.append(e);'b'->out.append('\b');'f'->out.append('\u000C');'n'->out.append('\n');'r'->out.append('\r');'t'->out.append('\t');'u'->{require(i+4<=source.length);out.append(source.substring(i,i+4).toInt(16).toChar());i+=4};else->error("Escape")}};else->out.append(c)}};error("Unterminated string") }
    private fun number(): Long { val start=i;if(peek('-'))i++;require(i<source.length&&source[i].isDigit());while(i<source.length&&source[i].isDigit())i++;require(i>=source.length||source[i] !in charArrayOf('.','e','E'));return source.substring(start,i).toLong() }
    private fun <T:J> literal(text:String,result:T):T{require(source.startsWith(text,i));i+=text.length;return result}
    private fun ws(){while(i<source.length&&source[i].isWhitespace())i++}
    private fun expect(c:Char){require(peek(c)){"Expected $c at $i"};i++}
    private fun peek(c:Char)=i<source.length&&source[i]==c
}
