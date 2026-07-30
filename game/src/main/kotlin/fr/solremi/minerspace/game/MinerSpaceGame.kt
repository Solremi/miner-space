package fr.solremi.minerspace.game

import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.screen.FatalErrorScreen
import fr.solremi.minerspace.game.screen.OfflineReturnScreen
import fr.solremi.minerspace.game.screen.SectorExplorationScreen
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MinerSpaceGame(
    private val services: GameServices,
    private val logger: GameLogger = SilentGameLogger,
) : KtxGame<KtxScreen>() {
    private var gameplayAdded = false

    override fun create() {
        try {
            logger.info(TAG, "Starting Miner Space save and exploration bootstrap.")
            addScreen(
                OfflineReturnScreen(services) {
                    if (!gameplayAdded) {
                        addScreen(SectorExplorationScreen(services))
                        gameplayAdded = true
                    }
                    setScreen<SectorExplorationScreen>()
                },
            )
            setScreen<OfflineReturnScreen>()
        } catch (failure: Throwable) {
            logger.error(TAG, "Unable to create the initial scene.", failure)
            addScreen(
                FatalErrorScreen(
                    title = "Erreur de démarrage",
                    details = failure.message ?: failure::class.simpleName.orEmpty(),
                ),
            )
            setScreen<FatalErrorScreen>()
        }
    }

    private companion object {
        const val TAG = "MinerSpaceGame"
    }
}
