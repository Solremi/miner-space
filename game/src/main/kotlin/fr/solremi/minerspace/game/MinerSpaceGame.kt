package fr.solremi.minerspace.game

import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.presentation.PresentationController
import fr.solremi.minerspace.game.screen.FatalErrorScreen
import fr.solremi.minerspace.game.screen.MeteorShowerScreen
import fr.solremi.minerspace.game.screen.MissionControlScreen
import fr.solremi.minerspace.game.screen.NarrativeArchiveScreen
import fr.solremi.minerspace.game.screen.OfflineReturnScreen
import fr.solremi.minerspace.game.screen.PresentationGameplayScreen
import fr.solremi.minerspace.game.screen.PresentationSettingsScreen
import fr.solremi.minerspace.game.screen.RobotFleetScreen
import fr.solremi.minerspace.game.screen.StrategyLabScreen
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MinerSpaceGame(private val services: GameServices, private val logger: GameLogger = SilentGameLogger) : KtxGame<KtxScreen>() {
    private var added = false

    override fun create() {
        try {
            PresentationController.loadAndApply(services)
            addScreen(OfflineReturnScreen(services) { addScreens(); setScreen<PresentationGameplayScreen>() })
            setScreen<OfflineReturnScreen>()
        } catch (failure: Throwable) {
            logger.error(TAG, "Unable to create the initial scene.", failure)
            addScreen(FatalErrorScreen("Erreur de démarrage", failure.message ?: failure::class.simpleName.orEmpty()))
            setScreen<FatalErrorScreen>()
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
        ))
        addScreen(MeteorShowerScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(RobotFleetScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(StrategyLabScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(MissionControlScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(NarrativeArchiveScreen(services) { setScreen<PresentationGameplayScreen>() })
        addScreen(PresentationSettingsScreen(services) { setScreen<PresentationGameplayScreen>() })
        added = true
    }

    private companion object { const val TAG = "MinerSpaceGame" }
}
