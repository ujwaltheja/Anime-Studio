package com.kavach.engine.math

import android.opengl.Matrix

object MathUtils {

    /**
     * Computes the normal matrix (transpose of the inverse of the upper-left 3x3 of modelMatrix).
     * Returns a column-major float[9] ready for glUniformMatrix3fv(..., false, ...).
     */
    fun computeNormalMatrix(modelMatrix: FloatArray): FloatArray {
        val inv = FloatArray(16)
        val ok = Matrix.invertM(inv, 0, modelMatrix, 0)
        if (!ok) {
            // Degenerate matrix - fall back to the model's rotation part
            return floatArrayOf(
                modelMatrix[0], modelMatrix[1], modelMatrix[2],
                modelMatrix[4], modelMatrix[5], modelMatrix[6],
                modelMatrix[8], modelMatrix[9], modelMatrix[10]
            )
        }
        // Transpose of the upper-left 3×3 of inv, in column-major order:
        // col0 = row0 of inv,  col1 = row1 of inv,  col2 = row2 of inv
        return floatArrayOf(
            inv[0], inv[4], inv[8],
            inv[1], inv[5], inv[9],
            inv[2], inv[6], inv[10]
        )
    }

    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun clamp(value: Float, min: Float, max: Float) = value.coerceIn(min, max)

    fun toRadians(degrees: Float) = (degrees * Math.PI / 180.0).toFloat()
}
