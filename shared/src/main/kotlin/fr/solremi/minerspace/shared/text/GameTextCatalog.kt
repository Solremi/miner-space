package fr.solremi.minerspace.shared.text

enum class GameTextKey {
    ACTION_SAVED,
    ACTION_CANCELLED_SAVE_UNAVAILABLE,
    AUTOSAVE_DEFERRED,
    SAVE_DEFERRED,
    MATERIALS_INSUFFICIENT,
    STORAGE_FULL,
    TECHNOLOGY_LOCKED,
    TECHNOLOGY_ITEM_REQUIRED,
    MANUFACTURING_STOCK_SOLD,
    MANUFACTURING_COLLECTION_TRANSFERRED,
    MANUFACTURING_REFINING_STARTED,
    MANUFACTURING_REFINING_COLLECTED,
    MANUFACTURING_REFINING_CANCELLED,
    MANUFACTURING_ASSEMBLY_STARTED,
    MANUFACTURING_ASSEMBLY_COLLECTED,
    MANUFACTURING_TECHNOLOGY_INSTALLED,
    EXPLORATION_SCAN_REQUIRED,
    EXPLORATION_SCANNER_LOW,
    EXPLORATION_PATH_LOCKED,
    EXPLORATION_TECHNOLOGY_REQUIRED,
    EXPLORATION_MONEY_LOW,
    EXPLORATION_COMPONENTS_LOW,
    EXPLORATION_SECTOR_REVEALED,
    EXPLORATION_SECTOR_OPENED,
    EXPLORATION_TRANSACTION_PENDING,
    METEOR_TOUCH_HINT,
    METEOR_FINISHED,
    METEOR_STANDARD_CAPTURED,
    METEOR_RARE_CAPTURED,
    METEOR_REWARD_STORAGE_FULL,
    METEOR_REWARD_COMMITTED,
    METEOR_REWARD_PENDING,
    REWARDED_OPTIONAL,
    REWARDED_UNAVAILABLE,
    REWARDED_ALREADY_GRANTED,
    FERRUM_TITLE,
    FERRUM_INITIAL_MESSAGE,
    FERRUM_BASE,
    FERRUM_REFINER,
    FERRUM_ASSEMBLER,
    FERRUM_IRON_DEPOSIT,
    FERRUM_COPPER_DEPOSIT,
    FERRUM_CRYSTAL_DEPOSIT,
    FERRUM_NO_SELECTION,
    FERRUM_BASE_HELP,
    FERRUM_SELECTION_HINT,
    FERRUM_NO_REFINING_RECIPE,
    FERRUM_NO_ASSEMBLY_RECIPE,
    NAV_METEORS,
    NAV_ROBOTS,
    NAV_STRATEGY,
    NAV_MISSIONS,
    NAV_ARCHIVES,
    NAV_SETTINGS,
    NAV_DEPARTURE,
    NAV_ADS,
    BUTTON_RECIPE,
    BUTTON_ACTION,
    BUTTON_TASK,
    BUTTON_CENTER,
    BUTTON_SELL,
    BUTTON_LAUNCH,
    BUTTON_COLLECT,
    BUTTON_CANCEL,
    BUTTON_INSTALL,
    BUTTON_REFUNDS,
    ASSET_LOADING,
}

interface GameTextCatalog {
    fun text(key: GameTextKey, arguments: Map<String, Any?> = emptyMap()): String
}

