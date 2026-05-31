package com.kavach.engine.physics

import com.kavach.engine.ecs.Component
import com.kavach.engine.math.Vector3

/**
 * Axis-Aligned Bounding Box collider.
 * [size] is the full extent in world-space (before transform scale is applied).
 * [isTrigger] – true means overlap events fire but no physical response.
 */
class BoxCollider(
    val size: Vector3 = Vector3(1f, 1f, 1f),
    var isTrigger: Boolean = false
) : Component() {

    var onCollisionEnter: ((BoxCollider) -> Unit)? = null

    /** World-space AABB min/max computed from the transform each frame. */
    fun worldMin(): Vector3 {
        val p = gameObject.transform.position
        val s = gameObject.transform.scale
        return Vector3(
            p.x - size.x * s.x / 2f,
            p.y - size.y * s.y / 2f,
            p.z - size.z * s.z / 2f
        )
    }

    fun worldMax(): Vector3 {
        val p = gameObject.transform.position
        val s = gameObject.transform.scale
        return Vector3(
            p.x + size.x * s.x / 2f,
            p.y + size.y * s.y / 2f,
            p.z + size.z * s.z / 2f
        )
    }

    fun overlaps(other: BoxCollider): Boolean {
        val aMin = worldMin(); val aMax = worldMax()
        val bMin = other.worldMin(); val bMax = other.worldMax()
        return aMax.x > bMin.x && aMin.x < bMax.x &&
               aMax.y > bMin.y && aMin.y < bMax.y &&
               aMax.z > bMin.z && aMin.z < bMax.z
    }
}
