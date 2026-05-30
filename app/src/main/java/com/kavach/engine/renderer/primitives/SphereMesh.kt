package com.kavach.engine.renderer.primitives

import com.kavach.engine.renderer.Mesh
import kotlin.math.cos
import kotlin.math.sin

object SphereMesh {

    fun create(radius: Float = 0.5f, latSegs: Int = 16, lonSegs: Int = 16): Mesh {
        val verts = FloatArray((latSegs + 1) * (lonSegs + 1) * 8)
        var vi = 0
        for (lat in 0..latSegs) {
            val theta = lat * Math.PI / latSegs
            val sinT = sin(theta).toFloat()
            val cosT = cos(theta).toFloat()
            for (lon in 0..lonSegs) {
                val phi = lon * 2.0 * Math.PI / lonSegs
                val nx = (sinT * cos(phi)).toFloat()
                val ny = cosT
                val nz = (sinT * sin(phi)).toFloat()
                verts[vi * 8 + 0] = radius * nx
                verts[vi * 8 + 1] = radius * ny
                verts[vi * 8 + 2] = radius * nz
                verts[vi * 8 + 3] = nx; verts[vi * 8 + 4] = ny; verts[vi * 8 + 5] = nz
                verts[vi * 8 + 6] = lon.toFloat() / lonSegs
                verts[vi * 8 + 7] = 1f - lat.toFloat() / latSegs
                vi++
            }
        }

        val indices = IntArray(latSegs * lonSegs * 6)
        var i = 0
        for (lat in 0 until latSegs) {
            for (lon in 0 until lonSegs) {
                val a = lat * (lonSegs + 1) + lon
                val b = a + lonSegs + 1
                indices[i++] = a;     indices[i++] = b;     indices[i++] = a + 1
                indices[i++] = b;     indices[i++] = b + 1; indices[i++] = a + 1
            }
        }
        return Mesh(verts, indices)
    }
}
