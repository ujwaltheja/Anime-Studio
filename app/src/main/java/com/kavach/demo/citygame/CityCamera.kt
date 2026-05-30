package com.kavach.demo.citygame

import com.kavach.engine.math.Vector3
import com.kavach.engine.renderer.Camera
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top-down isometric camera controller for the city builder.
 * [center] is the look-at point on the ground plane (y = 0).
 */
class CityCamera(private val camera: Camera, val gridSize: Int = 20) {

    val center = Vector3(gridSize / 2f, 0f, gridSize / 2f)
    var heightAngleDeg = 40f   // elevation angle from horizontal
    var orbitAngleDeg  = 35f   // yaw around the Y axis
    var distance       = 20f   // distance from center

    private val MIN_DIST = 6f
    private val MAX_DIST = 40f

    fun applyToCamera() {
        val elevation = Math.toRadians(heightAngleDeg.toDouble())
        val orbit     = Math.toRadians(orbitAngleDeg.toDouble())

        val horDist = (distance * cos(elevation)).toFloat()
        val verDist = (distance * sin(elevation)).toFloat()

        camera.position.set(
            center.x + horDist * sin(orbit).toFloat(),
            verDist,
            center.z + horDist * cos(orbit).toFloat()
        )
        camera.target.set(center.x, 0f, center.z)
    }

    /** Pan the camera by a world-space delta (clamped to grid bounds). */
    fun pan(dx: Float, dz: Float) {
        center.x = (center.x - dx).coerceIn(0f, gridSize.toFloat())
        center.z = (center.z - dz).coerceIn(0f, gridSize.toFloat())
    }

    /** Zoom in/out. */
    fun zoom(delta: Float) {
        distance = (distance - delta).coerceIn(MIN_DIST, MAX_DIST)
    }
}
