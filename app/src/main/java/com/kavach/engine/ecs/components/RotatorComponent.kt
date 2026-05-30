package com.kavach.engine.ecs.components

import com.kavach.engine.ecs.Component

/** Continuously rotates its GameObject around each axis at the given speeds (degrees/sec). */
class RotatorComponent(
    var speedX: Float = 0f,
    var speedY: Float = 45f,
    var speedZ: Float = 0f
) : Component() {

    override fun update(deltaTime: Float) {
        gameObject.transform.rotation.x += speedX * deltaTime
        gameObject.transform.rotation.y += speedY * deltaTime
        gameObject.transform.rotation.z += speedZ * deltaTime
    }
}
