package com.kavach.engine

import com.kavach.engine.core.InputManager
import com.kavach.engine.core.SceneManager

/**
 * Kavach Engine – Native Android Kotlin Game Engine.
 * Entry point for all engine subsystems.
 */
class KavachEngine private constructor() {

    val sceneManager = SceneManager()
    val inputManager = InputManager()

    companion object {
        @Volatile private var _instance: KavachEngine? = null

        val instance: KavachEngine?
            get() = _instance

        fun create(): KavachEngine {
            val engine = KavachEngine()
            _instance = engine
            return engine
        }
    }
}
