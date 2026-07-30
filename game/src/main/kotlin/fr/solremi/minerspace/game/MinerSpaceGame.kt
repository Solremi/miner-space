package fr.solremi.minerspace.game

import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.screen.FatalErrorScreen
import fr.solremi.minerspace.game.screen.ManufacturingPlanetScreen
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MinerSpaceGame(
    private val services: GameServices,
    private val logger: GameLogger = SilentGameLogger,
) : KtxGame<KtxScreen>() {
    override fun create() {
        try {
            logger.info(TAG, "Starting Miner Space manufacturing vertical slice.")
            addScreen(ManufacturingPlanetScreen(services))
            setScreen<ManufacturingPlanetScreen>()
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
