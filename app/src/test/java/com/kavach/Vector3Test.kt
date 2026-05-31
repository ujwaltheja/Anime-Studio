package com.kavach

import com.kavach.engine.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test

class Vector3Test {

    @Test
    fun `length of unit Y vector is 1`() {
        assertEquals(1f, Vector3.UP.length(), 0.0001f)
    }

    @Test
    fun `dot product of perpendicular vectors is 0`() {
        assertEquals(0f, Vector3.UP.dot(Vector3.RIGHT), 0.0001f)
    }

    @Test
    fun `cross product of X and Y is Z`() {
        val result = Vector3.RIGHT.cross(Vector3.UP)
        assertEquals(Vector3(0f, 0f, 1f), result)
    }
}
