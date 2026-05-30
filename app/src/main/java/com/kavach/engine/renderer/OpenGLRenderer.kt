package com.kavach.engine.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.kavach.engine.KavachEngine
import com.kavach.engine.core.Game
import com.kavach.engine.core.Time
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(
    private val context: Context,
    private val engine: KavachEngine,
    private val game: Game
) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("KavachEngine", "Surface created – OpenGL ES ${GLES30.glGetString(GLES30.GL_VERSION)}")
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glClearColor(0.08f, 0.08f, 0.12f, 1.0f)

        Time.reset()
        game.start(engine)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        engine.sceneManager.currentScene?.camera?.aspectRatio = width.toFloat() / height.toFloat()
        Log.d("KavachEngine", "Viewport: ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        Time.update()
        engine.inputManager.processEvents()

        game.update(Time.deltaTime)

        val scene = engine.sceneManager.currentScene
        scene?.update(Time.deltaTime)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        scene?.render()

        // Log FPS every second
        if (Time.fps > 0f && (Time.totalTime % 1f) < Time.deltaTime) {
            Log.d("KavachEngine", "FPS: ${"%.1f".format(Time.fps)}")
        }
    }
}
