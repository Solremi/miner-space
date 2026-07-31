package fr.solremi.minerspace.game

import fr.solremi.minerspace.data.save.PrestigeStateCodec
import fr.solremi.minerspace.domain.prestige.PlanetId
import fr.solremi.minerspace.domain.prestige.PlanetPrestigeEngine
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.presentation.PresentationController
import fr.solremi.minerspace.game.screen.*
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MinerSpaceGame(private val services: GameServices, private val logger: GameLogger = SilentGameLogger) : KtxGame<KtxScreen>() {
    private var added = false

    override fun create() {
        try {
            PresentationController.loadAndApply(services)
            addScreen(OfflineReturnScreen(services) { addScreens(); routeAfterOffline() })
            setScreen<OfflineReturnScreen>()
        } catch (failure: Throwable) {
            logger.error(TAG, "Unable to create the initial scene.", failure)
            addScreen(FatalErrorScreen("Erreur de démarrage", failure.message ?: failure::class.simpleName.orEmpty()))
            setScreen<FatalErrorScreen>()
        }
    }

    private fun routeAfterOffline() {
        val initial = PlanetPrestigeEngine().initialState()
        val prestige = runCatching {
            services.save.loadLatest(PrestigeStateCodec.SLOT_ID)?.let(PrestigeStateCodec()::decode) ?: initial
        }.getOrElse { initial }
        when {
            prestige.pendingTransfer != null -> setScreen<PlanetTransferScreen>()
            prestige.activePlanet == PlanetId.CRYOS_IX -> setScreen<CryosIxScreen>()
            else -> setScreen<PresentationGameplayScreen>()
        }
    }

    private fun addScreens() {
        if (added) return
        addScreen(PresentationGameplayScreen(
            services,
            { setScreen<MeteorShowerScreen>() },
            { setScreen<RobotFleetScreen>() },
            { setScreen<StrategyLabScreen>() },
            { setScreen<MissionControlScreen>() },
            { setScreen<NarrativeArchiveScreen>() },
            { setScreen<PresentationSettingsScreen>() },
            { setScreen<PlanetTransferScreen>() },
        ))
        addScreen(MeteorShowerScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(RobotFleetScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(StrategyLabScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(MissionControlScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(NarrativeArchiveScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(PresentationSettingsScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(PlanetTransferScreen(
            services,
            onFerrumBack = { setScreen<PresentationGameplayScreen>() },
            onCryosReady = { setScreen<CryosIxScreen>() },
        ))
        addScreen(CryosIxScreen(services) { setScreen<PlanetTransferScreen>() })
        added = true
    }

    private companion object { const val TAG = "MinerSpaceGame" }
}
