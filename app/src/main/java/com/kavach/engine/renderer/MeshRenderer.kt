package com.kavach.engine.renderer

import android.opengl.GLES30
import com.kavach.engine.ecs.Component
import com.kavach.engine.math.MathUtils
import com.kavach.engine.math.Vector3

class MeshRenderer(val mesh: Mesh, var shader: Shader) : Component() {

    var color = Vector3(1f, 1f, 1f)

    override fun onStart() {
        if (!mesh.uploaded) mesh.upload()
    }

    fun render(camera: Camera, lightDir: Vector3, lightColor: Vector3, ambientColor: Vector3) {
        if (!uploaded()) return

        shader.use()

        val model  = gameObject.transform.getModelMatrix()
        val normal = MathUtils.computeNormalMatrix(model)

        shader.setMat4("uModel",        model)
        shader.setMat4("uView",         camera.getViewMatrix())
        shader.setMat4("uProjection",   camera.getProjectionMatrix())
        shader.setMat3("uNormalMatrix", normal)
        shader.setVec3("uObjectColor",  color.x,       color.y,       color.z)
        shader.setVec3("uLightDir",     lightDir.x,    lightDir.y,    lightDir.z)
        shader.setVec3("uLightColor",   lightColor.x,  lightColor.y,  lightColor.z)
        shader.setVec3("uAmbientColor", ambientColor.x, ambientColor.y, ambientColor.z)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,         mesh.vbo)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.ebo)

        val stride = Mesh.VERTEX_STRIDE
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, Mesh.OFFSET_POSITION)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, Mesh.OFFSET_NORMAL)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, Mesh.OFFSET_TEXCOORD)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun uploaded() = mesh.uploaded
}
