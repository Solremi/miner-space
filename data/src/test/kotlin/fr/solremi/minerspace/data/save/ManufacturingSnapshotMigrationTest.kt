package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.SavePayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManufacturingSnapshotMigrationTest {
    @Test
    fun `schema one refining save migrates with an empty assembly state`() {
        val payload = SavePayload(
            slotId = "primary",
            schemaVersion = 1,
            contentVersion = "0.3.0",
            bytes = """
                format=1
                contentVersion=0.3.0
                spaceDollars=42
                transactionSequence=7
                inventory=raw_iron:18
                deposits=deposit_iron_alpha:90:10
                nextJobSequence=1
                refundBuffer=
                jobs=
            """.trimIndent().toByteArray(),
        )

        val decoded = ManufacturingSnapshotCodec().decodeWithMetadata(payload)

        assertEquals(42L, decoded.state.economy.spaceDollars)
        assertTrue(decoded.state.assembly.jobs.isEmpty())
        assertTrue(decoded.state.assembly.installedTechnologyIds.isEmpty())
        assertTrue(decoded.requiresRewrite)
    }

    @Test
    fun `schema two is accepted and marked for rewrite`() {
        val codec = ManufacturingSnapshotCodec()
        val schemaThree = codec.encode(emptyState(), "0.4.0", savedAtEpochMillis = 1_000L)
        val schemaTwo = schemaThree.copy(
            schemaVersion = 2,
            bytes = schemaThree.bytes.toString(Charsets.UTF_8)
                .replaceFirst("format=3", "format=2")
                .toByteArray(),
        )

        val decoded = codec.decodeWithMetadata(schemaTwo)

        assertEquals(2, decoded.sourceSchemaVersion)
        assertTrue(decoded.requiresRewrite)
    }

    private fun emptyState() = ManufacturingGameState(
        economy = EconomyState(emptyMap(), emptyMap(), 0L, 0L),
        refining = RefiningState.empty(),
        assembly = AssemblyState.empty(),
    )
}
