package fr.solremi.minerspace.game.navigation

import fr.solremi.minerspace.domain.prestige.PlanetId
import fr.solremi.minerspace.domain.prestige.PrestigeState

enum class InitialRoute {
    FERRUM,
    PLANET_TRANSFER,
    CRYOS,
    FRONTIER,
}

object InitialRouteResolver {
    fun resolve(prestige: PrestigeState, hasActiveFrontierWorld: Boolean): InitialRoute = when {
        prestige.pendingTransfer != null -> InitialRoute.PLANET_TRANSFER
        prestige.activePlanet == PlanetId.CRYOS_IX && hasActiveFrontierWorld -> InitialRoute.FRONTIER
        prestige.activePlanet == PlanetId.CRYOS_IX -> InitialRoute.CRYOS
        else -> InitialRoute.FERRUM
    }
}
