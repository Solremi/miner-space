package fr.solremi.minerspace.game.ferrum.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray

class FerrumCameraController(
    val camera: OrthographicCamera,
) {
    fun recenter() {
        camera.position.set(12.5f, 13f, 12.5f)
        camera.up.set(Vector3.Y)
        camera.lookAt(0f, 0f, 0f)
        camera.near = 0.1f
        camera.far = 80f
        camera.zoom = 1f
        camera.update()
    }

    fun pan(deltaX: Float, deltaY: Float) {
        val scale = 0.018f * camera.zoom
        camera.translate(-deltaX * scale, 0f, -deltaY * scale)
        camera.position.x = camera.position.x.coerceIn(7f, 17f)
        camera.position.z = camera.position.z.coerceIn(7f, 17f)
        camera.update()
    }

    fun zoom(previousDistance: Float, currentDistance: Float) {
        if (currentDistance <= 0f) return
        camera.zoom = (camera.zoom * previousDistance / currentDistance).coerceIn(0.65f, 1.45f)
        camera.update()
    }

    fun pickRay(screenX: Float, screenY: Float): Ray = camera.getPickRay(
        screenX,
        screenY,
        0f,
        0f,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat(),
    )
}
