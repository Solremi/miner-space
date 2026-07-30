package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.assembly.AssemblyContentLoader
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.refining.RefiningContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.ManufacturingStateMigrator
import fr.solremi.minerspace.domain.assembly.AssemblyEngine
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.simulation.offline.OfflineProgressEngine
import fr.solremi.minerspace.simulation.offline.OfflineProgressReport
import ktx.app.KtxScreen
import kotlin.math.max

class OfflineReturnScreen(
    private val services: GameServices,
    private val onContinue: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val titleFont = BitmapFont().apply { data.setScale(1.15f) }
    private val font = BitmapFont().apply { data.setScale(0.82f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.70f) }
    private var continued = false
    private val preparation = prepareOfflineProgress()

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = com.badlogic.gdx.math.Vector2(screenX.toFloat(), screenY.toFloat())
            viewport.unproject(point)
            if (layout().continueButton.contains(point)) continueToGame()
            return true
        }
    }

    override fun show() {
        if (!preparation.shouldShow) {
            Gdx.app.postRunnable(::continueToGame)
        } else {
            Gdx.input.inputProcessor = input
        }
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        if (!preparation.shouldShow) return
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        val layout = layout()

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = PANEL_SHADOW
        shapes.rect(layout.panel.x + 10f, layout.panel.y - 10f, layout.panel.width, layout.panel.height)
        shapes.color = PANEL
        shapes.rect(layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height)
        shapes.color = ACCENT
        shapes.rect(layout.panel.x, layout.panel.y + layout.panel.height - 5f, layout.panel.width, 5f)
        shapes.color = BUTTON
        shapes.rect(
            layout.continueButton.x,
            layout.continueButton.y,
            layout.continueButton.width,
            layout.continueButton.height,
        )
        shapes.color = ACCENT
        shapes.rect(layout.continueButton.x, layout.continueButton.y, layout.continueButton.width, 4f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = TEXT
        titleFont.draw(batch, "RETOUR SUR FERRUM DELTA", layout.panel.x + 22f, layout.panel.y + layout.panel.height - 28f)
        font.color = TEXT
        smallFont.color = MUTED
        val lines = preparation.lines()
        var y = layout.panel.y + layout.panel.height - 66f
        lines.forEachIndexed { index, line ->
            (if (index == 0) font else smallFont).draw(batch, line, layout.panel.x + 22f, y)
            y -= if (index == 0) 28f else 22f
        }
        font.color = TEXT
        font.draw(batch, "CONTINUER", layout.continueButton.x + 25f, layout.continueButton.y + 31f)
        batch.end()
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        titleFont.dispose()
        font.dispose()
        smallFont.dispose()
    }

    private fun continueToGame() {
        if (continued) return
        continued = true
        hide()
        onContinue()
    }

    private fun prepareOfflineProgress(): Preparation {
        val payload = services.save.loadLatest() ?: return Preparation.empty()
        return runCatching {
            val economyDefinitions = CoreEconomyContentLoader().load(services.content)
            val refiningDefinitions = RefiningContentLoader().load(services.content)
            val assemblyDefinitions = AssemblyContentLoader().load(services.content)
            require(refiningDefinitions.contentVersion == economyDefinitions.contentVersion)
            require(assemblyDefinitions.contentVersion == economyDefinitions.contentVersion)

            val codec = ManufacturingSnapshotCodec()
            val decoded = codec.decodeWithMetadata(payload)
            val migrated = ManufacturingStateMigrator(
                economyDefinitions,
                refiningDefinitions,
                assemblyDefinitions,
            ).migrate(decoded.state)
            val economy = CoreEconomyEngine(economyDefinitions)
            val refiner = RefiningEngine(
                refiningDefinitions,
                economyDefinitions.resources.mapValues { it.value.storageCapacity },
            )
            val assembler = AssemblyEngine(
                assemblyDefinitions,
                economyDefinitions.resources.mapValues { it.value.storageCapacity },
            )
            economy.requireValid(migrated.state.economy)
            val now = services.clock.nowEpochMillis().coerceAtLeast(0L)
            val offline = OfflineProgressEngine(economy, refiner, assembler).apply(
                state = migrated.state,
                savedAtEpochMillis = payload.savedAtEpochMillis,
                nowEpochMillis = now,
            )
            val rewrite = decoded.requiresRewrite || migrated.changed ||
                payload.contentVersion != economyDefinitions.contentVersion ||
                payload.recoveredFromFallback || offline.report.simulatedSeconds > 0L
            val writeSucceeded = !rewrite || services.save.save(
                codec.encode(
                    offline.state,
                    economyDefinitions.contentVersion,
                    savedAtEpochMillis = now,
                ),
            ) == SaveWriteStatus.WRITTEN
            Preparation(
                report = offline.report,
                recoveredOlderSnapshot = payload.recoveredFromFallback,
                migrated = decoded.requiresRewrite || migrated.changed ||
                    payload.contentVersion != economyDefinitions.contentVersion,
                saveFailed = !writeSucceeded,
                unrecoverable = false,
            )
        }.getOrElse {
            services.save.clear()
            Preparation(
                report = null,
                recoveredOlderSnapshot = false,
                migrated = false,
                saveFailed = false,
                unrecoverable = true,
            )
        }
    }

    private fun layout(): ReturnLayout {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat()
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        val safeLeft = Gdx.graphics.safeInsetLeft * scaleX + 12f
        val safeRight = Gdx.graphics.safeInsetRight * scaleX + 12f
        val safeTop = Gdx.graphics.safeInsetTop * scaleY + 12f
        val safeBottom = Gdx.graphics.safeInsetBottom * scaleY + 12f
        val availableWidth = max(1f, width - safeLeft - safeRight)
        val availableHeight = max(1f, height - safeTop - safeBottom)
        val panelWidth = availableWidth.coerceAtMost(620f)
        val panelHeight = availableHeight.coerceAtMost(300f)
        val panel = Rectangle(
            safeLeft + (availableWidth - panelWidth) / 2f,
            safeBottom + (availableHeight - panelHeight) / 2f,
            panelWidth,
            panelHeight,
        )
        val button = Rectangle(panel.x + panel.width - 154f, panel.y + 18f, 132f, 48f)
        return ReturnLayout(panel, button)
    }

    private data class ReturnLayout(
        val panel: Rectangle,
        val continueButton: Rectangle,
    )

    private data class Preparation(
        val report: OfflineProgressReport?,
        val recoveredOlderSnapshot: Boolean,
        val migrated: Boolean,
        val saveFailed: Boolean,
        val unrecoverable: Boolean,
    ) {
        val shouldShow: Boolean
            get() = unrecoverable || recoveredOlderSnapshot || migrated || saveFailed ||
                report?.hasMeaningfulProgress == true

        fun lines(): List<String> {
            if (unrecoverable) {
                return listOf(
                    "Aucune sauvegarde valide n’a pu être restaurée.",
                    "Une nouvelle partie sera utilisée.",
                    "Aucune ressource partielle n’a été attribuée.",
                )
            }
            val report = report
            val output = mutableListOf<String>()
            if (report != null) {
                output += "Absence : ${formatDuration(report.absentSeconds)} · simulée : ${formatDuration(report.simulatedSeconds)}"
                val extracted = report.extractedByResource.values.sum()
                output += "Extraction hors ligne : $extracted unité(s)"
                output += "Productions terminées : RF ${report.refiningCompleted} · AS ${report.assemblyCompleted}"
                if (report.depletedDepositIds.isNotEmpty()) output += "${report.depletedDepositIds.size} gisement(s) épuisé(s)"
                if (report.storageBlockedDepositIds.isNotEmpty()) output += "Production arrêtée par stockage ou transport plein"
                if (report.capped) output += "Progression plafonnée à 8 heures"
                if (report.clockMovedBackward) output += "Horloge modifiée : aucun gain excessif appliqué"
            }
            if (recoveredOlderSnapshot) output += "La dernière copie valide de la sauvegarde a été restaurée"
            if (migrated) output += "Sauvegarde mise à niveau vers le format actuel"
            if (saveFailed) output += "La progression est chargée, mais sa réécriture a échoué"
            return output.ifEmpty { listOf("Progression restaurée") }
        }

        companion object {
            fun empty(): Preparation = Preparation(null, false, false, false, false)

            private fun formatDuration(seconds: Long): String = when {
                seconds >= 3_600L -> "${seconds / 3_600L} h ${seconds % 3_600L / 60L} min"
                seconds >= 60L -> "${seconds / 60L} min"
                else -> "$seconds s"
            }
        }
    }

    private companion object {
        val BACKGROUND = Color(0.008f, 0.014f, 0.035f, 1f)
        val PANEL_SHADOW = Color(0.005f, 0.008f, 0.018f, 1f)
        val PANEL = Color(0.035f, 0.075f, 0.13f, 1f)
        val BUTTON = Color(0.08f, 0.18f, 0.26f, 1f)
        val ACCENT = Color(0.20f, 0.82f, 0.88f, 1f)
        val TEXT = Color(0.90f, 0.96f, 1f, 1f)
        val MUTED = Color(0.61f, 0.72f, 0.82f, 1f)
    }
}
