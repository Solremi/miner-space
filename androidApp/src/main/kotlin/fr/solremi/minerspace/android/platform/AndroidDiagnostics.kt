package fr.solremi.minerspace.android.platform

import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.diagnostics.DiagnosticGameLogger
import fr.solremi.minerspace.shared.diagnostics.GameDiagnosticEvent
import fr.solremi.minerspace.shared.diagnostics.GameDiagnosticStore
import fr.solremi.minerspace.shared.diagnostics.RingBufferGameDiagnosticStore

internal object AndroidDiagnosticStore : GameDiagnosticStore {
    private val delegate = RingBufferGameDiagnosticStore(64)
    override fun record(event: GameDiagnosticEvent) = delegate.record(event)
    override fun snapshot(): List<GameDiagnosticEvent> = delegate.snapshot()
}

internal object AndroidDiagnosticLogger : GameLogger by DiagnosticGameLogger(
    delegate = AndroidGameLogger,
    store = AndroidDiagnosticStore,
    nowEpochMillis = System::currentTimeMillis,
)
