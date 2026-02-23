package com.simple.posedetection.domain.model

import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.ValidationResult

data class ExerciseFrameResult(
    val phase: Phase,
    val repCount: Int,
    val validationResults: List<ValidationResult>
)
