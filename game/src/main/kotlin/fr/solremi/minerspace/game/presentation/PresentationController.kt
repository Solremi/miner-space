package fr.solremi.minerspace.game.presentation

import fr.solremi.minerspace.data.save.PresentationSettingsCodec
import fr.solremi.minerspace.domain.presentation.FeedbackKind
import fr.solremi.minerspace.domain.presentation.PresentationSettings
import fr.solremi.minerspace.domain.presentation.PresentationSettingsEngine
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.domain.services.SoundCue

object PresentationController {
    private val codec = PresentationSettingsCodec()
    private val engine = PresentationSettingsEngine()
    var current: PresentationSettings = PresentationSettings()
        private set

    fun loadAndApply(services: GameServices): PresentationSettings {
        val loaded = services.save.loadLatest(PresentationSettingsCodec.SLOT_ID)?.let { payload ->
            runCatching { codec.decode(payload) }.getOrNull()
        } ?: PresentationSettings()
        current = engine.normalize(loaded)
        applyToServices(services)
        return current
    }

    fun update(services: GameServices, settings: PresentationSettings): Boolean {
        current = engine.normalize(settings)
        applyToServices(services)
        val written = services.save.save(codec.encode(current, services.clock.nowEpochMillis().coerceAtLeast(0L))) == SaveWriteStatus.WRITTEN
        GameFeedbackBus.emit(FeedbackKind.INTERACTION, services.clock.monotonicMillis())
        return written
    }

    fun play(services: GameServices, cue: SoundCue, kind: FeedbackKind, normalizedX: Float = .5f, normalizedY: Float = .5f) {
        services.audio.play(cue)
        GameFeedbackBus.emit(kind, services.clock.monotonicMillis(), normalizedX, normalizedY)
    }

    fun engine(): PresentationSettingsEngine = engine

    private fun applyToServices(services: GameServices) {
        services.audio.setSoundEnabled(current.soundEnabled)
        services.audio.setMasterVolume(engine.normalizedVolume(current))
        services.haptic.setEnabled(current.vibrationEnabled)
    }
}
