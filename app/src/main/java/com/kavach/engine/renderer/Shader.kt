package com.kavach.engine.renderer

import android.opengl.GLES30
import android.util.Log

class Shader(private val vertexSource: String, private val fragmentSource: String) {

    var programId: Int = 0
        private set

    private val uniformCache = mutableMapOf<String, Int>()

    fun compile() {
        val vert = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val frag = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vert)
        GLES30.glAttachShader(programId, frag)
        GLES30.glLinkProgram(programId)

        val status = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == GLES30.GL_FALSE) {
            Log.e("Shader", "Link error: ${GLES30.glGetProgramInfoLog(programId)}")
        }

        GLES30.glDeleteShader(vert)
        GLES30.glDeleteShader(frag)
    }

    fun use() = GLES30.glUseProgram(programId)

    fun setMat4(name: String, value: FloatArray) =
        GLES30.glUniformMatrix4fv(loc(name), 1, false, value, 0)

    fun setMat3(name: String, value: FloatArray) =
        GLES30.glUniformMatrix3fv(loc(name), 1, false, value, 0)

    fun setVec3(name: String, x: Float, y: Float, z: Float) =
        GLES30.glUniform3f(loc(name), x, y, z)

    fun setFloat(name: String, v: Float) =
        GLES30.glUniform1f(loc(name), v)

    fun setInt(name: String, v: Int) =
        GLES30.glUniform1i(loc(name), v)

    fun destroy() {
        GLES30.glDeleteProgram(programId)
        uniformCache.clear()
    }

    private fun loc(name: String): Int =
        uniformCache.getOrPut(name) { GLES30.glGetUniformLocation(programId, name) }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == GLES30.GL_FALSE) {
            val typeName = if (type == GLES30.GL_VERTEX_SHADER) "VERTEX" else "FRAGMENT"
            Log.e("Shader", "$typeName compile error: ${GLES30.glGetShaderInfoLog(shader)}")
        }
        return shader
    }
}
