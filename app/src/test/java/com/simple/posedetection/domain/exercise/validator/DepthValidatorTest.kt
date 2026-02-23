package com.simple.posedetection.domain.exercise.validator

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Severity
import com.simple.posedetection.domain.model.PoseFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthValidatorTest {

    private fun features(kneeAngleLeft: Float?, kneeAngleRight: Float?) = PoseFeatures(
        kneeAngleLeft = kneeAngleLeft,
        kneeAngleRight = kneeAngleRight,
        hipAngleLeft = null,
        hipAngleRight = null,
        ankleAngleLeft = null,
        ankleAngleRight = null,
        torsoAngle = null,
        hipVerticalDisplacement = null,
        kneeHorizontalDisplacementLeft = null,
        kneeHorizontalDisplacementRight = null,
    )

    private val config = ExerciseConfig(
        depthKneeAngleDeg = 100f,
        torsoToleranceDeg = 25f,
        debounceFrames = 2,
        standingKneeAngleDeg = 140f
    )

    private val validator = DepthValidator(config)

    @Test
    fun validate_notBottomPhase_returnsValid() {
        val f = features(85f, 90f)
        listOf(Phase.START, Phase.DESCENDING, Phase.ASCENDING, Phase.END).forEach { phase ->
            val r = validator.validate(f, phase)
            assertTrue(r.isValid)
            assertEquals(Severity.INFO, r.severity)
        }
    }

    @Test
    fun validate_bottomPhase_kneeAtDepth_returnsValid() {
        val f = features(95f, 98f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
        assertEquals(Severity.INFO, r.severity)
    }

    @Test
    fun validate_bottomPhase_kneeAboveDepth_returnsInvalid() {
        val f = features(105f, 110f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertFalse(r.isValid)
        assertEquals("Depth not reached", r.message)
        assertEquals(Severity.WARNING, r.severity)
    }

    @Test
    fun validate_bottomPhase_kneeExactlyAtThreshold_returnsValid() {
        val f = features(100f, 100f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }

    @Test
    fun validate_bottomPhase_nullKnee_returnsValid() {
        val f = features(null, null)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }
}
