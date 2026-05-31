package com.kavach.engine.ecs.components

import com.kavach.engine.ecs.Component

/**
 * Convenience base class for inline game-logic components.
 *
 * Usage:
 * ```
 * gameObject.addComponent(object : BehaviourScript() {
 *     override fun tick(dt: Float) { transform.rotation.y += 90f * dt }
 * })
 * ```
 */
abstract class BehaviourScript : Component() {
    final override fun update(deltaTime: Float) = tick(deltaTime)

    abstract fun tick(deltaTime: Float)
}
