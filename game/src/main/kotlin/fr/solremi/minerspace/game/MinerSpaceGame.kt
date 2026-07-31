package fr.solremi.minerspace.game

import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.screen.FatalErrorScreen
import fr.solremi.minerspace.game.screen.GameplayHubScreen
import fr.solremi.minerspace.game.screen.MeteorShowerScreen
import fr.solremi.minerspace.game.screen.OfflineReturnScreen
import fr.solremi.minerspace.game.screen.RobotFleetScreen
import fr.solremi.minerspace.game.screen.StrategyLabScreen
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MinerSpaceGame(
    private val services: GameServices,
    private val logger: GameLogger = SilentGameLogger,
) : KtxGame<KtxScreen>() {
    private var gameplayScreensAdded = false

    override fun create() {
        try {
            logger.info(TAG, "Starting Miner Space save and offline bootstrap.")
            addScreen(
                OfflineReturnScreen(services) {
                    addGameplayScreensIfNeeded()
                    setScreen<GameplayHubScreen>()
                },
            )
            setScreen<OfflineReturnScreen>()
        } catch (failure: Throwable) {
            logger.error(TAG, "Unable to create the initial scene.", failure)
            addScreen(FatalErrorScreen("Erreur de démarrage", failure.message ?: failure::class.simpleName.orEmpty()))
            setScreen<FatalErrorScreen>()
        }
    }

    private fun addGameplayScreensIfNeeded() {
        if (gameplayScreensAdded) return
        addScreen(
            GameplayHubScreen(
                services = services,
                onMeteorRequested = { setScreen<MeteorShowerScreen>() },
                onRobotsRequested = { setScreen<RobotFleetScreen>() },
                onStrategyRequested = { setScreen<StrategyLabScreen>() },
            ),
        )
        addScreen(MeteorShowerScreen(services) { setScreen<GameplayHubScreen>() })
        addScreen(RobotFleetScreen(services) { setScreen<GameplayHubScreen>() })
        addScreen(StrategyLabScreen(services) { setScreen<GameplayHubScreen>() })
        gameplayScreensAdded = true
    }

    private companion object { const val TAG = "MinerSpaceGame" }
}
