package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32

class FileSaveService(
    private val rootDirectory: Path,
) : SaveService {
    init {
        Files.createDirectories(rootDirectory)
    }

    @Synchronized
    override fun loadLatest(slotId: String): SavePayload? {
        require(isValidSlot(slotId)) { "Invalid save slot" }
        val candidates = listOf('a', 'b').map { suffix -> suffix to snapshotPath(slotId, suffix) }
        val existing = candidates.filter { (_, path) -> Files.exists(path) }
        val decoded = existing.mapNotNull { (suffix, path) -> readSnapshot(path, slotId, suffix) }
        if (decoded.isNotEmpty()) {
            val latest = decoded.maxBy { it.payload.sequence }.payload
            return latest.copy(recoveredFromFallback = decoded.size < existing.size)
        }

        val legacy = legacyPath(slotId)
        if (!Files.exists(legacy)) return null
        return readLegacy(legacy, slotId)?.copy(recoveredFromFallback = true)
    }

    @Synchronized
    override fun save(payload: SavePayload): SaveWriteStatus {
        if (!isValidSlot(payload.slotId) || payload.schemaVersion < 1 ||
            payload.contentVersion.isBlank() || payload.bytes.size > MAX_PAYLOAD_BYTES ||
            payload.savedAtEpochMillis < 0L
        ) {
            return SaveWriteStatus.REJECTED
        }

        return runCatching {
            Files.createDirectories(rootDirectory)
            val current = listOf('a', 'b').associateWith { suffix ->
                readSnapshot(snapshotPath(payload.slotId, suffix), payload.slotId, suffix)
            }
            val nextSequence = Math.addExact(current.values.mapNotNull { it?.payload?.sequence }.maxOrNull() ?: 0L, 1L)
            val targetSuffix = when {
                current.getValue('a') == null -> 'a'
                current.getValue('b') == null -> 'b'
                current.getValue('a')!!.payload.sequence <= current.getValue('b')!!.payload.sequence -> 'a'
                else -> 'b'
            }
            val target = snapshotPath(payload.slotId, targetSuffix)
            val temporary = rootDirectory.resolve(".${payload.slotId}.$targetSuffix.tmp")
            val checksum = checksum(payload.bytes)

            DataOutputStream(BufferedOutputStream(Files.newOutputStream(temporary))).use { output ->
                output.writeInt(MAGIC_V2)
                output.writeInt(CONTAINER_VERSION)
                output.writeLong(nextSequence)
                output.writeLong(payload.savedAtEpochMillis)
                output.writeInt(payload.schemaVersion)
                output.writeUTF(payload.contentVersion)
                output.writeInt(payload.bytes.size)
                output.writeLong(checksum)
                output.write(payload.bytes)
                output.flush()
            }
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { it.force(true) }
            atomicReplace(temporary, target)
            SaveWriteStatus.WRITTEN
        }.getOrElse { SaveWriteStatus.FAILED }
    }

    @Synchronized
    override fun clear(slotId: String) {
        if (!isValidSlot(slotId)) return
        Files.deleteIfExists(snapshotPath(slotId, 'a'))
        Files.deleteIfExists(snapshotPath(slotId, 'b'))
        Files.deleteIfExists(legacyPath(slotId))
        Files.deleteIfExists(rootDirectory.resolve(".$slotId.a.tmp"))
        Files.deleteIfExists(rootDirectory.resolve(".$slotId.b.tmp"))
    }

    private data class SnapshotCandidate(
        val suffix: Char,
        val payload: SavePayload,
    )

    private fun readSnapshot(path: Path, slotId: String, suffix: Char): SnapshotCandidate? {
        if (!Files.exists(path)) return null
        return runCatching {
            DataInputStream(BufferedInputStream(Files.newInputStream(path))).use { input ->
                require(input.readInt() == MAGIC_V2) { "Invalid Miner Space save header" }
                require(input.readInt() == CONTAINER_VERSION) { "Unsupported save container" }
                val sequence = input.readLong()
                val savedAt = input.readLong()
                val schemaVersion = input.readInt()
                val contentVersion = input.readUTF()
                val size = input.readInt()
                val expectedChecksum = input.readLong()
                require(sequence > 0L && savedAt >= 0L && schemaVersion > 0)
                require(size in 0..MAX_PAYLOAD_BYTES) { "Invalid save payload size" }
                val bytes = ByteArray(size)
                input.readFully(bytes)
                require(checksum(bytes) == expectedChecksum) { "Invalid save checksum" }
                SnapshotCandidate(
                    suffix = suffix,
                    payload = SavePayload(
                        slotId = slotId,
                        schemaVersion = schemaVersion,
                        contentVersion = contentVersion,
                        bytes = bytes,
                        savedAtEpochMillis = savedAt,
                        sequence = sequence,
                    ),
                )
            }
        }.getOrNull()
    }

    private fun readLegacy(path: Path, slotId: String): SavePayload? = runCatching {
        DataInputStream(BufferedInputStream(Files.newInputStream(path))).use { input ->
            require(input.readInt() == MAGIC_V1) { "Invalid legacy save header" }
            val schemaVersion = input.readInt()
            val contentVersion = input.readUTF()
            val size = input.readInt()
            require(size in 0..MAX_PAYLOAD_BYTES)
            val bytes = ByteArray(size)
            input.readFully(bytes)
            SavePayload(
                slotId = slotId,
                schemaVersion = schemaVersion,
                contentVersion = contentVersion,
                bytes = bytes,
                savedAtEpochMillis = Files.getLastModifiedTime(path).toMillis().coerceAtLeast(0L),
                sequence = 0L,
            )
        }
    }.getOrNull()

    private fun snapshotPath(slotId: String, suffix: Char): Path =
        rootDirectory.resolve("$slotId.$suffix.msv")

    private fun legacyPath(slotId: String): Path = rootDirectory.resolve("$slotId.msv")

    private fun atomicReplace(temporary: Path, target: Path) {
        runCatching {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun checksum(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value

    private fun isValidSlot(slotId: String): Boolean = SLOT_PATTERN.matches(slotId)

    private companion object {
        const val MAGIC_V1 = 0x4D535631
        const val MAGIC_V2 = 0x4D535632
        const val CONTAINER_VERSION = 2
        const val MAX_PAYLOAD_BYTES = 2 * 1024 * 1024
        val SLOT_PATTERN = Regex("[a-z0-9_]{1,32}")
    }
}
