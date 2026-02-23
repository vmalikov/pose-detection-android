package com.simple.posedetection.domain.exercise.validator

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Severity
import com.simple.posedetection.domain.exercise.ValidationCode
import com.simple.posedetection.domain.model.PoseFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackAngleValidatorTest {

    private fun features(torsoAngle: Float?) = PoseFeatures(
        kneeAngleLeft = null,
        kneeAngleRight = null,
        hipAngleLeft = null,
        hipAngleRight = null,
        ankleAngleLeft = null,
        ankleAngleRight = null,
        torsoAngle = torsoAngle,
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

    private val validator = BackAngleValidator(config)

    @Test
    fun validate_nullTorso_returnsValid() {
        val f = features(null)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
        assertEquals(Severity.INFO, r.severity)
    }

    @Test
    fun validate_torsoWithinTolerance_returnsValid() {
        val f = features(20f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }

    @Test
    fun validate_torsoNegativeWithinTolerance_returnsValid() {
        val f = features(-20f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }

    @Test
    fun validate_torsoExceedsTolerance_returnsInvalid() {
        val f = features(30f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertFalse(r.isValid)
        assertEquals("Keep torso upright", r.message)
        assertEquals(Severity.WARNING, r.severity)
        assertEquals(ValidationCode.TORSO_DEVIATION, r.code)
    }

    @Test
    fun validate_torsoNegativeExceedsTolerance_returnsInvalid() {
        val f = features(-30f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertFalse(r.isValid)
    }

    @Test
    fun validate_torsoExactlyAtTolerance_returnsValid() {
        val f = features(25f)
        val r = validator.validate(f, Phase.BOTTOM)
        assertTrue(r.isValid)
    }
}
