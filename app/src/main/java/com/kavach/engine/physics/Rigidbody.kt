package com.kavach.engine.physics

import com.kavach.engine.ecs.Component
import com.kavach.engine.math.Vector3

/**
 * Lightweight Rigidbody component.
 * Applies gravity and linear drag each frame; the [CollisionSystem] resolves overlaps afterwards.
 */
class Rigidbody(
    var mass: Float    = 1f,
    var useGravity: Boolean = true,
    var drag: Float    = 0.02f
) : Component() {

    val velocity = Vector3()
    var isGrounded: Boolean = false

    fun applyForce(force: Vector3) {
        velocity.x += force.x / mass
        velocity.y += force.y / mass
        velocity.z += force.z / mass
    }

    fun applyImpulse(impulse: Vector3) = applyForce(impulse)

    override fun update(deltaTime: Float) {
        if (useGravity && !isGrounded) {
            velocity.y -= 9.81f * deltaTime
        }

        // Integrate position
        gameObject.transform.position.x += velocity.x * deltaTime
        gameObject.transform.position.y += velocity.y * deltaTime
        gameObject.transform.position.z += velocity.z * deltaTime

        // Linear drag
        val d = 1f - drag * deltaTime
        velocity.x *= d; velocity.y *= d; velocity.z *= d
    }
}
