package fr.solremi.minerspace.game.ferrum.text

import fr.solremi.minerspace.game.ferrum.model.FerrumColonyStage
import fr.solremi.minerspace.game.ferrum.model.FerrumIds
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ui.FerrumPrimaryDestination
import fr.solremi.minerspace.game.ui.FerrumSecondaryDestination
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.text.TemplateTextCatalog

enum class FerrumAdviceKey {
    INITIAL_TITLE,
    INITIAL_DETAIL,
    PRODUCTION_READY_TITLE,
    PRODUCTION_READY_DETAIL,
    ASSEMBLY_READY_TITLE,
    ASSEMBLY_READY_DETAIL,
    REFUND_TITLE,
    REFUND_DETAIL,
    IRON_TITLE,
    IRON_DETAIL,
    COPPER_TITLE,
    COPPER_DETAIL,
    IRON_REFINING_TITLE,
    IRON_REFINING_READY,
    COPPER_REFINING_TITLE,
    COPPER_REFINING_READY,
    POWER_CELL_TITLE,
    POWER_CELL_READY,
    SENSOR_TITLE,
    SENSOR_READY,
    INSTALL_TECH_TITLE,
    INSTALL_TECH_DETAIL,
    BUILD_TECH_TITLE,
    BUILD_TECH_READY,
    COMPLETE_TITLE,
    COMPLETE_RUNNING,
    COMPLETE_IDLE,
    MISSING_INPUTS,
}

interface FerrumTextCatalog {
    val title: String
    val initialMessage: String
    val noRecipe: String
    val noTask: String

    fun primaryDestination(destination: FerrumPrimaryDestination): String
    fun secondaryDestination(destination: FerrumSecondaryDestination): String
    fun stageName(stage: FerrumColonyStage): String
    fun stageAnnouncement(stage: FerrumColonyStage): String
    fun progressLabel(phase: Int, total: Int): String
    fun saveStatus(secondsSinceSave: Long?): String
    fun selectionTitle(node: FerrumNodeId?): String
    fun economyLine(spaceDollars: Long, iron: Long, copper: Long, crystal: Long): String
    fun recipeLabel(recipeId: GameId?): String
    fun actionLabel(node: FerrumNodeId?): String
    fun taskLabel(node: FerrumNodeId?, ready: Boolean, cancellable: Boolean, installable: Boolean): String
    fun utilityLabel(productionNode: Boolean, batchLabel: String): String
    fun refiningRecipeName(recipeId: GameId): String
    fun assemblyRecipeName(recipeId: GameId): String
    fun resourceName(resourceId: GameId): String
    fun refiningReady(count: Int): String
    fun refiningRunning(name: String, remainingSeconds: Long): String
    fun queue(name: String, size: Int, capacity: Int): String
    fun technologyRequired(name: String): String
    fun batchSelected(label: String): String
    fun batchLaunched(count: Int): String
    fun collectedLots(count: Int): String
    fun advice(key: FerrumAdviceKey, arguments: Map<String, Any?> = emptyMap()): String
}

object FrenchFerrumText : FerrumTextCatalog {
    override val title: String = "FERRUM DELTA"
    override val initialMessage: String = "NOVA · Touchez le conseil pour rejoindre la prochaine action."
    override val noRecipe: String = "Aucune recette disponible"
    override val noTask: String = "Aucune tâche disponible."

    override fun primaryDestination(destination: FerrumPrimaryDestination): String = when (destination) {
        FerrumPrimaryDestination.EXPLORATION -> "EXPLORER"
        FerrumPrimaryDestination.FLEET -> "FLOTTE"
        FerrumPrimaryDestination.MISSIONS -> "MISSIONS"
        FerrumPrimaryDestination.MENU -> "MENU"
    }

    override fun secondaryDestination(destination: FerrumSecondaryDestination): String = when (destination) {
        FerrumSecondaryDestination.STRATEGY -> "STRATÉGIE"
        FerrumSecondaryDestination.ARCHIVES -> "ARCHIVES"
        FerrumSecondaryDestination.SETTINGS -> "RÉGLAGES"
        FerrumSecondaryDestination.TRANSFER -> "DÉPART"
        FerrumSecondaryDestination.BONUS -> "BONUS"
    }

    override fun stageName(stage: FerrumColonyStage): String = when (stage) {
        FerrumColonyStage.OUTPOST -> "AVANT-POSTE"
        FerrumColonyStage.INDUSTRIAL -> "COMPLEXE INDUSTRIEL"
        FerrumColonyStage.NETWORKED -> "RÉSEAU AUTOMATISÉ"
        FerrumColonyStage.ORBITAL -> "CHANTIER ORBITAL"
    }

