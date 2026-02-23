package com.simple.posedetection.domain.exercise

data class ValidationResult(
    val isValid: Boolean,
    val message: String?,
    val severity: Severity,
    val code: ValidationCode? = null
)

enum class Severity {
    INFO,
    WARNING,
    ERROR
}

enum class ValidationCode {
    DEPTH_NOT_REACHED,
    TORSO_DEVIATION,
    KNEE_OVER_TOE
}
