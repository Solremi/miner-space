package fr.solremi.minerspace.game.scene

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import fr.solremi.minerspace.game.performance.RuntimePerformanceBudget
import kotlin.math.floor
import kotlin.math.sin

enum class FerrumNodeId {
    BASE,
    REFINER,
    ASSEMBLER,
    IRON_DEPOSIT,
    COPPER_DEPOSIT,
    CRYSTAL_DEPOSIT,
}

data class FerrumSceneNode(
    val id: FerrumNodeId,
    val position: Vector3,
    val pickingBounds: BoundingBox,
)

object FerrumSceneSpec {
    val nodes: List<FerrumSceneNode> = listOf(
        node(FerrumNodeId.BASE, 0f, 0f, 0f, 4.6f, 2.4f, 3.4f),
        node(FerrumNodeId.REFINER, 5.2f, 0f, 0.2f, 2.8f, 2.2f, 2.6f),
        node(FerrumNodeId.ASSEMBLER, -5.1f, 0f, 0.1f, 2.8f, 2.2f, 2.6f),
        node(FerrumNodeId.IRON_DEPOSIT, -7.4f, 0f, -4.3f, 2.3f, 1.8f, 2.3f),
        node(FerrumNodeId.COPPER_DEPOSIT, 7.5f, 0f, -4.1f, 2.5f, 1.9f, 2.5f),
        node(FerrumNodeId.CRYSTAL_DEPOSIT, 6.8f, 0f, 5.0f, 2.1f, 2.2f, 2.1f),
    )

    private fun node(
        id: FerrumNodeId,
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        depth: Float,
    ): FerrumSceneNode {
        val position = Vector3(x, y, z)
        val halfWidth = width / 2f
        val halfDepth = depth / 2f
        return FerrumSceneNode(
            id = id,
            position = position,
            pickingBounds = BoundingBox(
                Vector3(x - halfWidth, y, z - halfDepth),
                Vector3(x + halfWidth, y + height, z + halfDepth),
            ),
        )
    }
}

/**
 * Procedural 2.5D colony scene. The primitive geometry is intentionally kept
 * lightweight, while the visible infrastructure evolves with player progress.
 */
