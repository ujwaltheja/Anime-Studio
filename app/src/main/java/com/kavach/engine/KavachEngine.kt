package com.kavach.engine

import android.content.Context
import com.kavach.engine.audio.MusicManager
import com.kavach.engine.audio.SoundManager
import com.kavach.engine.core.InputManager
import com.kavach.engine.core.SceneManager
import com.kavach.engine.physics.CollisionSystem

/**
 * Kavach Engine – singleton entry point.
 * Create once per Activity via [create]; access subsystems from anywhere via [instance].
 */
class KavachEngine private constructor() {

    val sceneManager    = SceneManager()
    val inputManager    = InputManager()
    val collisionSystem = CollisionSystem()

    var soundManager: SoundManager? = null
        private set
    var musicManager: MusicManager? = null
        private set

    fun initAudio(context: Context) {
        soundManager = SoundManager(context)
        musicManager = MusicManager(context)
    }

    fun releaseAudio() {
        soundManager?.release()
        soundManager = null
        musicManager?.stop()
        musicManager = null
    }

    companion object {
        @Volatile private var _instance: KavachEngine? = null

        val instance: KavachEngine? get() = _instance

        fun create(): KavachEngine {
            val engine = KavachEngine()
            _instance = engine
            return engine
        }
    }
}