class TemplateGameTextCatalog(
    private val templates: Map<GameTextKey, String>,
) : GameTextCatalog {
    init {
        require(templates.keys == GameTextKey.entries.toSet()) {
            val missing = GameTextKey.entries.filterNot(templates::containsKey)
            "Missing game text keys: $missing"
        }
    }

    override fun text(key: GameTextKey, arguments: Map<String, Any?>): String {
        val template = templates.getValue(key)
        val expected = PLACEHOLDER.findAll(template).map { it.groupValues[1] }.toSet()
        require(arguments.keys == expected) {
            "Arguments for $key must be $expected but were ${arguments.keys}"
        }
        return PLACEHOLDER.replace(template) { match ->
            arguments.getValue(match.groupValues[1]).toString()
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("\\{([a-z][a-zA-Z0-9]*)}")
    }
}

object FrenchGameText : GameTextCatalog by TemplateGameTextCatalog(
    mapOf(
        GameTextKey.ACTION_SAVED to "Action enregistrée",
        GameTextKey.ACTION_CANCELLED_SAVE_UNAVAILABLE to "Action annulée · sauvegarde indisponible",
        GameTextKey.AUTOSAVE_DEFERRED to "Sauvegarde automatique différée",
        GameTextKey.SAVE_DEFERRED to "Sauvegarde différée",
        GameTextKey.MATERIALS_INSUFFICIENT to "Matériaux insuffisants",
        GameTextKey.STORAGE_FULL to "Stockage plein · résultat conservé",
        GameTextKey.TECHNOLOGY_LOCKED to "Technologie verrouillée · installez le nœud précédent",
        GameTextKey.TECHNOLOGY_ITEM_REQUIRED to "Fabriquez puis collectez la technologie",
        GameTextKey.MANUFACTURING_STOCK_SOLD to "Stock vendu",
        GameTextKey.MANUFACTURING_COLLECTION_TRANSFERRED to "Collecte transférée",
        GameTextKey.MANUFACTURING_REFINING_STARTED to "Raffinage lancé · ingrédients réservés",
        GameTextKey.MANUFACTURING_REFINING_COLLECTED to "Matériau raffiné collecté",
        GameTextKey.MANUFACTURING_REFINING_CANCELLED to "Raffinage annulé · remboursement appliqué",
        GameTextKey.MANUFACTURING_ASSEMBLY_STARTED to "Assemblage lancé · composants réservés",
        GameTextKey.MANUFACTURING_ASSEMBLY_COLLECTED to "Production AS collectée",
        GameTextKey.MANUFACTURING_TECHNOLOGY_INSTALLED to "Technologie installée · effet appliqué",
        GameTextKey.EXPLORATION_SCAN_REQUIRED to "Scannez d’abord ce secteur",
        GameTextKey.EXPLORATION_SCANNER_LOW to "Scanner insuffisant",
        GameTextKey.EXPLORATION_PATH_LOCKED to "Ouvrez le secteur précédent",
        GameTextKey.EXPLORATION_TECHNOLOGY_REQUIRED to "Technologie requise",
        GameTextKey.EXPLORATION_MONEY_LOW to "SpaceDollars insuffisants",
        GameTextKey.EXPLORATION_COMPONENTS_LOW to "Composants insuffisants",
        GameTextKey.EXPLORATION_SECTOR_REVEALED to "Secteur révélé",
        GameTextKey.EXPLORATION_SECTOR_OPENED to "Secteur ouvert · {reason}",
        GameTextKey.EXPLORATION_TRANSACTION_PENDING to "Transaction interrompue · redémarrez pour reprendre",
        GameTextKey.METEOR_TOUCH_HINT to "Touchez ou glissez sur les fragments",
        GameTextKey.METEOR_FINISHED to "Pluie terminée · récompenses prêtes",
        GameTextKey.METEOR_STANDARD_CAPTURED to "Fragment récupéré",
        GameTextKey.METEOR_RARE_CAPTURED to "Cœur météorique récupéré",
        GameTextKey.METEOR_REWARD_STORAGE_FULL to "Stockage des récompenses insuffisant",
        GameTextKey.METEOR_REWARD_COMMITTED to "Récompenses attribuées une seule fois",
        GameTextKey.METEOR_REWARD_PENDING to "Finalisation interrompue · relancez ou redémarrez",
        GameTextKey.REWARDED_OPTIONAL to "La publicité récompensée reste facultative",
        GameTextKey.REWARDED_UNAVAILABLE to "Aucune publicité disponible · le jeu normal reste accessible",
        GameTextKey.REWARDED_ALREADY_GRANTED to "Récompense déjà attribuée",
        GameTextKey.FERRUM_TITLE to "FERRUM DELTA · VERTICAL SLICE 2.5D",
        GameTextKey.FERRUM_INITIAL_MESSAGE to "Chaîne de production Ferrum prête",
        GameTextKey.FERRUM_BASE to "Base Delta",
        GameTextKey.FERRUM_REFINER to "Raffineur RF-01",
        GameTextKey.FERRUM_ASSEMBLER to "Assembleur AS-01",
        GameTextKey.FERRUM_IRON_DEPOSIT to "Gisement de fer",
        GameTextKey.FERRUM_COPPER_DEPOSIT to "Gisement de cuivre",
        GameTextKey.FERRUM_CRYSTAL_DEPOSIT to "Gisement de cristal",
        GameTextKey.FERRUM_NO_SELECTION to "Aucune sélection",
        GameTextKey.FERRUM_BASE_HELP to "Vendez le stock ou récupérez les remboursements de raffinage.",
        GameTextKey.FERRUM_SELECTION_HINT to "Touchez une installation ou un gisement.",
        GameTextKey.FERRUM_NO_REFINING_RECIPE to "Aucune recette de raffinage disponible",
        GameTextKey.FERRUM_NO_ASSEMBLY_RECIPE to "Aucune recette d’assemblage disponible",
        GameTextKey.NAV_METEORS to "MÉT.",
        GameTextKey.NAV_ROBOTS to "ROBOTS",
        GameTextKey.NAV_STRATEGY to "STRAT.",
        GameTextKey.NAV_MISSIONS to "MISS.",
        GameTextKey.NAV_ARCHIVES to "ARCH.",
        GameTextKey.NAV_SETTINGS to "FX",
        GameTextKey.NAV_DEPARTURE to "DÉPART",
        GameTextKey.NAV_ADS to "PUB",
        GameTextKey.BUTTON_RECIPE to "RECETTE",
        GameTextKey.BUTTON_ACTION to "ACTION",
        GameTextKey.BUTTON_TASK to "TÂCHE",
        GameTextKey.BUTTON_CENTER to "CENTRER",
        GameTextKey.BUTTON_SELL to "VENDRE",
        GameTextKey.BUTTON_LAUNCH to "LANCER",
        GameTextKey.BUTTON_COLLECT to "COLLECTER",
        GameTextKey.BUTTON_CANCEL to "ANNULER",
        GameTextKey.BUTTON_INSTALL to "INSTALLER",
        GameTextKey.BUTTON_REFUNDS to "REMBOURS.",
        GameTextKey.ASSET_LOADING to "CHARGEMENT",
    ),
)
