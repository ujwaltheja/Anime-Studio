package com.kavach.engine.core

import com.kavach.engine.ecs.Scene

class SceneManager {

    var currentScene: Scene? = null
        private set

    private val sceneRegistry = mutableMapOf<String, Scene>()

    fun register(scene: Scene) {
        sceneRegistry[scene.name] = scene
    }

    fun loadScene(scene: Scene) {
        currentScene?.onStop()
        currentScene = scene
        sceneRegistry[scene.name] = scene
        scene.onStart()
    }

    fun loadScene(name: String) {
        val scene = sceneRegistry[name]
            ?: throw IllegalArgumentException("Scene '$name' not registered")
        loadScene(scene)
    }
}
