package com.kavach.demo.citygame

import android.opengl.Matrix
import com.kavach.engine.math.Vector3
import kotlin.math.abs
import kotlin.math.floor

/**
 * Converts 2-D screen coordinates to a grid cell by ray-casting against the y = 0 plane.
 */
object GridRaycaster {

    /**
     * Returns (gridX, gridZ) or null if the ray doesn't intersect y = 0 in front of the camera.
     */
    fun screenToGrid(
        screenX: Float, screenY: Float,
        screenW: Int,   screenH: Int,
        viewMatrix: FloatArray,
        projMatrix: FloatArray
    ): Pair<Int, Int>? {
        // Build VP and invert
        val vp    = FloatArray(16)
        val invVP = FloatArray(16)
        Matrix.multiplyMM(vp, 0, projMatrix, 0, viewMatrix, 0)
        if (!Matrix.invertM(invVP, 0, vp, 0)) return null

        // NDC coordinates
        val ndcX = 2f * screenX / screenW - 1f
        val ndcY = 1f - 2f * screenY / screenH

        val near = unproject(ndcX, ndcY, -1f, invVP)
        val far  = unproject(ndcX, ndcY,  1f, invVP)

        val dy = far.y - near.y
        if (abs(dy) < 1e-4f) return null

        val t = -near.y / dy
        if (t < 0f) return null

        val worldX = near.x + t * (far.x - near.x)
        val worldZ = near.z + t * (far.z - near.z)

        return Pair(floor(worldX).toInt(), floor(worldZ).toInt())
    }

    private fun unproject(ndcX: Float, ndcY: Float, ndcZ: Float, invVP: FloatArray): Vector3 {
        val clip  = floatArrayOf(ndcX, ndcY, ndcZ, 1f)
        val world = FloatArray(4)
        Matrix.multiplyMV(world, 0, invVP, 0, clip, 0)
        val w = world[3]
        return Vector3(world[0] / w, world[1] / w, world[2] / w)
    }
}
