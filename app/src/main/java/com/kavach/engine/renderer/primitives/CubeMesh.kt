package com.kavach.engine.renderer.primitives

import com.kavach.engine.renderer.Mesh

object CubeMesh {

    /**
     * Creates a unit cube centred at origin with 24 vertices (4 per face),
     * correct per-face normals, and UV coordinates.
     * Vertex layout: position(3) + normal(3) + texcoord(2) = 8 floats.
     */
    fun create(): Mesh {
        // Each row: px, py, pz,  nx, ny, nz,  u, v
        val v = floatArrayOf(
            // Front face  (+Z)
            -0.5f, -0.5f,  0.5f,  0f,  0f,  1f,  0f, 0f,
             0.5f, -0.5f,  0.5f,  0f,  0f,  1f,  1f, 0f,
             0.5f,  0.5f,  0.5f,  0f,  0f,  1f,  1f, 1f,
            -0.5f,  0.5f,  0.5f,  0f,  0f,  1f,  0f, 1f,
            // Back face   (-Z)
             0.5f, -0.5f, -0.5f,  0f,  0f, -1f,  0f, 0f,
            -0.5f, -0.5f, -0.5f,  0f,  0f, -1f,  1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f,  0f, -1f,  1f, 1f,
             0.5f,  0.5f, -0.5f,  0f,  0f, -1f,  0f, 1f,
            // Left face   (-X)
            -0.5f, -0.5f, -0.5f, -1f,  0f,  0f,  0f, 0f,
            -0.5f, -0.5f,  0.5f, -1f,  0f,  0f,  1f, 0f,
            -0.5f,  0.5f,  0.5f, -1f,  0f,  0f,  1f, 1f,
            -0.5f,  0.5f, -0.5f, -1f,  0f,  0f,  0f, 1f,
            // Right face  (+X)
             0.5f, -0.5f,  0.5f,  1f,  0f,  0f,  0f, 0f,
             0.5f, -0.5f, -0.5f,  1f,  0f,  0f,  1f, 0f,
             0.5f,  0.5f, -0.5f,  1f,  0f,  0f,  1f, 1f,
             0.5f,  0.5f,  0.5f,  1f,  0f,  0f,  0f, 1f,
            // Top face    (+Y)
            -0.5f,  0.5f,  0.5f,  0f,  1f,  0f,  0f, 0f,
             0.5f,  0.5f,  0.5f,  0f,  1f,  0f,  1f, 0f,
             0.5f,  0.5f, -0.5f,  0f,  1f,  0f,  1f, 1f,
            -0.5f,  0.5f, -0.5f,  0f,  1f,  0f,  0f, 1f,
            // Bottom face (-Y)
            -0.5f, -0.5f, -0.5f,  0f, -1f,  0f,  0f, 0f,
             0.5f, -0.5f, -0.5f,  0f, -1f,  0f,  1f, 0f,
             0.5f, -0.5f,  0.5f,  0f, -1f,  0f,  1f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, -1f,  0f,  0f, 1f
        )

        val i = intArrayOf(
             0,  1,  2,   0,  2,  3,   // front
             4,  5,  6,   4,  6,  7,   // back
             8,  9, 10,   8, 10, 11,   // left
            12, 13, 14,  12, 14, 15,   // right
            16, 17, 18,  16, 18, 19,   // top
            20, 21, 22,  20, 22, 23    // bottom
        )

        return Mesh(v, i)
    }
}
