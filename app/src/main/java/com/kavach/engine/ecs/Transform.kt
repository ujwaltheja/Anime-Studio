package com.kavach.engine.ecs

import android.opengl.Matrix
import com.kavach.engine.math.Vector3

class Transform {
    val position = Vector3(0f, 0f, 0f)
    val rotation = Vector3(0f, 0f, 0f)   // Euler angles in degrees (X=pitch, Y=yaw, Z=roll)
    val scale    = Vector3(1f, 1f, 1f)

    var parent: Transform? = null

    fun getModelMatrix(): FloatArray {
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        Matrix.translateM(m, 0, position.x, position.y, position.z)
        Matrix.rotateM(m, 0, rotation.y, 0f, 1f, 0f)
        Matrix.rotateM(m, 0, rotation.x, 1f, 0f, 0f)
        Matrix.rotateM(m, 0, rotation.z, 0f, 0f, 1f)
        Matrix.scaleM(m, 0, scale.x, scale.y, scale.z)

        parent?.let { p ->
            val world = FloatArray(16)
            Matrix.multiplyMM(world, 0, p.getModelMatrix(), 0, m, 0)
            return world
        }
        return m
    }

    fun setPosition(x: Float, y: Float, z: Float) = position.set(x, y, z)
    fun setRotation(x: Float, y: Float, z: Float) = rotation.set(x, y, z)
    fun setScale(x: Float, y: Float, z: Float)    = scale.set(x, y, z)
    fun setScale(uniform: Float)                   = scale.set(uniform, uniform, uniform)
}
