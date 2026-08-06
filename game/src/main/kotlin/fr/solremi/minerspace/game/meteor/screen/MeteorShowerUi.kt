package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.ScreenUtils
import fr.solremi.minerspace.domain.event.MeteorEventDefinition
import fr.solremi.minerspace.domain.event.MeteorEventEngine
import fr.solremi.minerspace.domain.event.MeteorEventPhase
import fr.solremi.minerspace.domain.event.MeteorEventState
import fr.solremi.minerspace.domain.event.MeteorFragmentKind
import kotlin.math.max

data class MeteorShowerLayout(
    val top: Rectangle,
    val bottom: Rectangle,
    val play: Rectangle,
    val assist: Rectangle,
    val ad: Rectangle,
    val codex: Rectangle,
    val action: Rectangle,
)

object MeteorShowerLayoutCalculator {
    fun calculate(width: Float, height: Float): MeteorShowerLayout {
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat()
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 50f, right - left, 50f)
        val bottomBar = Rectangle(left, bottom, right - left, 50f)
        val play = Rectangle(
            left,
            bottom + 56f,
            right - left,
            (top - bottom - 112f).coerceAtLeast(120f),
        )
        val gap = 6f
        val action = Rectangle(right - 104f, bottom, 104f, 48f)
        val codex = Rectangle(action.x - gap - 76f, bottom, 76f, 48f)
        val ad = Rectangle(codex.x - gap - 94f, bottom, 94f, 48f)
        val assist = Rectangle(ad.x - gap - 104f, bottom, 104f, 48f)
        return MeteorShowerLayout(topBar, bottomBar, play, assist, ad, codex, action)
    }
}

data class MeteorShowerViewModel(
    val event: MeteorEventState,
    val message: String,
    val codexOpen: Boolean,
    val transactionBlocked: Boolean,
    val adBusy: Boolean,
    val adAvailable: Boolean,
    val actionLabel: String,
)

