package com.simple.posedetection.domain.exercise

data class ValidationResult(
    val isValid: Boolean,
    val message: String?,
    val severity: Severity
)

enum class Severity {
    INFO,
    WARNING,
    ERROR
}
