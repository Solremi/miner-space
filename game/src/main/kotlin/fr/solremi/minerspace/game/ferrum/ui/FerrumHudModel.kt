package fr.solremi.minerspace.game.ferrum.ui

data class FerrumHudModel(
    val title: String,
    val subtitle: String,
    val adviceHeading: String,
    val adviceDetail: String,
    val footer: String,
    val primaryLabels: List<String>,
    val secondaryLabels: List<String>,
    val recipeLabel: String,
    val actionLabel: String,
    val taskLabel: String,
    val utilityLabel: String,
    val menuOpen: Boolean,
    val recipeEnabled: Boolean,
    val actionEnabled: Boolean,
    val taskEnabled: Boolean,
)
