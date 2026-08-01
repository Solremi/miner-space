package fr.solremi.minerspace.data.transaction

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class SaveMutation(
    val slotId: String,
    val payload: SavePayload?,
) {
    init {
        require(SLOT_PATTERN.matches(slotId)) { "Invalid transaction slot: $slotId" }
        require(slotId != SaveTransactionCoordinator.JOURNAL_SLOT_ID) { "The journal cannot mutate itself" }
        payload?.let { require(it.slotId == slotId) { "Payload slot does not match mutation slot" } }
    }

    companion object {
        private val SLOT_PATTERN = Regex("[a-z0-9_]{1,32}")
        fun clear(slotId: String): SaveMutation = SaveMutation(slotId, null)
        fun write(payload: SavePayload): SaveMutation = SaveMutation(payload.slotId, payload)
    }
}

enum class SaveTransactionStatus {
    NO_PENDING,
    COMMITTED,
    PENDING,
    BUSY,
    CORRUPT,
    PREPARE_FAILED,
}

data class SaveTransactionResult(
    val status: SaveTransactionStatus,
    val transactionId: String? = null,
    val failedSlotId: String? = null,
)

class SaveTransactionCoordinator(
    private val save: SaveService,
) {
    private val codec = SaveTransactionJournalCodec()

    fun execute(
        transactionId: String,
        mutations: List<SaveMutation>,
        nowEpochMillis: Long,
    ): SaveTransactionResult {
        require(TRANSACTION_ID_PATTERN.matches(transactionId)) { "Invalid transaction id: $transactionId" }
        require(nowEpochMillis >= 0L)
        require(mutations.isNotEmpty())
        require(mutations.map { it.slotId }.distinct().size == mutations.size) { "Duplicate transaction slot" }

        val pendingPayload = runCatching { save.loadLatest(JOURNAL_SLOT_ID) }
            .getOrElse { return SaveTransactionResult(SaveTransactionStatus.PENDING, transactionId, JOURNAL_SLOT_ID) }
        if (pendingPayload != null) {
            val pending = runCatching { codec.decode(pendingPayload) }
                .getOrElse { return SaveTransactionResult(SaveTransactionStatus.CORRUPT) }
            if (pending.transactionId != transactionId) {
                return SaveTransactionResult(SaveTransactionStatus.BUSY, pending.transactionId)
            }
            return applyAndFinalize(pending)
        }

        val journal = SaveTransactionJournal(transactionId, nowEpochMillis, mutations)
        val preparation = runCatching { save.save(codec.encode(journal)) }
            .getOrElse { SaveWriteStatus.FAILED }
        if (preparation != SaveWriteStatus.WRITTEN) {
            return SaveTransactionResult(SaveTransactionStatus.PREPARE_FAILED, transactionId, JOURNAL_SLOT_ID)
        }
        return applyAndFinalize(journal)
    }

    fun recoverPending(): SaveTransactionResult {
        val payload = runCatching { save.loadLatest(JOURNAL_SLOT_ID) }
            .getOrElse { return SaveTransactionResult(SaveTransactionStatus.PENDING, failedSlotId = JOURNAL_SLOT_ID) }
            ?: return SaveTransactionResult(SaveTransactionStatus.NO_PENDING)
        val journal = runCatching { codec.decode(payload) }
            .getOrElse { return SaveTransactionResult(SaveTransactionStatus.CORRUPT) }
        return applyAndFinalize(journal)
    }

    private fun applyAndFinalize(journal: SaveTransactionJournal): SaveTransactionResult {
        journal.mutations.forEach { mutation ->
            val applied = runCatching {
                val target = mutation.payload
                if (target == null) {
                    save.clear(mutation.slotId)
                    save.loadLatest(mutation.slotId) == null
                } else {
                    val current = save.loadLatest(mutation.slotId)
                    if (!matches(current, target) && save.save(target) != SaveWriteStatus.WRITTEN) {
                        false
                    } else {
                        matches(save.loadLatest(mutation.slotId), target)
                    }
                }
            }.getOrDefault(false)
            if (!applied) {
                return SaveTransactionResult(
                    SaveTransactionStatus.PENDING,
                    journal.transactionId,
                    mutation.slotId,
                )
            }
        }

        val finalized = runCatching {
            save.clear(JOURNAL_SLOT_ID)
            save.loadLatest(JOURNAL_SLOT_ID) == null
        }.getOrDefault(false)
        return if (finalized) {
            SaveTransactionResult(SaveTransactionStatus.COMMITTED, journal.transactionId)
        } else {
            SaveTransactionResult(SaveTransactionStatus.PENDING, journal.transactionId, JOURNAL_SLOT_ID)
        }
    }

    private fun matches(current: SavePayload?, target: SavePayload): Boolean =
        current != null &&
            current.slotId == target.slotId &&
            current.schemaVersion == target.schemaVersion &&
            current.contentVersion == target.contentVersion &&
            current.bytes.contentEquals(target.bytes)

    companion object {
        const val JOURNAL_SLOT_ID = "transaction_journal"
        private val TRANSACTION_ID_PATTERN = Regex("[a-z0-9_]{1,64}")
    }
}

