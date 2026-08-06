package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle
import kotlin.math.max

enum class MissionControlTab(val label: String) {
    OBJECTIVES("OBJECTIFS"),
    CONTRACTS("CONTRATS"),
    CODEX("CODEX"),
}

data class MissionControlRowItem(
    val id: String,
    val title: String,
    val detail: String,
)

data class MissionControlLayout(
    val top: Rectangle,
    val tutorial: Rectangle,
    val rows: List<Rectangle>,
    val message: Rectangle,
    val tab: Rectangle,
    val action: Rectangle,
    val pin: Rectangle,
    val back: Rectangle,
)

object MissionControlLayoutCalculator {
    fun calculate(width: Float, height: Float): MissionControlLayout {
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1)
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 50f, right - left, 50f)
        val tutorial = Rectangle(left, topBar.y - 62f, right - left, 56f)
        val back = Rectangle(right - 92f, bottom, 92f, 48f)
        val pin = Rectangle(back.x - 92f, bottom, 86f, 48f)
        val action = Rectangle(pin.x - 98f, bottom, 92f, 48f)
        val tab = Rectangle(action.x - 94f, bottom, 88f, 48f)
        val message = Rectangle(left, bottom + 50f, right - left, 22f)
        val listBottom = message.y + message.height + 4f
        val listTop = tutorial.y - 6f
        val gap = 5f
        val rowHeight = ((listTop - listBottom - gap * 3f) / 4f).coerceAtLeast(38f)
        val rows = (0 until 4).map { index ->
            Rectangle(
                left,
                listTop - (index + 1) * rowHeight - index * gap,
                right - left,
                rowHeight,
            )
        }
        return MissionControlLayout(topBar, tutorial, rows, message, tab, action, pin, back)
    }
}

object MissionControlText {
    fun rejection(code: String): String = when (code) {
        "mission_incomplete" -> "Objectif incomplet"
        "contract_locked" -> "Contrat pas encore disponible"
        "contract_inventory_missing" -> "Stock insuffisant"
        "collection_incomplete" -> "Collection incomplète"
        else -> code
    }

    fun success(reason: String): String = when (reason) {
        "deliver_contract" -> "Contrat livré"
        "claim_collection" -> "Collection complétée"
        "claim_achievement" -> "Exploit validé"
        else -> "Mission validée"
    }
}
