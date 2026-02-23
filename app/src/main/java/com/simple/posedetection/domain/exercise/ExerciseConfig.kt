package com.simple.posedetection.domain.exercise

/**
 * Thresholds and options for an exercise (e.g. squat).
 * Allows different configs for fitness (loose) vs rehab (strict).
 */
data class ExerciseConfig(
    val depthKneeAngleDeg: Float,
    val torsoToleranceDeg: Float,
    val kneeOverToeTolerance: Float = 0f,
    val debounceFrames: Int = 2,
    val minDepthAtBottom: Boolean = true,
    /** Knee angle above this is considered "standing" for START/END (e.g. 150°). */
    val standingKneeAngleDeg: Float = 150f
)