class FerrumPrimitiveScene {
    private val modelBatch = ModelBatch()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.42f, 0.48f, 0.58f, 1f))
        add(DirectionalLight().set(0.82f, 0.88f, 1f, -0.55f, -1f, -0.35f))
        add(DirectionalLight().set(0.24f, 0.35f, 0.52f, 0.65f, -0.35f, 0.45f))
    }
    private val builder = ModelBuilder()
    private val ownedModels = mutableListOf<Model>()
    private val staticInstances = mutableListOf<ModelInstance>()
    private val developmentInstances = linkedMapOf<FerrumColonyStage, List<ModelInstance>>()
    private val nodeInstances = linkedMapOf<FerrumNodeId, ModelInstance>()
    private val robots = mutableListOf<RobotPrimitive>()
    private val crates = mutableListOf<ModelInstance>()
    private val highlight: ModelInstance
    private val orbitalBeacon: ModelInstance
    private var selected: FerrumNodeId? = null

    init {
        staticInstances += box(0f, -0.35f, 0f, 24f, 0.5f, 17f, GROUND)
        staticInstances += box(0f, -0.05f, 0f, 4f, 0.12f, 16f, ROAD)
        staticInstances += box(0f, -0.04f, 0f, 22f, 0.10f, 2.2f, ROAD)
        staticInstances += box(-4.3f, -0.03f, -2.5f, 8.5f, 0.08f, 1.1f, ROAD)
        staticInstances += box(4.1f, -0.03f, -2.4f, 8.2f, 0.08f, 1.1f, ROAD)

        nodeInstances[FerrumNodeId.BASE] = box(0f, 1.15f, 0f, 4.6f, 2.3f, 3.4f, BASE)
        staticInstances += box(0f, 2.55f, 0f, 2.2f, 0.5f, 1.5f, WINDOW)
        staticInstances += cylinder(-1.45f, 2.1f, 0f, 0.72f, 1.9f, BASE_DARK)
        staticInstances += cylinder(1.45f, 2.1f, 0f, 0.72f, 1.9f, BASE_DARK)

        nodeInstances[FerrumNodeId.REFINER] = box(5.2f, 1.05f, 0.2f, 2.8f, 2.1f, 2.6f, REFINER)
        staticInstances += cylinder(5.2f, 2.45f, 0.2f, 0.75f, 0.7f, HOT)
        staticInstances += box(5.2f, 1.0f, 1.65f, 1.6f, 0.35f, 0.5f, PIPE)

        nodeInstances[FerrumNodeId.ASSEMBLER] = box(-5.1f, 1.05f, 0.1f, 2.8f, 2.1f, 2.6f, ASSEMBLER)
        staticInstances += box(-5.1f, 2.35f, 0.1f, 1.5f, 0.45f, 1.5f, TECH)
        staticInstances += cylinder(-5.1f, 2.75f, 0.1f, 0.42f, 0.75f, TECH)

        nodeInstances[FerrumNodeId.IRON_DEPOSIT] =
            sphere(-7.4f, 0.65f, -4.3f, 2.2f, 1.3f, 2.2f, IRON)
        nodeInstances[FerrumNodeId.COPPER_DEPOSIT] =
            sphere(7.5f, 0.7f, -4.1f, 2.4f, 1.4f, 2.4f, COPPER)
        nodeInstances[FerrumNodeId.CRYSTAL_DEPOSIT] =
            cylinder(6.8f, 1.0f, 5.0f, 1.1f, 2.0f, CRYSTAL)

        staticInstances += nodeInstances.values
        developmentInstances[FerrumColonyStage.INDUSTRIAL] = buildIndustrialStage()
        developmentInstances[FerrumColonyStage.NETWORKED] = buildNetworkedStage()
        developmentInstances[FerrumColonyStage.ORBITAL] = buildOrbitalStage()

        repeat(4) { index ->
            val body = box(-1.5f + index, 0.45f, 2.5f, 0.72f, 0.62f, 0.94f, ROBOT)
            val head = box(-1.5f + index, 0.90f, 2.5f, 0.48f, 0.32f, 0.52f, WINDOW)
            robots += RobotPrimitive(body, head)
        }
        repeat(6) {
            crates += box(0f, 0.32f, 0f, 0.48f, 0.48f, 0.48f, CRATE)
        }
        highlight = cylinder(0f, 0.07f, 0f, 1.7f, 0.10f, SELECTION)
        orbitalBeacon = cylinder(0f, 5.7f, 5.7f, 0.36f, 1.4f, BEACON)
    }

    fun select(id: FerrumNodeId?) {
        selected = id
    }

    fun pick(ray: Ray): FerrumNodeId? = FerrumSceneSpec.nodes
        .asSequence()
        .filter { Intersector.intersectRayBoundsFast(ray, it.pickingBounds) }
        .minByOrNull { ray.origin.dst2(it.position) }
        ?.id

    fun render(
        camera: OrthographicCamera,
        nowMillis: Long,
        budget: RuntimePerformanceBudget,
        productionActive: Boolean,
        developmentStage: FerrumColonyStage = FerrumColonyStage.OUTPOST,
    ) {
        val seconds = nowMillis.coerceAtLeast(0L) / 1_000f
        val visibleRobots = minOf(robots.size, budget.maxVisibleRobots)
        val visibleCrates = if (productionActive) minOf(crates.size, budget.maxParticles / 8) else 1

        modelBatch.begin(camera)
        modelBatch.render(staticInstances, environment)
        developmentInstances.forEach { (stage, instances) ->
            if (developmentStage.includes(stage)) modelBatch.render(instances, environment)
        }

        repeat(visibleRobots) { index ->
            val phase = (seconds * (0.12f + index * 0.012f) + index * 0.23f) % 1f
            val position = routePosition(phase)
            val robot = robots[index]
            robot.body.transform.setToTranslation(position.x, 0.45f, position.z)
            robot.head.transform.setToTranslation(position.x, 0.91f, position.z)
            modelBatch.render(robot.body, environment)
            modelBatch.render(robot.head, environment)
        }

        repeat(visibleCrates) { index ->
            val phase = (seconds * 0.17f + index.toFloat() / visibleCrates.coerceAtLeast(1)) % 1f
            val position = crateRoutePosition(phase)
            val crate = crates[index]
            crate.transform.setToTranslation(position.x, 0.31f, position.z)
            modelBatch.render(crate, environment)
        }

        if (developmentStage.includes(FerrumColonyStage.ORBITAL)) {
            val pulse = 1f + sin(seconds * 2.8f) * 0.12f
            orbitalBeacon.transform.setToTranslation(0f, 5.7f, 5.7f).scale(pulse, 1f, pulse)
            modelBatch.render(orbitalBeacon, environment)
        }

        selected?.let { id ->
            val node = FerrumSceneSpec.nodes.first { it.id == id }
            highlight.transform.setToTranslation(node.position.x, 0.07f, node.position.z)
            modelBatch.render(highlight, environment)
        }
        modelBatch.end()
    }

    fun dispose() {
        modelBatch.dispose()
        ownedModels.distinct().forEach(Model::dispose)
        ownedModels.clear()
    }

    private fun buildIndustrialStage(): List<ModelInstance> = listOf(
        cylinder(3.2f, 0.8f, 2.8f, 1.25f, 1.6f, STORAGE),
        cylinder(4.6f, 0.8f, 2.8f, 1.25f, 1.6f, STORAGE),
        box(3.9f, 0.52f, 1.55f, 3.8f, 0.30f, 0.58f, PIPE),
        box(-3.4f, 0.42f, -3.7f, 4.8f, 0.22f, 0.72f, ROAD_LIGHT),
        box(3.3f, 0.42f, -3.7f, 4.8f, 0.22f, 0.72f, ROAD_LIGHT),
    )

    private fun buildNetworkedStage(): List<ModelInstance> = listOf(
        box(-8.2f, 0.25f, 3.7f, 2.4f, 0.18f, 1.5f, SOLAR),
        box(-5.3f, 0.25f, 4.7f, 2.4f, 0.18f, 1.5f, SOLAR),
        box(-2.4f, 0.25f, 5.5f, 2.4f, 0.18f, 1.5f, SOLAR),
        cylinder(-8.5f, 2.1f, 0.8f, 0.28f, 4.2f, RELAY),
        cylinder(8.5f, 2.1f, 1.2f, 0.28f, 4.2f, RELAY),
        cylinder(0f, 2.7f, -6.3f, 0.34f, 5.4f, RELAY),
        sphere(-8.5f, 4.25f, 0.8f, 0.82f, 0.82f, 0.82f, WINDOW),
        sphere(8.5f, 4.25f, 1.2f, 0.82f, 0.82f, 0.82f, WINDOW),
        sphere(0f, 5.45f, -6.3f, 0.92f, 0.92f, 0.92f, WINDOW),
    )

    private fun buildOrbitalStage(): List<ModelInstance> = listOf(
        box(-1.65f, 2.6f, 5.7f, 0.55f, 5.2f, 0.55f, GANTRY),
        box(1.65f, 2.6f, 5.7f, 0.55f, 5.2f, 0.55f, GANTRY),
        box(0f, 4.9f, 5.7f, 3.85f, 0.45f, 0.72f, GANTRY),
        box(0f, 0.28f, 5.7f, 5.1f, 0.42f, 4.2f, LAUNCH_PAD),
        cylinder(0f, 2.2f, 5.7f, 1.25f, 3.9f, ROCKET),
        cylinder(0f, 4.45f, 5.7f, 0.78f, 1.25f, ROCKET_LIGHT),
    )

    private fun routePosition(phase: Float): Vector3 {
        val route = ROBOT_ROUTE
        val scaled = phase * route.size
        val index = floor(scaled).toInt().coerceIn(0, route.lastIndex)
        val next = (index + 1) % route.size
        return Vector3(route[index]).lerp(route[next], scaled - floor(scaled))
    }

    private fun crateRoutePosition(phase: Float): Vector3 {
        val start = Vector3(-7.4f, 0f, -4.3f)
        val end = Vector3(0f, 0f, 0f)
        return start.lerp(end, phase)
    }

    private fun box(
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        depth: Float,
        color: Color,
    ): ModelInstance {
        val model = builder.createBox(
            width,
            height,
            depth,
            material(color),
            Usage.Position or Usage.Normal,
        ).also(ownedModels::add)
        return ModelInstance(model).apply { transform.setToTranslation(x, y, z) }
    }

    private fun cylinder(
        x: Float,
        y: Float,
        z: Float,
        diameter: Float,
        height: Float,
        color: Color,
    ): ModelInstance {
        val model = builder.createCylinder(
            diameter,
            height,
            diameter,
            20,
            material(color),
            Usage.Position or Usage.Normal,
        ).also(ownedModels::add)
        return ModelInstance(model).apply { transform.setToTranslation(x, y, z) }
    }

    private fun sphere(
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        depth: Float,
        color: Color,
    ): ModelInstance {
        val model = builder.createSphere(
            width,
            height,
            depth,
            20,
            14,
            material(color),
            Usage.Position or Usage.Normal,
        ).also(ownedModels::add)
        return ModelInstance(model).apply { transform.setToTranslation(x, y, z) }
    }

    private fun material(color: Color): Material =
        Material(ColorAttribute.createDiffuse(color))

    private data class RobotPrimitive(
        val body: ModelInstance,
        val head: ModelInstance,
    )

    private companion object {
        val ROBOT_ROUTE = listOf(
            Vector3(-7.4f, 0f, -4.3f),
            Vector3(-2.1f, 0f, -1.1f),
            Vector3(0f, 0f, 0f),
            Vector3(4.8f, 0f, -0.2f),
            Vector3(7.5f, 0f, -4.1f),
            Vector3(1.6f, 0f, -1.1f),
        )
        val GROUND = Color(0.075f, 0.095f, 0.13f, 1f)
        val ROAD = Color(0.11f, 0.15f, 0.19f, 1f)
        val ROAD_LIGHT = Color(0.18f, 0.34f, 0.42f, 1f)
        val BASE = Color(0.19f, 0.27f, 0.37f, 1f)
        val BASE_DARK = Color(0.10f, 0.15f, 0.22f, 1f)
        val REFINER = Color(0.25f, 0.26f, 0.34f, 1f)
        val ASSEMBLER = Color(0.17f, 0.28f, 0.30f, 1f)
        val HOT = Color(1f, 0.47f, 0.16f, 1f)
        val TECH = Color(0.20f, 0.90f, 0.72f, 1f)
        val PIPE = Color(0.38f, 0.44f, 0.52f, 1f)
        val WINDOW = Color(0.44f, 0.91f, 0.95f, 1f)
        val IRON = Color(0.52f, 0.58f, 0.66f, 1f)
        val COPPER = Color(0.76f, 0.39f, 0.19f, 1f)
        val CRYSTAL = Color(0.42f, 0.52f, 0.94f, 1f)
        val ROBOT = Color(0.70f, 0.76f, 0.80f, 1f)
        val CRATE = Color(0.82f, 0.62f, 0.26f, 1f)
        val SELECTION = Color(0.96f, 0.78f, 0.24f, 1f)
        val STORAGE = Color(0.29f, 0.38f, 0.46f, 1f)
        val SOLAR = Color(0.15f, 0.32f, 0.58f, 1f)
        val RELAY = Color(0.32f, 0.62f, 0.72f, 1f)
        val GANTRY = Color(0.56f, 0.38f, 0.25f, 1f)
        val LAUNCH_PAD = Color(0.20f, 0.23f, 0.29f, 1f)
        val ROCKET = Color(0.72f, 0.76f, 0.82f, 1f)
        val ROCKET_LIGHT = Color(0.92f, 0.48f, 0.18f, 1f)
        val BEACON = Color(0.36f, 0.94f, 0.92f, 1f)
    }
}
