package fr.solremi.minerspace.game

import fr.solremi.minerspace.data.save.FrontierStateCodec
import fr.solremi.minerspace.data.save.PrestigeStateCodec
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.frontier.FrontierWorldStatus
import fr.solremi.minerspace.domain.prestige.PlanetPrestigeEngine
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.ferrum.screen.FerrumCommandScreen
import fr.solremi.minerspace.game.navigation.InitialRoute
import fr.solremi.minerspace.game.navigation.InitialRouteResolver
import fr.solremi.minerspace.game.presentation.PresentationController
import fr.solremi.minerspace.game.screen.*
import fr.solremi.minerspace.shared.GameLogger
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MinerSpaceGame(
    private val services: GameServices,
    private val logger: GameLogger = services.logger,
) : KtxGame<KtxScreen>() {
    private var added = false

    override fun create() {
        try {
            recoverPendingSaveTransaction()
            PresentationController.loadAndApply(services)
            addScreen(OfflineReturnScreen(services) { addScreens(); routeAfterOffline() })
            setScreen<OfflineReturnScreen>()
        } catch (failure: Throwable) {
            logger.error(TAG, "Unable to create the initial scene.", failure)
            addScreen(FatalErrorScreen("Erreur de démarrage", failure.message ?: failure::class.simpleName.orEmpty()))
            setScreen<FatalErrorScreen>()
        }
    }

    private fun recoverPendingSaveTransaction() {
        val recovery = SaveTransactionCoordinator(services.save).recoverPending()
        when (recovery.status) {
            SaveTransactionStatus.NO_PENDING -> Unit
            SaveTransactionStatus.COMMITTED ->
                logger.info(TAG, "Recovered save transaction ${recovery.transactionId}.")
            else -> error(
                "Unable to recover save transaction " +
                    "${recovery.transactionId.orEmpty()} (${recovery.status}, ${recovery.failedSlotId.orEmpty()})",
            )
        }
    }

    private fun routeAfterOffline() {
        val initial = PlanetPrestigeEngine().initialState()
        val prestige = runCatching {
            services.save.loadLatest(PrestigeStateCodec.SLOT_ID)?.let(PrestigeStateCodec()::decode) ?: initial
        }.onFailure {
            logger.warning(TAG, "Unable to load prestige routing state; using Ferrum Delta.", it)
        }.getOrElse { initial }

        when (InitialRouteResolver.resolve(prestige, hasActiveFrontierWorld())) {
            InitialRoute.PLANET_TRANSFER -> setScreen<PlanetTransferScreen>()
            InitialRoute.FRONTIER -> setScreen<InterplanetaryFrontierScreen>()
            InitialRoute.CRYOS -> setScreen<CryosFrontierGatewayScreen>()
            InitialRoute.FERRUM -> setScreen<FerrumCommandScreen>()
        }
    }

    private fun hasActiveFrontierWorld(): Boolean = runCatching {
        val payload = services.save.loadLatest(FrontierStateCodec.SLOT_ID) ?: return@runCatching false
        FrontierStateCodec().decode(payload).worlds.values.any { it.status == FrontierWorldStatus.ACTIVE }
    }.onFailure {
        logger.warning(TAG, "Unable to load frontier routing state; returning to Cryos IX.", it)
    }.getOrDefault(false)

    private fun addScreens() {
        if (added) return
        addScreen(FerrumCommandScreen(
            services,
            { setScreen<MeteorShowerScreen>() },
            { setScreen<RobotFleetScreen>() },
            { setScreen<StrategyLabScreen>() },
            { setScreen<MissionControlScreen>() },
            { setScreen<NarrativeArchiveScreen>() },
            { setScreen<PresentationSettingsScreen>() },
            { setScreen<PlanetTransferScreen>() },
            { setScreen<RewardedAdsScreen>() },
        ))
        addScreen(MeteorShowerScreen(services) { setScreen<FerrumCommandScreen>() })
        addScreen(RobotFleetScreen(services) { setScreen<FerrumCommandScreen>() })
        addScreen(StrategyLabScreen(services) { setScreen<FerrumCommandScreen>() })
        addScreen(MissionControlScreen(services) { setScreen<FerrumCommandScreen>() })
        addScreen(NarrativeArchiveScreen(services) { setScreen<FerrumCommandScreen>() })
        addScreen(PresentationSettingsScreen(
            services,
            onLegalRequested = { setScreen<LegalInformationScreen>() },
            onBack = { setScreen<FerrumCommandScreen>() },
        ))
        addScreen(LegalInformationScreen(services) { setScreen<PresentationSettingsScreen>() })
        addScreen(RewardedAdsScreen(services) { setScreen<FerrumCommandScreen>() })
        addScreen(PlanetTransferScreen(
            services,
            onFerrumBack = { setScreen<FerrumCommandScreen>() },
            onCryosReady = { setScreen<CryosFrontierGatewayScreen>() },
        ))
        addScreen(CryosFrontierGatewayScreen(
            services,
            onTransferRequested = { setScreen<PlanetTransferScreen>() },
            onFrontierRequested = { setScreen<InterplanetaryFrontierScreen>() },
        ))
        addScreen(InterplanetaryFrontierScreen(services) { setScreen<CryosFrontierGatewayScreen>() })
        added = true
    }

    private companion object {
        const val TAG = "MinerSpaceGame"
    }
}
