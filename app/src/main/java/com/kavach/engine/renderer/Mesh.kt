package com.kavach.engine.renderer

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * GPU mesh holding interleaved vertex data: position(3) + normal(3) + texcoord(2) = 8 floats/vertex.
 */
class Mesh(
    val vertices: FloatArray,
    val indices: IntArray
) {
    companion object {
        const val VERTEX_STRIDE = 8 * Float.SIZE_BYTES   // 32 bytes per vertex
        const val OFFSET_POSITION  = 0
        const val OFFSET_NORMAL    = 3 * Float.SIZE_BYTES
        const val OFFSET_TEXCOORD  = 6 * Float.SIZE_BYTES
    }

    internal var vbo: Int = 0
        private set
    internal var ebo: Int = 0
        private set
    val indexCount: Int = indices.size
    var uploaded: Boolean = false
        private set

    fun upload() {
        if (uploaded) return

        val bufs = IntArray(2)
        GLES30.glGenBuffers(2, bufs, 0)
        vbo = bufs[0]
        ebo = bufs[1]

        val vb = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(vertices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * Float.SIZE_BYTES, vb, GLES30.GL_STATIC_DRAW)

        val ib = ByteBuffer.allocateDirect(indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()
        ib.put(indices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * Int.SIZE_BYTES, ib, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        uploaded = true
    }

    fun destroy() {
        if (uploaded) {
            GLES30.glDeleteBuffers(2, intArrayOf(vbo, ebo), 0)
            uploaded = false
        }
    }
}
