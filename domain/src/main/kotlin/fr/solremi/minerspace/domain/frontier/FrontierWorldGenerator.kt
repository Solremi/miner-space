package fr.solremi.minerspace.domain.frontier

import fr.solremi.minerspace.shared.GameId

class FrontierWorldGenerator(private val definitions: FrontierDefinitions) {
    fun generate(
        seed: Long,
        generationIndex: Int,
        difficulty: FrontierDifficulty,
        previousSignature: String?,
    ): FrontierWorldDefinition {
        require(generationIndex >= 0)
        repeat(128) { salt ->
            val random = DeterministicRandom(mix(seed, generationIndex, salt))
            val family = FrontierVisualFamily.entries[random.nextInt(FrontierVisualFamily.entries.size)]
            val objective = definitions.objectives.values.sortedBy { it.id.value }
                .let { it[random.nextInt(it.size)] }
            val modifiers = selectModifiers(random, family, objective, difficulty.modifierCount) ?: return@repeat
            val sectors = selectSectors(random, family, objective, modifiers, difficulty, generationIndex)
            val progressMultiplier = modifiers.fold(1_000_000L) { total, modifier ->
                (total + modifier.progressMultiplierMillionths - 1_000_000L).coerceAtLeast(400_000L)
            }
            val rewardMultiplier = modifiers.fold(1_000_000L) { total, modifier ->
                total + modifier.rewardMultiplierMillionths - 1_000_000L
            }
            val difficultyFactor = when (difficulty) {
                FrontierDifficulty.SCOUT -> 1L
                FrontierDifficulty.EXPEDITION -> 2L
                FrontierDifficulty.DEEP -> 3L
            }
            val target = multiplyMillionths(objective.baseTarget * difficultyFactor, progressMultiplier).coerceAtLeast(1L)
            val reward = multiplyMillionths(10L * difficultyFactor, rewardMultiplier).coerceAtLeast(1L)
            val world = FrontierWorldDefinition(
                id = GameId.of("frontier_world_${generationIndex}_${token(seed, salt)}"),
                seed = mix(seed, generationIndex, salt),
                generationIndex = generationIndex,
                family = family,
                difficulty = difficulty,
                modifierIds = modifiers.mapTo(linkedSetOf()) { it.id },
                objectiveId = objective.id,
                sectors = sectors,
                targetProgress = target,
                rewardKind = objective.rewardKind,
                rewardAmount = reward,
                estimatedDays = difficulty.estimatedDays,
            )
            if (world.signature != previousSignature && validationErrors(world).isEmpty()) return world
        }
        error("Unable to generate a compatible frontier world")
    }

    fun validationErrors(world: FrontierWorldDefinition): List<String> {
        val errors = mutableListOf<String>()
        val objective = definitions.objectives[world.objectiveId]
        if (objective == null) errors += "unknown_objective"
        val templates = world.sectors.mapNotNull { sector -> definitions.sectorTemplates[sector.templateId] }
        if (templates.size != world.sectors.size) errors += "unknown_sector_template"
        if (templates.any { it.family != world.family }) errors += "mixed_visual_family"
        if (objective != null && templates.none { objective.requiredCapability in it.capabilities }) errors += "objective_capability_missing"
        val modifierDefinitions = world.modifierIds.mapNotNull(definitions.modifiers::get)
        if (modifierDefinitions.size != world.modifierIds.size) errors += "unknown_modifier"
        modifierDefinitions.forEach { modifier ->
            if (world.family !in modifier.compatibleFamilies) errors += "modifier_family_incompatible:${modifier.id}"
            if (modifier.incompatibleModifierIds.any(world.modifierIds::contains)) errors += "modifier_pair_incompatible:${modifier.id}"
            if (modifier.requiredCapabilities.any { required -> templates.none { required in it.capabilities } }) {
                errors += "modifier_capability_missing:${modifier.id}"
            }
        }
        world.sectors.forEachIndexed { index, sector ->
            val expected = if (index == 0) null else world.sectors[index - 1].id
            if (sector.requiredSectorId != expected) errors += "broken_progression_chain:${sector.id}"
        }
        return errors
    }

    private fun selectModifiers(
        random: DeterministicRandom,
        family: FrontierVisualFamily,
        objective: FrontierObjectiveDefinition,
        count: Int,
    ): List<FrontierModifierDefinition>? {
        val candidates = definitions.modifiers.values
            .filter { family in it.compatibleFamilies }
            .filter { modifier -> modifier.requiredCapabilities.isEmpty() || objective.requiredCapability in modifier.requiredCapabilities }
            .shuffled(random)
        val selected = mutableListOf<FrontierModifierDefinition>()
        candidates.forEach { candidate ->
            if (selected.size >= count) return@forEach
            val compatible = selected.none { existing ->
                candidate.id in existing.incompatibleModifierIds || existing.id in candidate.incompatibleModifierIds
            }
            if (compatible) selected += candidate
        }
        return selected.takeIf { it.size == count }
    }

    private fun selectSectors(
        random: DeterministicRandom,
        family: FrontierVisualFamily,
        objective: FrontierObjectiveDefinition,
        modifiers: List<FrontierModifierDefinition>,
        difficulty: FrontierDifficulty,
        generationIndex: Int,
    ): List<GeneratedFrontierSector> {
        val required = linkedSetOf(objective.requiredCapability).apply {
            modifiers.flatMapTo(this) { it.requiredCapabilities }
        }
        val candidates = definitions.sectorTemplates.values.filter { it.family == family }.shuffled(random)
        val selected = mutableListOf<FrontierSectorTemplate>()
        required.forEach { capability ->
            candidates.firstOrNull { capability in it.capabilities && it !in selected }?.let(selected::add)
        }
        candidates.filterNot(selected::contains).forEach { if (selected.size < difficulty.sectorCount) selected += it }
        require(selected.size == difficulty.sectorCount)
        return selected.mapIndexed { index, template ->
            val id = GameId.of("frontier_sector_${generationIndex}_${index + 1}")
            GeneratedFrontierSector(id, template.id, if (index == 0) null else GameId.of("frontier_sector_${generationIndex}_${index}"))
        }
    }

    private fun <T> List<T>.shuffled(random: DeterministicRandom): List<T> {
        val copy = toMutableList()
        for (i in copy.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val value = copy[i]; copy[i] = copy[j]; copy[j] = value
        }
        return copy
    }

    private fun mix(seed: Long, index: Int, salt: Int): Long {
        var value = seed xor (index.toLong() * -7046029254386353131L) xor (salt.toLong() * -4658895280553007687L)
        value = (value xor (value ushr 30)) * -4658895280553007687L
        value = (value xor (value ushr 27)) * -7723592293110705685L
        return value xor (value ushr 31)
    }

    private fun token(seed: Long, salt: Int): Long = ((seed xor salt.toLong()) ushr 1) % 1_000_000L
    private fun multiplyMillionths(value: Long, multiplier: Long): Long = Math.multiplyExact(value, multiplier) / 1_000_000L
}

private class DeterministicRandom(seed: Long) {
    private var state = if (seed == 0L) -7046029254386353131L else seed
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return ((state and Long.MAX_VALUE) % bound).toInt()
    }
}
