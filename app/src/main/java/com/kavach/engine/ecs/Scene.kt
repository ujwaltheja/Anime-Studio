package com.kavach.engine.ecs

import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.Camera
import com.kavach.engine.renderer.MeshRenderer

class Scene(val name: String) {

    val camera = Camera()
    val lightDir = Vector3(-0.3f, -1.0f, -0.5f)
    val lightColor = Vector3(1.0f, 1.0f, 0.95f)
    val ambientColor = Vector3(0.2f, 0.2f, 0.25f)

    private val objects = mutableListOf<GameObject>()

    fun add(obj: GameObject): Scene {
        objects.add(obj)
        return this
    }

    fun remove(obj: GameObject) = objects.remove(obj)

    fun findByName(name: String) = objects.firstOrNull { it.name == name }

    fun all(): List<GameObject> = objects

    internal fun onStart() = objects.forEach { it.start() }

    internal fun onStop() {}

    fun update(deltaTime: Float) = objects.forEach { it.update(deltaTime) }

    fun render() {
        objects.forEach { obj ->
            if (obj.enabled) {
                obj.getComponent<MeshRenderer>()?.render(camera, lightDir, lightColor, ambientColor)
            }
        }
    }
}
