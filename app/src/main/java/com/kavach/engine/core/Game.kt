package com.kavach.engine.core

import com.kavach.engine.KavachEngine

/**
 * Base class for all Kavach games. Subclass this and pass an instance to [KavachEngine.run].
 *
 * [start] is called once on the GL thread after the OpenGL context is ready.
 * [update] is called every frame on the GL thread.
 */
abstract class Game {
    /** Called once after the GL surface is ready. Set up scenes, load models, etc. */
    abstract fun start(engine: KavachEngine)

    /** Called every frame. [deltaTime] is seconds since the last frame. */
    abstract fun update(deltaTime: Float)
}
