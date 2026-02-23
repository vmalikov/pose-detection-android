package com.simple.posedetection.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AngleUtilsTest {

    @Test
    fun rightAngle_returns90() {
        // Right angle at B: A=(0,0), B=(0,1), C=(1,1) -> BA=(0,-1), BC=(1,0) -> 90°
        val result = angleDegrees(
            a = 0f to 0f,
            b = 0f to 1f,
            c = 1f to 1f
        )
        assertEquals(90f, result!!, 0.01f)
    }

    @Test
    fun straightAngle_returns180() {
        // Straight line: A=(0,0), B=(1,0), C=(2,0) -> 180°
        val result = angleDegrees(
            a = 0f to 0f,
            b = 1f to 0f,
            c = 2f to 0f
        )
        assertEquals(180f, result!!, 0.01f)
    }

    @Test
    fun zeroAngle_returns0() {
        // Degenerate "zero" angle: A=(1,0), B=(0,0), C=(1,0) -> same direction
        val result = angleDegrees(
            a = 1f to 0f,
            b = 0f to 0f,
            c = 1f to 0f
        )
        assertEquals(0f, result!!, 0.01f)
    }

    @Test
    fun degenerate_zeroLengthBa_returnsNull() {
        val result = angleDegrees(
            a = 1f to 1f,
            b = 1f to 1f,
            c = 2f to 2f
        )
        assertNull(result)
    }

    @Test
    fun degenerate_zeroLengthBc_returnsNull() {
        val result = angleDegrees(
            a = 0f to 0f,
            b = 2f to 2f,
            c = 2f to 2f
        )
        assertNull(result)
    }

    @Test
    fun fortyFiveDegrees_returns45() {
        // 45° at B: A=(1,0), B=(0,0), C=(1,1) -> BA=(1,0), BC=(1,1) -> angle 45°
        val result = angleDegrees(
            a = 1f to 0f,
            b = 0f to 0f,
            c = 1f to 1f
        )
        assertEquals(45f, result!!, 0.5f)
    }
}
