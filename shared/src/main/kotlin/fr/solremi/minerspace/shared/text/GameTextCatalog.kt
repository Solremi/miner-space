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
    ),
)
