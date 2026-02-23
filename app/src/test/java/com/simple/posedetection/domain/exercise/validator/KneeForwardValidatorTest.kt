package com.simple.posedetection.domain.exercise.validator

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Severity
import com.simple.posedetection.domain.model.PoseFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KneeForwardValidatorTest {

    private fun features(
        kneeHorizontalDisplacementLeft: Float? = null,
        kneeHorizontalDisplacementRight: Float? = null
    ) = PoseFeatures(
        kneeAngleLeft = null,
        kneeAngleRight = null,
        hipAngleLeft = null,
        hipAngleRight = null,
        ankleAngleLeft = null,
        ankleAngleRight = null,
        torsoAngle = null,
        hipVerticalDisplacement = null,
        kneeHorizontalDisplacementLeft = kneeHorizontalDisplacementLeft,
        kneeHorizontalDisplacementRight = kneeHorizontalDisplacementRight,
    )

    private val config = ExerciseConfig(
        depthKneeAngleDeg = 100f,
        torsoToleranceDeg = 25f,
        kneeOverToeTolerance = 0.03f,
        debounceFrames = 2,
        standingKneeAngleDeg = 140f
    )

    private val validator = KneeForwardValidator(config)

    @Test
    fun validate_bothWithinTolerance_returnsValid() {
        val f = features(0.02f, 0.01f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
        assertEquals(Severity.INFO, r.severity)
    }

    @Test
    fun validate_leftExceedsTolerance_returnsInvalid() {
        val f = features(0.05f, 0.01f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertFalse(r.isValid)
        assertEquals("Knee over toe", r.message)
        assertEquals(Severity.WARNING, r.severity)
    }

    @Test
    fun validate_rightExceedsTolerance_returnsInvalid() {
        val f = features(0.01f, 0.04f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertFalse(r.isValid)
    }

    @Test
    fun validate_bothNull_returnsValid() {
        val f = features(null, null)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }

    @Test
    fun validate_exactlyAtTolerance_returnsValid() {
        val f = features(0.03f, 0.03f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }
}