private data class SaveTransactionJournal(
    val transactionId: String,
    val createdAtEpochMillis: Long,
    val mutations: List<SaveMutation>,
)

private class SaveTransactionJournalCodec {
    fun encode(journal: SaveTransactionJournal): SavePayload {
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(FORMAT_VERSION)
                output.writeUTF(journal.transactionId)
                output.writeLong(journal.createdAtEpochMillis)
                output.writeInt(journal.mutations.size)
                journal.mutations.forEach { mutation ->
                    output.writeUTF(mutation.slotId)
                    val target = mutation.payload
                    output.writeBoolean(target != null)
                    if (target != null) {
                        output.writeInt(target.schemaVersion)
                        output.writeUTF(target.contentVersion)
                        output.writeLong(target.savedAtEpochMillis)
                        output.writeInt(target.bytes.size)
                        output.write(target.bytes)
                    }
                }
            }
            buffer.toByteArray()
        }
        require(bytes.size <= MAX_JOURNAL_BYTES) { "Transaction journal is too large" }
        return SavePayload(
            slotId = SaveTransactionCoordinator.JOURNAL_SLOT_ID,
            schemaVersion = FORMAT_VERSION,
            contentVersion = CONTENT_VERSION,
            bytes = bytes,
            savedAtEpochMillis = journal.createdAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): SaveTransactionJournal {
        require(payload.slotId == SaveTransactionCoordinator.JOURNAL_SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        return DataInputStream(ByteArrayInputStream(payload.bytes)).use { input ->
            require(input.readInt() == MAGIC) { "Invalid transaction journal header" }
            require(input.readInt() == FORMAT_VERSION) { "Unsupported transaction journal" }
            val transactionId = input.readUTF()
            require(TRANSACTION_ID_PATTERN.matches(transactionId))
            val createdAt = input.readLong()
            require(createdAt >= 0L)
            val count = input.readInt()
            require(count in 1..MAX_MUTATIONS)
            val mutations = List(count) {
                val slotId = input.readUTF()
                require(SLOT_PATTERN.matches(slotId))
                require(slotId != SaveTransactionCoordinator.JOURNAL_SLOT_ID)
                val payloadPresent = input.readBoolean()
                if (!payloadPresent) {
                    SaveMutation.clear(slotId)
                } else {
                    val schemaVersion = input.readInt()
                    val contentVersion = input.readUTF()
                    val savedAt = input.readLong()
                    val size = input.readInt()
                    require(schemaVersion > 0 && contentVersion.isNotBlank() && savedAt >= 0L)
                    require(size in 0..MAX_TARGET_BYTES)
                    val bytes = ByteArray(size)
                    input.readFully(bytes)
                    SaveMutation.write(
                        SavePayload(
                            slotId = slotId,
                            schemaVersion = schemaVersion,
                            contentVersion = contentVersion,
                            bytes = bytes,
                            savedAtEpochMillis = savedAt,
                        ),
                    )
                }
            }
            require(mutations.map { it.slotId }.distinct().size == mutations.size)
            require(input.available() == 0) { "Trailing transaction journal data" }
            SaveTransactionJournal(transactionId, createdAt, mutations)
        }
    }

    private companion object {
        const val MAGIC = 0x4D53544A
        const val FORMAT_VERSION = 1
        const val CONTENT_VERSION = "1.0.0"
        const val MAX_MUTATIONS = 32
        const val MAX_TARGET_BYTES = 2 * 1024 * 1024
        const val MAX_JOURNAL_BYTES = 2 * 1024 * 1024
        val SLOT_PATTERN = Regex("[a-z0-9_]{1,32}")
        val TRANSACTION_ID_PATTERN = Regex("[a-z0-9_]{1,64}")
    }
}
