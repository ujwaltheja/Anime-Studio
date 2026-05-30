package com.kavach.demo

import android.content.Context
import com.kavach.engine.KavachEngine
import com.kavach.engine.assets.AssetLoader
import com.kavach.engine.core.Game
import com.kavach.engine.ecs.GameObject
import com.kavach.engine.ecs.Scene
import com.kavach.engine.ecs.components.RotatorComponent
import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.MeshRenderer
import com.kavach.engine.renderer.Shader
import com.kavach.engine.renderer.primitives.CubeMesh
import com.kavach.engine.renderer.primitives.SphereMesh

/**
 * Simple tech-demo: rotating coloured shapes with an orbiting camera.
 * Not used by the default MainActivity (which runs the city builder),
 * but kept as a quick engine smoke-test.
 */
class DemoGame(private val context: Context) : Game() {

    private lateinit var scene: Scene

    override fun start(engine: KavachEngine) {
        val vertSrc = AssetLoader.loadText(context, "shaders/basic.vert")
        val fragSrc = AssetLoader.loadText(context, "shaders/basic.frag")

        val objects = listOf(
            Triple(Vector3( 0f, 0f,  0f),  Vector3(0.22f, 0.60f, 0.90f), "BlueCube"),
            Triple(Vector3( 2f, 0f, -1f),  Vector3(0.90f, 0.35f, 0.22f), "RedCube"),
            Triple(Vector3(-2f, 0f, -1f),  Vector3(0.30f, 0.80f, 0.35f), "GreenSphere")
        )

        scene = Scene("KavachDemo").apply {
            camera.position.set(0f, 2.5f, 6f)
        }

        objects.forEachIndexed { i, (pos, color, name) ->
            val mesh = if (i == 2) SphereMesh.create() else CubeMesh.create()
            mesh.upload()
            val mr = MeshRenderer(mesh, Shader(vertSrc, fragSrc).also { it.compile() })
            mr.color = color

            val go = GameObject(name).apply {
                transform.setPosition(pos.x, pos.y, pos.z)
                addComponent(mr)
                addComponent(RotatorComponent(speedX = 10f + i * 5f, speedY = 30f + i * 15f))
            }
            scene.add(go)
        }

        engine.sceneManager.loadScene(scene)

        engine.inputManager.onTouchDown = { _, _ ->
            scene.findByName("BlueCube")?.transform?.rotation?.y =
                (scene.findByName("BlueCube")?.transform?.rotation?.y ?: 0f) + 45f
        }
    }

    override fun update(deltaTime: Float) {
        scene.camera.orbitAround(Vector3.ZERO, 15f)
    }
}
