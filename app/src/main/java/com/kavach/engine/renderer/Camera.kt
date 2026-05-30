package com.kavach.engine.renderer

import android.opengl.Matrix
import com.kavach.engine.core.Time
import com.kavach.engine.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

class Camera {
    val position = Vector3(0f, 2f, 5f)
    var target   = Vector3(0f, 0f, 0f)
    val up       = Vector3(0f, 1f, 0f)

    var fov       = 45f
    var nearPlane = 0.1f
    var farPlane  = 1000f

    internal var aspectRatio = 1.7778f   // 16:9 default

    private val viewMatrix       = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    fun getViewMatrix(): FloatArray {
        Matrix.setLookAtM(
            viewMatrix, 0,
            position.x, position.y, position.z,
            target.x,   target.y,   target.z,
            up.x,       up.y,       up.z
        )
        return viewMatrix
    }

    fun getProjectionMatrix(): FloatArray {
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspectRatio, nearPlane, farPlane)
        return projectionMatrix
    }

    /** Orbits camera around target at the given angular speed (degrees/s). */
    fun orbitAround(target: Vector3 = Vector3.ZERO, speedDeg: Float = 30f) {
        val angle = Math.toRadians((Time.totalTime * speedDeg).toDouble())
        val dx = position.x - target.x
        val dz = position.z - target.z
        val radius = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()
        position.x = target.x + radius * sin(angle).toFloat()
        position.z = target.z + radius * cos(angle).toFloat()
        this.target.set(target.x, target.y, target.z)
    }
}
