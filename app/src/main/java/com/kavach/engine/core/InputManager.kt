package com.kavach.engine.core

import android.view.MotionEvent
import java.util.concurrent.ConcurrentLinkedQueue

class InputManager {

    data class TouchEvent(val x: Float, val y: Float, val action: Int)

    private val eventQueue = ConcurrentLinkedQueue<TouchEvent>()

    /** Updated by OpenGLRenderer.onSurfaceChanged — used for ray-casting. */
    var screenWidth:  Int = 1
    var screenHeight: Int = 1

    var onTouchDown: ((x: Float, y: Float) -> Unit)? = null
    var onTouchUp:   ((x: Float, y: Float) -> Unit)? = null
    var onTouchMove: ((x: Float, y: Float) -> Unit)? = null

    /** Called from the View thread (main thread) to enqueue a touch event. */
    fun queueTouchEvent(x: Float, y: Float, action: Int) {
        eventQueue.offer(TouchEvent(x, y, action))
    }

    /** Called from the GL thread each frame to dispatch queued events. */
    internal fun processEvents() {
        while (true) {
            val event = eventQueue.poll() ?: break
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> onTouchDown?.invoke(event.x, event.y)
                MotionEvent.ACTION_UP   -> onTouchUp?.invoke(event.x, event.y)
                MotionEvent.ACTION_MOVE -> onTouchMove?.invoke(event.x, event.y)
            }
        }
    }

    fun clearCallbacks() {
        onTouchDown = null
        onTouchUp = null
        onTouchMove = null
    }
}