class MeteorShowerRenderer {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.82f) }
    private val small = BitmapFont().apply { data.setScale(.64f) }

    fun render(
        camera: OrthographicCamera,
        width: Float,
        height: Float,
        layout: MeteorShowerLayout,
        model: MeteorShowerViewModel,
        definition: MeteorEventDefinition,
        engine: MeteorEventEngine,
    ) {
        ScreenUtils.clear(BACKGROUND)
        drawBackground(camera, width, height, layout)
        drawFragments(camera, layout, model.event, engine)
        drawHud(camera, layout, model, definition)
        if (model.codexOpen) drawCodex(camera, layout, model.event)
    }

    fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
        small.dispose()
    }

    private fun drawBackground(
        camera: OrthographicCamera,
        width: Float,
        height: Float,
        layout: MeteorShowerLayout,
    ) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = SKY
        shapes.rect(0f, 0f, width, height)
        shapes.color = HORIZON
        shapes.rect(layout.play.x, layout.play.y, layout.play.width, layout.play.height * .24f)
        shapes.color = PLANET
        shapes.arc(
            layout.play.x + layout.play.width / 2f,
            layout.play.y - layout.play.height * .45f,
            layout.play.width * .62f,
            18f,
            144f,
            64,
        )
        shapes.color = STAR
        repeat(28) { index ->
            val x = layout.play.x + ((index * 83) % 997) / 997f * layout.play.width
            val y = layout.play.y + ((index * 47 + 31) % 991) / 991f * layout.play.height
            shapes.circle(x, y, if (index % 5 == 0) 1.5f else 1f, 8)
        }
        shapes.end()
    }

    private fun drawFragments(
        camera: OrthographicCamera,
        layout: MeteorShowerLayout,
        event: MeteorEventState,
        engine: MeteorEventEngine,
    ) {
        if (event.phase != MeteorEventPhase.ACTIVE) return
        val play = layout.play
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        event.fragments.forEach { fragment ->
            val point = engine.position(fragment, event.elapsedActiveMillis)
            val x = play.x + point.xMillionths / NORMALIZED * play.width
            val y = play.y + point.yMillionths / NORMALIZED * play.height
            if (
                x !in play.x - 30f..play.x + play.width + 30f ||
                y !in play.y - 30f..play.y + play.height + 30f
            ) return@forEach
            val rare = fragment.kind == MeteorFragmentKind.RARE
            shapes.color = if (rare) RARE_TRAIL else STANDARD_TRAIL
            shapes.rectLine(x + 30f, y + 18f, x, y, if (rare) 5f else 3f)
            shapes.color = if (rare) RARE_GLOW else STANDARD_GLOW
            shapes.circle(x, y, if (rare) 20f else 13f, 20)
            shapes.color = if (rare) RARE_CORE else STANDARD_CORE
            shapes.circle(x, y, if (rare) 10f else 7f, 16)
        }
        shapes.end()
    }

    private fun drawHud(
        camera: OrthographicCamera,
        layout: MeteorShowerLayout,
        model: MeteorShowerViewModel,
        definition: MeteorEventDefinition,
    ) {
        val event = model.event
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HUD
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.rect(layout.bottom.x, layout.bottom.y, layout.bottom.width, layout.bottom.height)
        drawButton(layout.assist, event.phase == MeteorEventPhase.ACTIVE && !model.transactionBlocked)
        drawButton(layout.ad, model.adAvailable && !model.adBusy && !model.transactionBlocked)
        drawButton(layout.codex, !model.transactionBlocked)
        drawButton(layout.action, true)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        val remaining = ((definition.durationMillis - event.elapsedActiveMillis).coerceAtLeast(0L) + 999L) / 1_000L
        font.draw(batch, "PLUIE MÉTÉORIQUE · ${remaining}s", layout.top.x + 12f, layout.top.y + layout.top.height - 14f)
        small.color = MUTED
        small.draw(batch, "Fragments ${event.standardCollected} · Cœurs ${event.rareCollected} · actifs ${event.fragments.size}/${definition.maxActiveFragments}", layout.top.x + 12f, layout.top.y + 15f)
        small.color = TEXT
        small.draw(batch, if (event.assistanceEnabled) "ASSIST. OUI" else "ASSIST. NON", layout.assist.x + 7f, layout.assist.y + 30f)
        small.draw(batch, if (model.adBusy) "PUB..." else "PUB +15S", layout.ad.x + 9f, layout.ad.y + 30f)
        small.draw(batch, "CODEX", layout.codex.x + 14f, layout.codex.y + 30f)
        small.draw(batch, model.actionLabel, layout.action.x + 10f, layout.action.y + 30f)
        if (event.phase != MeteorEventPhase.ACTIVE) {
            drawSummaryText(layout, event, model.message)
        } else {
            small.color = MUTED
            small.draw(batch, model.message, layout.bottom.x + 8f, layout.bottom.y + layout.bottom.height - 10f)
        }
        batch.end()
    }

    private fun drawSummaryText(
        layout: MeteorShowerLayout,
        event: MeteorEventState,
        message: String,
    ) {
        val panel = layout.play
        font.color = TEXT
        font.draw(batch, "RÉSUMÉ", panel.x + 24f, panel.y + panel.height - 28f)
        small.color = MUTED
        small.draw(batch, "Fragments standards : ${event.standardCollected}", panel.x + 24f, panel.y + panel.height - 62f)
        small.draw(batch, "Cœur météorique rare : ${event.rareCollected}", panel.x + 24f, panel.y + panel.height - 86f)
        small.draw(batch, "Entrées Codex : ${event.codexEntryIds.size}/3", panel.x + 24f, panel.y + panel.height - 110f)
        small.draw(batch, message, panel.x + 24f, panel.y + 28f)
    }

    private fun drawCodex(
        camera: OrthographicCamera,
        layout: MeteorShowerLayout,
        event: MeteorEventState,
    ) {
        val play = layout.play
        val panel = Rectangle(
            play.x + play.width * .12f,
            play.y + play.height * .10f,
            play.width * .76f,
            play.height * .80f,
        )
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = CODEX_BG
        shapes.rect(panel.x, panel.y, panel.width, panel.height)
        shapes.color = ACCENT
        shapes.rect(panel.x, panel.y + panel.height - 4f, panel.width, 4f)
        shapes.end()
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "CODEX TEMPORAIRE", panel.x + 18f, panel.y + panel.height - 22f)
        codexLine(panel, 54f, "Pluie météorique", MeteorEventEngine.CODEX_EVENT in event.codexEntryIds)
        codexLine(panel, 82f, "Fragment standard", MeteorEventEngine.CODEX_STANDARD in event.codexEntryIds)
        codexLine(panel, 110f, "Cœur météorique", MeteorEventEngine.CODEX_RARE in event.codexEntryIds)
        small.color = MUTED
        small.draw(batch, "Touchez CODEX pour fermer", panel.x + 18f, panel.y + 18f)
        batch.end()
    }

    private fun codexLine(panel: Rectangle, offset: Float, label: String, discovered: Boolean) {
        small.color = if (discovered) TEXT else MUTED
        small.draw(batch, (if (discovered) "DÉCOUVERT · " else "INCONNU · ") + label, panel.x + 18f, panel.y + panel.height - offset)
    }

    private fun drawButton(rect: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) ACCENT else BORDER
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private companion object {
        const val NORMALIZED = 1_000_000f
        val BACKGROUND = Color(.004f, .008f, .025f, 1f)
        val SKY = Color(.012f, .025f, .070f, 1f)
        val HORIZON = Color(.070f, .055f, .080f, 1f)
        val PLANET = Color(.18f, .085f, .045f, 1f)
        val STAR = Color(.68f, .78f, .92f, 1f)
        val STANDARD_TRAIL = Color(.18f, .56f, .86f, .7f)
        val STANDARD_GLOW = Color(.20f, .72f, .98f, 1f)
        val STANDARD_CORE = Color(.78f, .94f, 1f, 1f)
        val RARE_TRAIL = Color(.76f, .34f, .95f, .8f)
        val RARE_GLOW = Color(.92f, .48f, 1f, 1f)
        val RARE_CORE = Color(1f, .88f, .34f, 1f)
        val HUD = Color(.020f, .045f, .085f, .96f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val DISABLED = Color(.045f, .065f, .085f, 1f)
        val BORDER = Color(.16f, .20f, .24f, 1f)
        val ACCENT = Color(.20f, .82f, .88f, 1f)
        val CODEX_BG = Color(.025f, .050f, .090f, .98f)
        val TEXT = Color(.90f, .96f, 1f, 1f)
        val MUTED = Color(.61f, .72f, .82f, 1f)
    }
}
