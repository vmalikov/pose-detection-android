package com.simple.posedetection.domain.exercise.validator

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Severity
import com.simple.posedetection.domain.exercise.ValidationCode
import com.simple.posedetection.domain.exercise.ValidationResult
import com.simple.posedetection.domain.exercise.Validator
import com.simple.posedetection.domain.model.PoseFeatures

/**
 * Side-view rule: knee must not pass toe line.
 * Uses ankle as toe proxy. Fails when knee.x > ankle.x + tolerance
 * (i.e. kneeHorizontalDisplacement > tolerance in normalized space).
 */
class KneeForwardValidator(
    private val config: ExerciseConfig
) : Validator {
    override fun validate(features: PoseFeatures, phase: Phase): ValidationResult {
        val tol = config.kneeOverToeTolerance
        val left = features.kneeHorizontalDisplacementLeft
        val right = features.kneeHorizontalDisplacementRight
        val leftFail = left != null && left > tol
        val rightFail = right != null && right > tol
        return if (leftFail || rightFail) {
            ValidationResult(
                isValid = false,
                message = "Knee over toe",
                severity = Severity.WARNING,
                code = ValidationCode.KNEE_OVER_TOE
            )
        } else {
            ValidationResult(isValid = true, message = null, severity = Severity.INFO, code = null)
        }
    }
}
