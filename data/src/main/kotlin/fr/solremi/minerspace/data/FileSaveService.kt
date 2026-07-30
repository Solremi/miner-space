package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class FileSaveService(
    private val rootDirectory: Path,
) : SaveService {
    init {
        Files.createDirectories(rootDirectory)
    }

    @Synchronized
    override fun loadLatest(slotId: String): SavePayload? {
        val path = slotPath(slotId)
        if (!Files.exists(path)) return null
        return runCatching {
            DataInputStream(Files.newInputStream(path)).use { input ->
                require(input.readInt() == MAGIC) { "Invalid Miner Space save header" }
                val schemaVersion = input.readInt()
                val contentVersion = input.readUTF()
                val size = input.readInt()
                require(size in 0..MAX_PAYLOAD_BYTES) { "Invalid save payload size" }
                val bytes = ByteArray(size)
                input.readFully(bytes)
                SavePayload(slotId, schemaVersion, contentVersion, bytes)
            }
        }.getOrNull()
    }

    @Synchronized
    override fun save(payload: SavePayload): SaveWriteStatus {
        if (!isValidSlot(payload.slotId) || payload.schemaVersion < 1 ||
            payload.contentVersion.isBlank() || payload.bytes.size > MAX_PAYLOAD_BYTES
        ) {
            return SaveWriteStatus.REJECTED
        }
        return runCatching {
            Files.createDirectories(rootDirectory)
            val target = slotPath(payload.slotId)
            val temporary = rootDirectory.resolve(".${payload.slotId}.tmp")
            DataOutputStream(Files.newOutputStream(temporary)).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(payload.schemaVersion)
                output.writeUTF(payload.contentVersion)
                output.writeInt(payload.bytes.size)
                output.write(payload.bytes)
                output.flush()
            }
            runCatching {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            }.getOrElse {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
            SaveWriteStatus.WRITTEN
        }.getOrElse { SaveWriteStatus.FAILED }
    }

    @Synchronized
    override fun clear(slotId: String) {
        if (isValidSlot(slotId)) Files.deleteIfExists(slotPath(slotId))
    }

    private fun slotPath(slotId: String): Path {
        require(isValidSlot(slotId)) { "Invalid save slot" }
        return rootDirectory.resolve("$slotId.msv")
    }

    private fun isValidSlot(slotId: String): Boolean = SLOT_PATTERN.matches(slotId)

    private companion object {
        const val MAGIC = 0x4D535631
        const val MAX_PAYLOAD_BYTES = 2 * 1024 * 1024
        val SLOT_PATTERN = Regex("[a-z0-9_]{1,32}")
    }
}
