package com.kavach.engine.renderer.primitives

import com.kavach.engine.renderer.Mesh

object PlaneMesh {

    /**
     * Creates a flat XZ-plane mesh centred at origin.
     * Segments control how many grid subdivisions exist (useful for large terrains).
     */
    fun create(
        width: Float = 1f,
        depth: Float = 1f,
        segmentsX: Int = 1,
        segmentsZ: Int = 1
    ): Mesh {
        val cols = segmentsX + 1
        val rows = segmentsZ + 1
        val verts = FloatArray(cols * rows * 8)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = (r * cols + c) * 8
                verts[idx + 0] = -width / 2f + width * c / segmentsX
                verts[idx + 1] = 0f
                verts[idx + 2] = -depth / 2f + depth * r / segmentsZ
                verts[idx + 3] = 0f; verts[idx + 4] = 1f; verts[idx + 5] = 0f  // normal up
                verts[idx + 6] = c.toFloat() / segmentsX
                verts[idx + 7] = r.toFloat() / segmentsZ
            }
        }

        val indices = IntArray(segmentsX * segmentsZ * 6)
        var i = 0
        for (r in 0 until segmentsZ) {
            for (c in 0 until segmentsX) {
                val tl = r * cols + c
                val tr = tl + 1
                val bl = (r + 1) * cols + c
                val br = bl + 1
                indices[i++] = tl; indices[i++] = bl; indices[i++] = br
                indices[i++] = tl; indices[i++] = br; indices[i++] = tr
            }
        }
        return Mesh(verts, indices)
    }
}