    override fun stageAnnouncement(stage: FerrumColonyStage): String =
        "NOVA · La colonie atteint le stade ${stageName(stage)}."

    override fun progressLabel(phase: Int, total: Int): String = "DÉMARRAGE $phase/$total"

    override fun saveStatus(secondsSinceSave: Long?): String = when {
        secondsSinceSave == null -> "NON SAUVEGARDÉ"
        secondsSinceSave < 5L -> "SAUVEGARDÉ"
        secondsSinceSave < 60L -> "SAUV. ${secondsSinceSave}s"
        else -> "SAUV. ${secondsSinceSave / 60L}min"
    }

    override fun selectionTitle(node: FerrumNodeId?): String = when (node) {
        FerrumNodeId.BASE -> "Base Delta"
        FerrumNodeId.REFINER -> "Rhea · Raffinerie"
        FerrumNodeId.ASSEMBLER -> "Kestrel · Assembleur"
        FerrumNodeId.IRON_DEPOSIT -> "Aster · Gisement de fer"
        FerrumNodeId.COPPER_DEPOSIT -> "Gisement de cuivre"
        FerrumNodeId.CRYSTAL_DEPOSIT -> "Gisement de cristal"
        null -> "Vue générale"
    }

    override fun economyLine(spaceDollars: Long, iron: Long, copper: Long, crystal: Long): String =
        "$spaceDollars SD · fer $iron · cuivre $copper · cristal $crystal"

    override fun recipeLabel(recipeId: GameId?): String = when (recipeId) {
        FerrumIds.RECIPE_IRON -> "FER"
        FerrumIds.RECIPE_COPPER -> "CUIVRE"
        FerrumIds.ASSEMBLY_POWER_CELL -> "PILE"
        FerrumIds.ASSEMBLY_SENSOR_ARRAY -> "CAPTEUR"
        FerrumIds.ASSEMBLY_TECH_EXTRACTION -> "PROTOCOLE"
        FerrumIds.ASSEMBLY_TECH_SORTING -> "TRI Q."
        else -> "RECETTE"
    }

    override fun actionLabel(node: FerrumNodeId?): String = when (node) {
        FerrumNodeId.BASE -> "VENDRE"
        FerrumNodeId.REFINER, FerrumNodeId.ASSEMBLER -> "LANCER"
        FerrumNodeId.IRON_DEPOSIT, FerrumNodeId.COPPER_DEPOSIT, FerrumNodeId.CRYSTAL_DEPOSIT -> "PRENDRE"
        null -> "ACTION"
    }

    override fun taskLabel(
        node: FerrumNodeId?,
        ready: Boolean,
        cancellable: Boolean,
        installable: Boolean,
    ): String = when (node) {
        FerrumNodeId.BASE -> "TOUT PRENDRE"
        FerrumNodeId.REFINER -> if (ready) "RÉCUPÉRER" else if (cancellable) "ANNULER" else "TÂCHE"
        FerrumNodeId.ASSEMBLER -> if (ready) "RÉCUPÉRER" else if (installable) "INSTALLER" else "TÂCHE"
        else -> "TÂCHE"
    }

    override fun utilityLabel(productionNode: Boolean, batchLabel: String): String =
        if (productionNode) batchLabel else "CENTRER"

    override fun refiningRecipeName(recipeId: GameId): String = when (recipeId) {
        FerrumIds.RECIPE_IRON -> "Lingots de fer"
        FerrumIds.RECIPE_COPPER -> "Plaques de cuivre"
        else -> "Production"
    }

    override fun assemblyRecipeName(recipeId: GameId): String = when (recipeId) {
        FerrumIds.ASSEMBLY_POWER_CELL -> "Pile énergétique"
        FerrumIds.ASSEMBLY_SENSOR_ARRAY -> "Réseau de capteurs"
        FerrumIds.ASSEMBLY_TECH_EXTRACTION -> "Protocole d’extraction"
        FerrumIds.ASSEMBLY_TECH_SORTING -> "Tri quantique"
        else -> "Composant"
    }

