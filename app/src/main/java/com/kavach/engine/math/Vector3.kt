package com.kavach.engine.math

import kotlin.math.sqrt

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    fun set(x: Float, y: Float, z: Float): Vector3 {
        this.x = x; this.y = y; this.z = z
        return this
    }

    operator fun plus(o: Vector3) = Vector3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3) = Vector3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)
    operator fun unaryMinus() = Vector3(-x, -y, -z)

    fun length() = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0f) Vector3(x / len, y / len, z / len) else Vector3()
    }

    fun dot(o: Vector3) = x * o.x + y * o.y + z * o.z

    fun cross(o: Vector3) = Vector3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )

    fun distanceTo(o: Vector3) = (this - o).length()

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, -1f)
        val RIGHT = Vector3(1f, 0f, 0f)
    }
}
