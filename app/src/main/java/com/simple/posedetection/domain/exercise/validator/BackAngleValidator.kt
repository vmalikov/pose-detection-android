package com.simple.posedetection.domain.exercise.validator

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Severity
import com.simple.posedetection.domain.exercise.ValidationResult
import com.simple.posedetection.domain.exercise.Validator
import com.simple.posedetection.domain.model.PoseFeatures
import kotlin.math.abs

/**
 * Fails when torso angle deviation from vertical exceeds config tolerance.
 */
class BackAngleValidator(
    private val config: ExerciseConfig
) : Validator {
    override fun validate(features: PoseFeatures, phase: Phase): ValidationResult {
        val torso = features.torsoAngle ?: return ValidationResult(
            isValid = true,
            message = null,
            severity = Severity.INFO
        )
        return if (abs(torso) > config.torsoToleranceDeg) {
            ValidationResult(
                isValid = false,
                message = "Keep torso upright",
                severity = Severity.WARNING
            )
        } else {
            ValidationResult(isValid = true, message = null, severity = Severity.INFO)
        }
    }
}
