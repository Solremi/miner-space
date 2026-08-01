package fr.solremi.minerspace.data.economy

import fr.solremi.minerspace.data.json.StrictJson
import fr.solremi.minerspace.data.json.optionalBoolean
import fr.solremi.minerspace.data.json.optionalLong
import fr.solremi.minerspace.data.json.requireArray
import fr.solremi.minerspace.data.json.requireKnownKeys
import fr.solremi.minerspace.data.json.requireLong
import fr.solremi.minerspace.data.json.requireObject
import fr.solremi.minerspace.data.json.requireString
import fr.solremi.minerspace.data.json.toIntExact
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
        val root = StrictJson.parse(content).requireObject("root")
        root.requireKnownKeys("root", "schemaVersion", "contentVersion", "items")
        val schemaVersion = root.requireLong("schemaVersion").toIntExact("schemaVersion")
        val contentVersion = root.requireString("contentVersion")
        val resources = linkedMapOf<GameId, ResourceDefinition>()
        val deposits = linkedMapOf<GameId, DepositDefinition>()

        root.requireArray("items").forEachIndexed { index, value ->
            val location = "items[$index]"
            val item = value.requireObject(location)
            when (item.requireString("type")) {
                "resource" -> {
                    item.requireKnownKeys(
                        location,
                        "type",
                        "id",
                        "nameKey",
                        "unitSalePrice",
                        "storageCapacity",
                        "sellable",
                    )
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
                    item.requireKnownKeys(
                        location,
                        "type",
                        "id",
                        "resourceId",
                        "initialReserve",
                        "extractionPerSecond",
                        "transportCapacity",
                        "productionMultiplier",
                    )
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
