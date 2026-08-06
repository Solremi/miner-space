package fr.solremi.minerspace.game.ferrum.input

import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

class FerrumInputController(
    private val cameraController: FerrumCameraController,
    private val hudPoint: (Float, Float) -> Vector2,
    private val isHudRegion: (Vector2) -> Boolean,
    private val onHudTap: (Vector2) -> Boolean,
    private val onWorldTap: (Float, Float) -> Unit,
) : GestureDetector.GestureAdapter() {
    private var startedOnHud = false
    private var previousZoomDistance = 0f

    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        startedOnHud = isHudRegion(hudPoint(x, y))
        previousZoomDistance = 0f
        return true
    }

    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
        if (onHudTap(hudPoint(x, y))) return true
        onWorldTap(x, y)
        return true
    }

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        if (startedOnHud) return false
        cameraController.pan(deltaX, deltaY)
        return true
    }

    override fun zoom(initialDistance: Float, distance: Float): Boolean {
        if (startedOnHud || distance <= MathUtils.FLOAT_ROUNDING_ERROR) return false
        val baseline = if (previousZoomDistance <= 0f) initialDistance else previousZoomDistance
        cameraController.zoom(baseline, distance)
        previousZoomDistance = distance
        return true
    }

    override fun pinchStop() {
        previousZoomDistance = 0f
    }
}
