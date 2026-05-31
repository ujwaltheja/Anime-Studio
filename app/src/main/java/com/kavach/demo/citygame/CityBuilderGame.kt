package com.kavach.demo.citygame

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kavach.engine.KavachEngine
import com.kavach.engine.assets.AssetLoader
import com.kavach.engine.core.Game
import com.kavach.engine.core.Time
import com.kavach.engine.ecs.GameObject
import com.kavach.engine.ecs.Scene
import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.MeshRenderer
import com.kavach.engine.renderer.Shader
import com.kavach.engine.renderer.primitives.CubeMesh
import com.kavach.engine.renderer.primitives.PlaneMesh

/**
 * Phase-5 City Builder game.
 *
 * Controls:
 *  • Tap  → place the selected building on the tapped grid cell
 *  • Drag → pan the camera
 *  • Bottom toolbar (Compose HUD) → select building type / deselect
 */
class CityBuilderGame(
    private val context: Context,
    val hud: CityHUDState
) : Game() {

    private val GRID_SIZE   = 20
    private val PAN_SPEED   = 0.015f
    private val HUD_INTERVAL = 20       // update HUD every N frames

    private lateinit var engine: KavachEngine
    private lateinit var scene:  Scene
    private lateinit var cityCamera: CityCamera
    private lateinit var defaultShader: Shader

    private val cityGrid  = CityGrid(GRID_SIZE)
    private var gameState = CityGameState()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var frameCount  = 0

    // Touch state
    private var touchStartX  = 0f
    private var touchStartY  = 0f
    private var touchStartMs = 0L
    private var isDragging   = false

    // ── Game lifecycle ─────────────────────────────────────────────────────

    override fun start(engine: KavachEngine) {
        this.engine = engine

        val vertSrc = AssetLoader.loadText(context, "shaders/basic.vert")
        val fragSrc = AssetLoader.loadText(context, "shaders/basic.frag")
        defaultShader = Shader(vertSrc, fragSrc).also { it.compile() }

        scene      = Scene("CityBuilder")
        cityCamera = CityCamera(scene.camera, GRID_SIZE)

        buildGround()

        // Restore saved game or start fresh
        val saved = CityGameState.load(context, cityGrid)
        if (saved != null) {
            gameState = saved
            cityGrid.allPlacements().forEach { (x, z, type) ->
                val obj = spawnBuildingMesh(x, z, type)
                cityGrid.place(x, z, type, obj)
            }
            Log.d("CityGame", "Loaded saved city – budget=${gameState.budget}")
        }

        engine.sceneManager.loadScene(scene)
        setupInput()
        pushHUD()
    }

    override fun update(deltaTime: Float) {
        cityCamera.applyToCamera()

        frameCount++
        if (frameCount % HUD_INTERVAL == 0) pushHUD()
    }

    // ── Ground ─────────────────────────────────────────────────────────────

    private fun buildGround() {
        val mesh = PlaneMesh.create(GRID_SIZE.toFloat(), GRID_SIZE.toFloat(), GRID_SIZE, GRID_SIZE)
        mesh.upload()
        val mr = MeshRenderer(mesh, defaultShader).also { it.color = Vector3(0.28f, 0.60f, 0.22f) }
        scene.add(GameObject("Ground").apply {
            transform.setPosition(GRID_SIZE / 2f, 0f, GRID_SIZE / 2f)
            addComponent(mr)
        })

        // Grid lines (thin flat planes) — optional subtle darker strip around grid
        val border = PlaneMesh.create(GRID_SIZE.toFloat() + 2f, GRID_SIZE.toFloat() + 2f).also { it.upload() }
        val borderMr = MeshRenderer(border, defaultShader).also { it.color = Vector3(0.20f, 0.45f, 0.18f) }
        scene.add(GameObject("Border").apply {
            transform.setPosition(GRID_SIZE / 2f, -0.01f, GRID_SIZE / 2f)
            addComponent(borderMr)
        })
    }

    // ── Building placement ─────────────────────────────────────────────────

    private fun handleTap(screenX: Float, screenY: Float) {
        val selected = hud.selectedBuilding ?: return

        val cell = GridRaycaster.screenToGrid(
            screenX, screenY,
            engine.inputManager.screenWidth,
            engine.inputManager.screenHeight,
            scene.camera.getViewMatrix(),
            scene.camera.getProjectionMatrix()
        ) ?: return

        val (gx, gz) = cell
        if (!cityGrid.isValid(gx, gz) || !cityGrid.isEmpty(gx, gz)) return

        if (gameState.budget < selected.cost) {
            showMessage("Need ₹${"%,d".format(selected.cost)} – not enough budget!")
            return
        }

        gameState.budget     -= selected.cost
        gameState.population += selected.populationBonus
        val obj = spawnBuildingMesh(gx, gz, selected)
        cityGrid.place(gx, gz, selected, obj)
        gameState.save(context, cityGrid)
        showMessage("${selected.displayName} placed!")
        pushHUD()
    }

    private fun spawnBuildingMesh(x: Int, z: Int, type: BuildingType): GameObject {
        val mesh = CubeMesh.create().also { it.upload() }
        val mr   = MeshRenderer(mesh, defaultShader).also { it.color = type.color }

        return GameObject("${type.name}_${x}_$z").apply {
            transform.setPosition(x + 0.5f, type.buildHeight / 2f, z + 0.5f)
            transform.setScale(type.footprint, type.buildHeight, type.footprint)
            addComponent(mr)
        }.also { scene.add(it) }
    }

    // ── Input ──────────────────────────────────────────────────────────────

    private fun setupInput() {
        engine.inputManager.onTouchDown = { x, y ->
            touchStartX  = x; touchStartY = y
            touchStartMs = System.currentTimeMillis()
            isDragging   = false
        }
        engine.inputManager.onTouchMove = { x, y ->
            val dx = x - touchStartX
            val dy = y - touchStartY
            if (!isDragging && (dx * dx + dy * dy) > 100f) isDragging = true
            if (isDragging) {
                cityCamera.pan(dx * PAN_SPEED, dy * PAN_SPEED)
                touchStartX = x; touchStartY = y
            }
        }
        engine.inputManager.onTouchUp = { x, y ->
            val ms = System.currentTimeMillis() - touchStartMs
            if (!isDragging && ms < 350) handleTap(x, y)
            isDragging = false
        }
    }

    // ── HUD helpers ────────────────────────────────────────────────────────

    private fun pushHUD() {
        mainHandler.post {
            hud.fps        = Time.fps
            hud.budget     = gameState.budget
            hud.population = gameState.population
        }
    }

    private fun showMessage(msg: String) {
        mainHandler.post {
            hud.message = msg
            mainHandler.postDelayed({ hud.message = "" }, 2500)
        }
    }
}
