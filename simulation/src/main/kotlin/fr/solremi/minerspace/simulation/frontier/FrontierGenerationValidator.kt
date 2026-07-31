package fr.solremi.minerspace.simulation.frontier

import fr.solremi.minerspace.domain.frontier.*

data class FrontierValidationReport(
    val generatedWorldCount: Int,
    val invalidWorldCount: Int,
    val immediateRepeatCount: Int,
    val minEstimatedDays: Int,
    val maxEstimatedDays: Int,
)

class FrontierGenerationValidator(
    private val definitions: FrontierDefinitions,
    private val generator: FrontierWorldGenerator = FrontierWorldGenerator(definitions),
) {
    fun validate(seed: Long, worldCount: Int = 10_000): FrontierValidationReport {
        require(worldCount > 0)
        var previousSignature: String? = null
        var invalid = 0
        var repeats = 0
        var minDays = Int.MAX_VALUE
        var maxDays = Int.MIN_VALUE
        repeat(worldCount) { index ->
            val difficulty = FrontierDifficulty.entries[index % FrontierDifficulty.entries.size]
            val world = generator.generate(seed, index, difficulty, previousSignature)
            if (generator.validationErrors(world).isNotEmpty()) invalid++
            if (world.signature == previousSignature) repeats++
            minDays = minOf(minDays, world.estimatedDays)
            maxDays = maxOf(maxDays, world.estimatedDays)
            previousSignature = world.signature
        }
        return FrontierValidationReport(worldCount, invalid, repeats, minDays, maxDays)
    }
}
