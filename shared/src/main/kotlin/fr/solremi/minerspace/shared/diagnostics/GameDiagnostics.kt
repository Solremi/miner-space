package fr.solremi.minerspace.shared.diagnostics

import fr.solremi.minerspace.shared.GameLogger

enum class DiagnosticLevel { DEBUG, INFO, WARNING, ERROR }

data class GameDiagnosticEvent(
    val timestampEpochMillis: Long,
    val level: DiagnosticLevel,
    val tag: String,
    val code: String,
    val exceptionClass: String? = null,
) {
    init {
        require(timestampEpochMillis >= 0L)
        require(TAG_PATTERN.matches(tag)) { "Invalid diagnostic tag" }
        require(CODE_PATTERN.matches(code)) { "Invalid diagnostic code" }
        require(exceptionClass == null || exceptionClass.length <= 160)
    }

    private companion object {
        val TAG_PATTERN = Regex("[A-Za-z0-9_.-]{1,80}")
        val CODE_PATTERN = Regex("log_[0-9a-f]{16}")
    }
}

interface GameDiagnosticStore {
    fun record(event: GameDiagnosticEvent)
    fun snapshot(): List<GameDiagnosticEvent>
}

class RingBufferGameDiagnosticStore(
    capacity: Int = 64,
) : GameDiagnosticStore {
    private val values = arrayOfNulls<GameDiagnosticEvent>(
        capacity.also { require(it in 8..512) },
    )
    private var cursor = 0
    private var count = 0

    @Synchronized
    override fun record(event: GameDiagnosticEvent) {
        values[cursor] = event
        cursor = (cursor + 1) % values.size
        if (count < values.size) count++
    }

    @Synchronized
    override fun snapshot(): List<GameDiagnosticEvent> {
        if (count == 0) return emptyList()
        val start = if (count < values.size) 0 else cursor
        return List(count) { offset ->
            values[(start + offset) % values.size]!!
        }
    }
}

class DiagnosticGameLogger(
    private val delegate: GameLogger,
    private val store: GameDiagnosticStore,
    private val nowEpochMillis: () -> Long,
) : GameLogger {
    override fun debug(tag: String, message: String) {
        record(DiagnosticLevel.DEBUG, tag, message, null)
        delegate.debug(tag, message)
    }

    override fun info(tag: String, message: String) {
        record(DiagnosticLevel.INFO, tag, message, null)
        delegate.info(tag, message)
    }

    override fun warning(tag: String, message: String, cause: Throwable?) {
        record(DiagnosticLevel.WARNING, tag, message, cause)
        delegate.warning(tag, message, cause)
    }

    override fun error(tag: String, message: String, cause: Throwable?) {
        record(DiagnosticLevel.ERROR, tag, message, cause)
        delegate.error(tag, message, cause)
    }

    private fun record(
        level: DiagnosticLevel,
        tag: String,
        message: String,
        cause: Throwable?,
    ) {
        store.record(
            GameDiagnosticEvent(
                timestampEpochMillis = nowEpochMillis().coerceAtLeast(0L),
                level = level,
                tag = sanitizeTag(tag),
                code = fingerprint(message),
                exceptionClass = cause?.javaClass?.name?.take(160),
            ),
        )
    }

    private fun sanitizeTag(value: String): String = value
        .take(80)
        .map { character ->
            if (character.isLetterOrDigit() || character in "_.-") character else '_'
        }
        .joinToString("")
        .ifBlank { "Unknown" }

    companion object {
        fun fingerprint(message: String): String {
            var hash = -0x340d631b7bdddcdbL
            message.forEach { character ->
                hash = hash xor character.code.toLong()
                hash *= 0x100000001b3L
            }
            return "log_${hash.toULong().toString(16).padStart(16, '0')}"
        }
    }
}
