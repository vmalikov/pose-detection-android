package com.simple.posedetection.domain.exercise

data class ExerciseDefinition(
    val name: String,
    val phases: List<Phase>,
    val validators: List<Validator>,
    val config: ExerciseConfig
)
