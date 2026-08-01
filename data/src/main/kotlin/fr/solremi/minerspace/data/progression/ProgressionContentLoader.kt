package fr.solremi.minerspace.data.progression

import fr.solremi.minerspace.data.json.StrictJson
import fr.solremi.minerspace.data.json.StrictJsonValue
import fr.solremi.minerspace.data.json.optionalArray
import fr.solremi.minerspace.data.json.optionalString
import fr.solremi.minerspace.data.json.requireArray
import fr.solremi.minerspace.data.json.requireKnownKeys
import fr.solremi.minerspace.data.json.requireLong
import fr.solremi.minerspace.data.json.requireObject
import fr.solremi.minerspace.data.json.requireString
import fr.solremi.minerspace.data.json.toIntExact
import fr.solremi.minerspace.domain.progression.CodexCategory
import fr.solremi.minerspace.domain.progression.CodexEntryDefinition
import fr.solremi.minerspace.domain.progression.CollectionDefinition
import fr.solremi.minerspace.domain.progression.ContractDefinition
import fr.solremi.minerspace.domain.progression.ContractTier
import fr.solremi.minerspace.domain.progression.MissionDefinition
import fr.solremi.minerspace.domain.progression.MissionKind
import fr.solremi.minerspace.domain.progression.ProgressMetric
import fr.solremi.minerspace.domain.progression.ProgressionDefinitions
import fr.solremi.minerspace.domain.progression.TutorialStepDefinition
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId

class ProgressionContentLoader {
    fun load(repository: ContentRepository, path: String = DEFAULT_PATH): ProgressionDefinitions =
        parse(repository.readText(path) ?: error("Missing progression content: $path"))

    fun parse(content: String): ProgressionDefinitions {
        val root = StrictJson.parse(content).requireObject("root")
        root.requireKnownKeys(
            "root",
            "schemaVersion",
            "contentVersion",
            "tutorial",
            "missions",
            "contracts",
            "codexEntries",
            "collections",
        )
        val tutorials = root.requireArray("tutorial").mapIndexed { index, value ->
            val location = "tutorial[$index]"
            val item = value.requireObject(location)
            item.requireKnownKeys(
                location,
                "id",
                "phaseLabel",
                "titleKey",
                "actionKey",
                "metric",
                "target",
            )
            TutorialStepDefinition(
                GameId.of(item.requireString("id")),
                item.requireString("phaseLabel"),
                item.requireString("titleKey"),
                item.requireString("actionKey"),
                ProgressMetric.valueOf(item.requireString("metric")),
                item.requireLong("target"),
            )
        }
        val missions = root.requireArray("missions").mapIndexed { index, value ->
            val location = "missions[$index]"
            val item = value.requireObject(location)
            item.requireKnownKeys(
                location,
                "id",
                "kind",
                "titleKey",
                "metric",
                "target",
                "rewardSpaceDollars",
                "requiredMissionIds",
            )
            val id = GameId.of(item.requireString("id"))
            id to MissionDefinition(
                id,
                MissionKind.valueOf(item.requireString("kind")),
                item.requireString("titleKey"),
                ProgressMetric.valueOf(item.requireString("metric")),
                item.requireLong("target"),
                item.requireLong("rewardSpaceDollars"),
                item.ids("requiredMissionIds"),
            )
        }.toMap(linkedMapOf())
        val contracts = root.requireArray("contracts").mapIndexed { index, value ->
            val location = "contracts[$index]"
            val item = value.requireObject(location)
            item.requireKnownKeys(
                location,
                "id",
                "tier",
                "titleKey",
                "resourceId",
                "quantity",
                "rewardSpaceDollars",
                "requiredMissionIds",
            )
            ContractDefinition(
                GameId.of(item.requireString("id")),
                ContractTier.valueOf(item.requireString("tier")),
                item.requireString("titleKey"),
                GameId.of(item.requireString("resourceId")),
                item.requireLong("quantity"),
                item.requireLong("rewardSpaceDollars"),
                item.ids("requiredMissionIds"),
            )
        }
        val entries = root.requireArray("codexEntries").mapIndexed { index, value ->
            val location = "codexEntries[$index]"
            val item = value.requireObject(location)
            item.requireKnownKeys(
                location,
                "id",
                "category",
                "titleKey",
                "metric",
                "target",
                "requiredMissionIds",
                "collectionId",
            )
            val id = GameId.of(item.requireString("id"))
            id to CodexEntryDefinition(
                id,
                CodexCategory.valueOf(item.requireString("category")),
                item.requireString("titleKey"),
                ProgressMetric.valueOf(item.requireString("metric")),
                item.requireLong("target"),
                item.ids("requiredMissionIds"),
                item.optionalString("collectionId")?.let(GameId::of),
            )
        }.toMap(linkedMapOf())
        val collections = root.requireArray("collections").mapIndexed { index, value ->
            val location = "collections[$index]"
            val item = value.requireObject(location)
            item.requireKnownKeys(
                location,
                "id",
                "titleKey",
                "entryIds",
                "rewardSpaceDollars",
            )
            val id = GameId.of(item.requireString("id"))
            id to CollectionDefinition(
                id,
                item.requireString("titleKey"),
                item.ids("entryIds"),
                item.requireLong("rewardSpaceDollars"),
            )
        }.toMap(linkedMapOf())
        return ProgressionDefinitions(
            schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion"),
            contentVersion = root.requireString("contentVersion"),
            tutorialSteps = tutorials,
            missions = missions,
            contracts = contracts,
            codexEntries = entries,
            collections = collections,
        )
    }

    companion object {
        const val DEFAULT_PATH = "data/progression.json"
    }
}

private fun StrictJsonValue.ObjectValue.ids(key: String): Set<GameId> =
    optionalArray(key).mapTo(linkedSetOf()) { value ->
        val text = (value as? StrictJsonValue.StringValue)?.value
            ?: error("Invalid id in $key")
        GameId.of(text)
    }
