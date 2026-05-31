package com.kavach.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.kavach.engine.KavachEngine
import com.kavach.engine.core.Game
import com.kavach.engine.renderer.OpenGLRenderer

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var kavachEngine: KavachEngine? = null

    init {
        setEGLContextClientVersion(3)
        // RGBA8888 surface, 16-bit depth buffer, no stencil
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
    }

    fun attach(engine: KavachEngine, game: Game) {
        kavachEngine = engine
        val renderer = OpenGLRenderer(context, engine, game)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        kavachEngine?.inputManager?.queueTouchEvent(event.x, event.y, event.action)
        return true
    }
}
