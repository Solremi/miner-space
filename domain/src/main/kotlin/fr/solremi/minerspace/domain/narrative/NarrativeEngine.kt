package fr.solremi.minerspace.domain.narrative

import fr.solremi.minerspace.shared.GameId

class NarrativeEngine(val definitions: NarrativeDefinitions) {
    fun initialState(): NarrativeState = NarrativeState(
        readTransmissionIds = emptySet(),
        resolvedChapterIds = emptySet(),
        anomalyAttempts = emptyMap(),
        discoveredRareResourceIds = emptySet(),
        veteranRobotId = null,
        selectedChapterId = definitions.chapters.keys.firstOrNull(),
        pendingGrant = null,
        transactionSequence = 0L,
    )

    fun normalize(source: NarrativeState): NarrativeState {
        val chapterIds = definitions.chapters.keys
        val rareIds = definitions.chapters.values.mapNotNullTo(linkedSetOf()) { it.rareResourceId }
        val resolved = source.resolvedChapterIds.filterTo(linkedSetOf(), chapterIds::contains)
        val read = source.readTransmissionIds.filterTo(linkedSetOf(), chapterIds::contains)
        val attempts = source.anomalyAttempts.filterKeys(chapterIds::contains).mapValues { it.value.coerceAtLeast(0) }
        val pending = source.pendingGrant?.takeIf { it.chapterId in chapterIds }
        return source.copy(
            readTransmissionIds = read,
            resolvedChapterIds = resolved,
            anomalyAttempts = attempts,
            discoveredRareResourceIds = source.discoveredRareResourceIds.filterTo(linkedSetOf(), rareIds::contains),
            veteranRobotId = source.veteranRobotId?.takeIf { it == definitions.veteranRobotId },
            selectedChapterId = source.selectedChapterId?.takeIf(chapterIds::contains) ?: chapterIds.firstOrNull(),
            pendingGrant = pending,
            transactionSequence = source.transactionSequence.coerceAtLeast(0L),
        )
    }

    fun chapterViews(state: NarrativeState, snapshot: NarrativeSnapshot): List<NarrativeChapterView> =
        definitions.chapters.values.map { chapter ->
            NarrativeChapterView(
                definition = chapter,
                available = isAvailable(state, snapshot, chapter),
                read = chapter.id in state.readTransmissionIds,
                resolved = chapter.id in state.resolvedChapterIds,
                attempts = state.anomalyAttempts[chapter.id] ?: 0,
            )
        }

    fun visibleArchives(state: NarrativeState): List<NarrativeChapterDefinition> =
        definitions.chapters.values.filter { it.id in state.readTransmissionIds }

    fun selectChapter(state: NarrativeState, chapterId: GameId): NarrativeState =
        if (chapterId in definitions.chapters) state.copy(selectedChapterId = chapterId) else state

