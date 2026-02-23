package com.simple.posedetection.domain.exercise

data class ExerciseState(
    val currentPhase: Phase,
    val repCount: Int,
    val debounceCounter: Int = 0,
    val lastCandidatePhase: Phase? = null
)