    override fun resourceName(resourceId: GameId): String = when (resourceId) {
        FerrumIds.RAW_IRON -> "fer"
        FerrumIds.RAW_COPPER -> "cuivre"
        FerrumIds.RAW_CRYSTAL -> "cristal"
        FerrumIds.REFINED_IRON -> "lingot(s) de fer"
        FerrumIds.REFINED_COPPER -> "plaque(s) de cuivre"
        FerrumIds.POWER_CELL -> "pile(s) énergétique(s)"
        FerrumIds.SENSOR_ARRAY -> "réseau(x) de capteurs"
        else -> resourceId.value.replace('_', ' ')
    }

    override fun refiningReady(count: Int): String = "$count production(s) prête(s)"
    override fun refiningRunning(name: String, remainingSeconds: Long): String = "$name · $remainingSeconds s"
    override fun queue(name: String, size: Int, capacity: Int): String = "$name · file $size/$capacity"
    override fun technologyRequired(name: String): String = "$name · technologie préalable requise"
    override fun batchSelected(label: String): String = "Lot de production : $label."
    override fun batchLaunched(count: Int): String = "$count production(s) ajoutée(s) à la file."
    override fun collectedLots(count: Int): String =
        if (count > 0) "$count lot(s) récupéré(s)." else "Aucune production prête."

    override fun advice(key: FerrumAdviceKey, arguments: Map<String, Any?>): String =
        adviceCatalog.text(key, arguments)

    private val adviceCatalog = TemplateTextCatalog(
        FerrumAdviceKey.entries.toSet(),
        mapOf(
            FerrumAdviceKey.INITIAL_TITLE to "Initialisation",
            FerrumAdviceKey.INITIAL_DETAIL to "NOVA analyse la chaîne de production.",
            FerrumAdviceKey.PRODUCTION_READY_TITLE to "Production terminée",
            FerrumAdviceKey.PRODUCTION_READY_DETAIL to "{name} prête dans le raffineur. Touchez ici pour vous y rendre.",
            FerrumAdviceKey.ASSEMBLY_READY_TITLE to "Assemblage terminé",
            FerrumAdviceKey.ASSEMBLY_READY_DETAIL to "{name} prêt dans l’assembleur.",
            FerrumAdviceKey.REFUND_TITLE to "Ressources remboursées",
            FerrumAdviceKey.REFUND_DETAIL to "La base conserve des matériaux issus d’une production annulée.",
            FerrumAdviceKey.IRON_TITLE to "Réveiller Aster",
            FerrumAdviceKey.IRON_DETAIL to "Collectez 10 unités de fer pour relancer la première chaîne de Ferrum Delta.",
            FerrumAdviceKey.COPPER_TITLE to "Rétablir le circuit cuivre",
            FerrumAdviceKey.COPPER_DETAIL to "Collectez 6 unités de cuivre. Rhea en a besoin pour stabiliser la raffinerie.",
            FerrumAdviceKey.IRON_REFINING_TITLE to "Produire les premiers lingots",
            FerrumAdviceKey.IRON_REFINING_READY to "Lancez un raffinage de fer.",
            FerrumAdviceKey.COPPER_REFINING_TITLE to "Préparer les plaques conductrices",
            FerrumAdviceKey.COPPER_REFINING_READY to "Lancez un raffinage de cuivre.",
            FerrumAdviceKey.POWER_CELL_TITLE to "Assembler une pile énergétique",
            FerrumAdviceKey.POWER_CELL_READY to "Kestrel peut assembler la première pile.",
            FerrumAdviceKey.SENSOR_TITLE to "Donner des yeux à la base",
            FerrumAdviceKey.SENSOR_READY to "Assemblez le premier réseau de capteurs.",
            FerrumAdviceKey.INSTALL_TECH_TITLE to "Installer le protocole d’extraction",
            FerrumAdviceKey.INSTALL_TECH_DETAIL to "Le module est prêt. Installez-le pour accélérer toutes les foreuses.",
            FerrumAdviceKey.BUILD_TECH_TITLE to "Compiler le protocole d’extraction",
            FerrumAdviceKey.BUILD_TECH_READY to "Fabriquez le module technologique puis récupérez-le.",
            FerrumAdviceKey.COMPLETE_TITLE to "Chaîne initiale opérationnelle",
            FerrumAdviceKey.COMPLETE_RUNNING to "{count} tâche(s) en cours. Le prochain objectif se trouve dans Missions.",
            FerrumAdviceKey.COMPLETE_IDLE to "Aster, Rhea et Kestrel attendent vos ordres. Développez maintenant la colonie.",
            FerrumAdviceKey.MISSING_INPUTS to "Il manque {items}.",
        ),
    )
}