    fun readTransmission(
        state: NarrativeState,
        chapterId: GameId,
        snapshot: NarrativeSnapshot,
    ): NarrativeCommandResult {
        val chapter = definitions.chapters[chapterId]
            ?: return NarrativeCommandResult.Rejected(state, "unknown_chapter")
        if (!isAvailable(state, snapshot, chapter)) {
            return NarrativeCommandResult.Rejected(state, "chapter_unavailable")
        }
        if (chapterId in state.readTransmissionIds) {
            return NarrativeCommandResult.Rejected(state, "transmission_already_read")
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return NarrativeCommandResult.Applied(
            state.copy(
                readTransmissionIds = state.readTransmissionIds + chapterId,
                selectedChapterId = chapterId,
                transactionSequence = sequence,
            ),
            NarrativeTransaction(sequence, "read_transmission", chapterId),
        )
    }

    fun investigate(
        state: NarrativeState,
        chapterId: GameId,
        snapshot: NarrativeSnapshot,
    ): NarrativeCommandResult {
        if (state.pendingGrant != null) return NarrativeCommandResult.Rejected(state, "grant_pending")
        val chapter = definitions.chapters[chapterId]
            ?: return NarrativeCommandResult.Rejected(state, "unknown_chapter")
        if (!isAvailable(state, snapshot, chapter)) {
            return NarrativeCommandResult.Rejected(state, "chapter_unavailable")
        }
        if (chapterId !in state.readTransmissionIds) {
            return NarrativeCommandResult.Rejected(state, "transmission_unread")
        }
        if (chapterId in state.resolvedChapterIds) {
            return NarrativeCommandResult.Rejected(state, "chapter_already_resolved")
        }

        val attempt = Math.addExact(state.anomalyAttempts[chapterId] ?: 0, 1)
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val roll = Math.floorMod(
            chapter.deterministicSeed.toLong() + attempt * 37L + sequence * 17L,
            100L,
        ).toInt()
        val success = roll < chapter.anomalyChancePercent || attempt >= chapter.pityAttempts
        if (!success) {
            return NarrativeCommandResult.Applied(
                state.copy(
                    anomalyAttempts = state.anomalyAttempts + (chapterId to attempt),
                    selectedChapterId = chapterId,
                    transactionSequence = sequence,
                ),
                NarrativeTransaction(sequence, "anomaly_no_result", chapterId),
            )
        }

        val rareId = chapter.rareResourceId
        val expectedRare = rareId?.let { Math.addExact(snapshot.inventory[it] ?: 0L, 1L) } ?: 0L
        val veteranId = definitions.veteranRobotId.takeIf { chapter.grantsVeteranRobot }
        val expectedVeteran = veteranId?.let {
            maxOf(snapshot.robotMasteryById[it] ?: 0L, definitions.veteranMasteryPoints)
        } ?: 0L
        val grant = PendingNarrativeGrant(
            grantId = "narrative_${sequence}_${chapter.id.value}",
            chapterId = chapter.id,
            rareResourceId = rareId,
            expectedRareTotal = expectedRare,
            veteranRobotId = veteranId,
            expectedVeteranMastery = expectedVeteran,
        )
        return NarrativeCommandResult.Applied(
            state.copy(
                anomalyAttempts = state.anomalyAttempts + (chapterId to attempt),
                selectedChapterId = chapterId,
                pendingGrant = grant,
                transactionSequence = sequence,
            ),
            NarrativeTransaction(sequence, "anomaly_grant_prepared", chapterId, grant),
        )
    }

    fun finalizePending(state: NarrativeState): NarrativeCommandResult {
        val grant = state.pendingGrant ?: return NarrativeCommandResult.Rejected(state, "no_pending_grant")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val rare = grant.rareResourceId
        return NarrativeCommandResult.Applied(
            state.copy(
                resolvedChapterIds = state.resolvedChapterIds + grant.chapterId,
                discoveredRareResourceIds = rare?.let { state.discoveredRareResourceIds + it }
                    ?: state.discoveredRareResourceIds,
                veteranRobotId = grant.veteranRobotId ?: state.veteranRobotId,
                pendingGrant = null,
                transactionSequence = sequence,
            ),
            NarrativeTransaction(sequence, "anomaly_grant_finalized", grant.chapterId),
        )
    }

    fun missingRareQuantity(grant: PendingNarrativeGrant, snapshot: NarrativeSnapshot): Long {
        val resourceId = grant.rareResourceId ?: return 0L
        return (grant.expectedRareTotal - (snapshot.inventory[resourceId] ?: 0L)).coerceAtLeast(0L)
    }

    fun requiredVeteranMastery(grant: PendingNarrativeGrant, snapshot: NarrativeSnapshot): Long {
        val robotId = grant.veteranRobotId ?: return 0L
        return (grant.expectedVeteranMastery - (snapshot.robotMasteryById[robotId] ?: 0L)).coerceAtLeast(0L)
    }

    private fun isAvailable(
        state: NarrativeState,
        snapshot: NarrativeSnapshot,
        chapter: NarrativeChapterDefinition,
    ): Boolean = snapshot.unlockedSectorCount >= chapter.requiredUnlockedSectors &&
        snapshot.installedTechnologyCount >= chapter.requiredTechnologies &&
        state.resolvedChapterIds.containsAll(chapter.requiredResolvedChapterIds)
}
