package com.kavach.engine.physics

import com.kavach.engine.ecs.Scene
import com.kavach.engine.math.Vector3
import kotlin.math.abs
import kotlin.math.min

/**
 * Broad + narrow phase AABB collision detection.
 * Call [step] once per frame from your Game.update() if physics is needed.
 */
class CollisionSystem {

    /**
     * Detects overlaps and resolves them (position correction + velocity reflection
     * for dynamic vs static pairs). Fires [BoxCollider.onCollisionEnter] callbacks.
     */
    fun step(scene: Scene) {
        val colliders = scene.all()
            .filter { it.enabled }
            .mapNotNull { go -> go.getComponent<BoxCollider>()?.let { Pair(go, it) } }

        for (i in colliders.indices) {
            for (j in i + 1 until colliders.size) {
                val (goA, colA) = colliders[i]
                val (goB, colB) = colliders[j]

                if (!colA.overlaps(colB)) continue

                colA.onCollisionEnter?.invoke(colB)
                colB.onCollisionEnter?.invoke(colA)

                if (colA.isTrigger || colB.isTrigger) continue

                // Minimal-translation-vector push-apart
                val mtv = computeMTV(colA, colB)
                val rbA = goA.getComponent<Rigidbody>()
                val rbB = goB.getComponent<Rigidbody>()

                when {
                    rbA != null && rbB != null -> {
                        goA.transform.position.x -= mtv.x * 0.5f
                        goA.transform.position.y -= mtv.y * 0.5f
                        goA.transform.position.z -= mtv.z * 0.5f
                        goB.transform.position.x += mtv.x * 0.5f
                        goB.transform.position.y += mtv.y * 0.5f
                        goB.transform.position.z += mtv.z * 0.5f
                        reflectVelocity(rbA, mtv, -1f)
                        reflectVelocity(rbB, mtv,  1f)
                    }
                    rbA != null -> {
                        goA.transform.position.x -= mtv.x
                        goA.transform.position.y -= mtv.y
                        goA.transform.position.z -= mtv.z
                        reflectVelocity(rbA, mtv, -1f)
                        if (abs(mtv.y) > abs(mtv.x) && abs(mtv.y) > abs(mtv.z)) {
                            rbA.isGrounded = true
                        }
                    }
                    rbB != null -> {
                        goB.transform.position.x += mtv.x
                        goB.transform.position.y += mtv.y
                        goB.transform.position.z += mtv.z
                        reflectVelocity(rbB, mtv, 1f)
                    }
                }
            }
        }
    }

    private fun computeMTV(a: BoxCollider, b: BoxCollider): Vector3 {
        val aMin = a.worldMin(); val aMax = a.worldMax()
        val bMin = b.worldMin(); val bMax = b.worldMax()

        val overlapX = min(aMax.x - bMin.x, bMax.x - aMin.x)
        val overlapY = min(aMax.y - bMin.y, bMax.y - aMin.y)
        val overlapZ = min(aMax.z - bMin.z, bMax.z - aMin.z)

        return when {
            overlapX <= overlapY && overlapX <= overlapZ ->
                Vector3(if (a.worldMin().x < b.worldMin().x) overlapX else -overlapX, 0f, 0f)
            overlapY <= overlapX && overlapY <= overlapZ ->
                Vector3(0f, if (a.worldMin().y < b.worldMin().y) overlapY else -overlapY, 0f)
            else ->
                Vector3(0f, 0f, if (a.worldMin().z < b.worldMin().z) overlapZ else -overlapZ)
        }
    }

    private fun reflectVelocity(rb: Rigidbody, normal: Vector3, sign: Float) {
        val n = normal.normalized()
        val dot = rb.velocity.dot(n)
        if (dot * sign < 0) {
            rb.velocity.x -= 2f * dot * n.x
            rb.velocity.y -= 2f * dot * n.y
            rb.velocity.z -= 2f * dot * n.z
            rb.velocity.x *= 0.4f; rb.velocity.y *= 0.4f; rb.velocity.z *= 0.4f
        }
    }
}
