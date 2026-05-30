package com.kavach.demo

import android.content.Context
import com.kavach.engine.KavachEngine
import com.kavach.engine.assets.AssetLoader
import com.kavach.engine.core.Game
import com.kavach.engine.ecs.GameObject
import com.kavach.engine.ecs.Scene
import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.MeshRenderer
import com.kavach.engine.renderer.Shader
import com.kavach.engine.renderer.primitives.CubeMesh

/**
 * Phase-1 MVP demo: renders three coloured cubes that rotate under a moving camera.
 * Demonstrates the full engine pipeline: Game → Scene → GameObject → MeshRenderer.
 */
class DemoGame(private val context: Context) : Game() {

    private val cubes = mutableListOf<GameObject>()
    private lateinit var scene: Scene

    override fun start(engine: KavachEngine) {
        val vertSrc = AssetLoader.loadText(context, "shaders/basic.vert")
        val fragSrc = AssetLoader.loadText(context, "shaders/basic.frag")

        // Three cubes with different colours and positions
        val cubeData = listOf(
            Triple(Vector3( 0f, 0f,  0f), Vector3(0.22f, 0.60f, 0.90f), "BlueCube"),
            Triple(Vector3( 2f, 0f, -1f), Vector3(0.90f, 0.35f, 0.22f), "RedCube"),
            Triple(Vector3(-2f, 0f, -1f), Vector3(0.30f, 0.80f, 0.35f), "GreenCube")
        )

        scene = Scene("KavachDemo").also { s ->
            s.camera.position.set(0f, 2.5f, 6f)

            for ((pos, color, name) in cubeData) {
                val shader = Shader(vertSrc, fragSrc).also { it.compile() }
                val mesh   = CubeMesh.create().also { it.upload() }
                val mr     = MeshRenderer(mesh, shader).also { it.color = color }

                val go = GameObject(name).apply {
                    transform.setPosition(pos.x, pos.y, pos.z)
                    addComponent(mr)
                }
                s.add(go)
                cubes.add(go)
            }
        }

        engine.sceneManager.loadScene(scene)

        // Touch spins the first cube faster
        engine.inputManager.onTouchDown = { _, _ ->
            cubes.firstOrNull()?.let { it.transform.rotation.y += 45f }
        }
    }

    override fun update(deltaTime: Float) {
        cubes.forEachIndexed { i, cube ->
            cube.transform.rotation.y += (30f + i * 15f) * deltaTime
            cube.transform.rotation.x += (10f + i *  5f) * deltaTime
        }

        // Orbit camera slowly
        scene.camera.orbitAround(Vector3.ZERO, 15f)
    }
}
