package com.simple.posedetection.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoseFeaturesTest {

    private fun features(
        kneeAngleLeft: Float? = null,
        kneeAngleRight: Float? = null,
        torsoAngle: Float? = null,
        kneeHorizontalDisplacementLeft: Float? = null,
        kneeHorizontalDisplacementRight: Float? = null
    ) = PoseFeatures(
        kneeAngleLeft = kneeAngleLeft,
        kneeAngleRight = kneeAngleRight,
        hipAngleLeft = null,
        hipAngleRight = null,
        ankleAngleLeft = null,
        ankleAngleRight = null,
        torsoAngle = torsoAngle,
        hipVerticalDisplacement = null,
        kneeHorizontalDisplacementLeft = kneeHorizontalDisplacementLeft,
        kneeHorizontalDisplacementRight = kneeHorizontalDisplacementRight,
    )

    @Test
    fun kneeAngleAvg_bothNull_returnsNull() {
        val f = features(kneeAngleLeft = null, kneeAngleRight = null)
        assertNull(f.kneeAngleAvg())
    }

    @Test
    fun kneeAngleAvg_leftOnly_returnsLeft() {
        val f = features(kneeAngleLeft = 90f, kneeAngleRight = null)
        assertEquals(90f, f.kneeAngleAvg()!!, 0.01f)
    }

    @Test
    fun kneeAngleAvg_rightOnly_returnsRight() {
        val f = features(kneeAngleLeft = null, kneeAngleRight = 100f)
        assertEquals(100f, f.kneeAngleAvg()!!, 0.01f)
    }

    @Test
    fun kneeAngleAvg_bothPresent_returnsAverage() {
        val f = features(kneeAngleLeft = 80f, kneeAngleRight = 100f)
        assertEquals(90f, f.kneeAngleAvg()!!, 0.01f)
    }

    @Test
    fun kneeAngleMax_bothNull_returnsNull() {
        val f = features(kneeAngleLeft = null, kneeAngleRight = null)
        assertNull(f.kneeAngleMax())
    }

    @Test
    fun kneeAngleMax_leftOnly_returnsLeft() {
        val f = features(kneeAngleLeft = 160f, kneeAngleRight = null)
        assertEquals(160f, f.kneeAngleMax()!!, 0.01f)
    }

    @Test
    fun kneeAngleMax_bothPresent_returnsMax() {
        val f = features(kneeAngleLeft = 140f, kneeAngleRight = 160f)
        assertEquals(160f, f.kneeAngleMax()!!, 0.01f)
    }

    @Test
    fun kneeAngleMax_bothPresent_returnsMax_reversed() {
        val f = features(kneeAngleLeft = 170f, kneeAngleRight = 90f)
        assertEquals(170f, f.kneeAngleMax()!!, 0.01f)
    }
}
