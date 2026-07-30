package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import java.util.concurrent.ConcurrentHashMap

class InMemorySaveService : SaveService {
    private val slots = ConcurrentHashMap<String, SavePayload>()

    override fun loadLatest(slotId: String): SavePayload? {
        val payload = slots[slotId] ?: return null
        return payload.copy(bytes = payload.bytes.copyOf())
    }

    override fun save(payload: SavePayload): SaveWriteStatus {
        if (payload.schemaVersion < 1 || payload.slotId.isBlank()) {
            return SaveWriteStatus.REJECTED
        }

        slots[payload.slotId] = payload.copy(bytes = payload.bytes.copyOf())
        return SaveWriteStatus.WRITTEN
    }

    override fun clear(slotId: String) {
        slots.remove(slotId)
    }
}
