package com.kavach.engine.core

object Time {
    /** Time in seconds since the last frame. Capped at 100ms. */
    var deltaTime: Float = 0f
        private set

    /** Total elapsed time in seconds since the engine started. */
    var totalTime: Float = 0f
        private set

    /** Frames per second (updated every second). */
    var fps: Float = 0f
        private set

    private var lastFrameNanos: Long = 0L
    private var frameCount: Int = 0
    private var fpsAccumulator: Float = 0f

    internal fun update() {
        val now = System.nanoTime()
        deltaTime = if (lastFrameNanos == 0L) 0f
                    else ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
        lastFrameNanos = now
        totalTime += deltaTime

        frameCount++
        fpsAccumulator += deltaTime
        if (fpsAccumulator >= 1f) {
            fps = frameCount / fpsAccumulator
            frameCount = 0
            fpsAccumulator = 0f
        }
    }

    internal fun reset() {
        lastFrameNanos = 0L
        deltaTime = 0f
        totalTime = 0f
        fps = 0f
        frameCount = 0
        fpsAccumulator = 0f
    }
}
