package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingActionResult
import fr.solremi.minerspace.game.text.GameplayText
import fr.solremi.minerspace.shared.text.FrenchGameText
import fr.solremi.minerspace.shared.text.GameTextKey

enum class FerrumFeedbackKind { NONE, IMPACT, SUCCESS, WARNING }

data class FerrumActionFeedback(
    val message: String,
    val kind: FerrumFeedbackKind = FerrumFeedbackKind.NONE,
)

internal class ManufacturingFeedbackMapper {
    fun map(result: ManufacturingActionResult): FerrumActionFeedback = when (result) {
        is ManufacturingActionResult.Applied -> FerrumActionFeedback(
            GameplayText.manufacturingSuccess(result.reason),
            FerrumFeedbackKind.SUCCESS,
        )
        is ManufacturingActionResult.Rejected -> FerrumActionFeedback(
            GameplayText.manufacturingError(result.code),
            FerrumFeedbackKind.WARNING,
        )
        is ManufacturingActionResult.PersistenceFailed -> FerrumActionFeedback(
            FrenchGameText.text(GameTextKey.ACTION_CANCELLED_SAVE_UNAVAILABLE),
            FerrumFeedbackKind.WARNING,
        )
    }
}
