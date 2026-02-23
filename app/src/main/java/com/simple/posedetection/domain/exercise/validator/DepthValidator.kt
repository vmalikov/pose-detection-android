package com.simple.posedetection.domain.exercise.validator

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Severity
import com.simple.posedetection.domain.exercise.ValidationResult
import com.simple.posedetection.domain.exercise.Validator
import com.simple.posedetection.domain.model.PoseFeatures

/**
 * Fails when in BOTTOM phase but knee angle is above the depth threshold
 * (squat not deep enough).
 */
class DepthValidator(
    private val config: ExerciseConfig
) : Validator {
    override fun validate(features: PoseFeatures, phase: Phase): ValidationResult {
        if (phase != Phase.BOTTOM) {
            return ValidationResult(isValid = true, message = null, severity = Severity.INFO)
        }
        val knee = features.kneeAngleAvg() ?: return ValidationResult(
            isValid = true,
            message = null,
            severity = Severity.INFO
        )
        return if (knee > config.depthKneeAngleDeg) {
            ValidationResult(
                isValid = false,
                message = "Depth not reached",
                severity = Severity.WARNING
            )
        } else {
            ValidationResult(isValid = true, message = null, severity = Severity.INFO)
        }
    }
}
