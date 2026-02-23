package com.simple.posedetection.domain.model

/**
 * Result of inference plus optional exercise layer.
 * When exercise is disabled, [exerciseResult] is null and this is equivalent to [PoseFrameResult].
 */
data class ProcessedPoseFrameResult(
    val poseFrameResult: PoseFrameResult,
    val exerciseResult: ExerciseFrameResult? = null
) {
    val frame: VideoFrame get() = poseFrameResult.frame
    val pose: PoseResult get() = poseFrameResult.pose
    val inferenceTimeMs: Long get() = poseFrameResult.inferenceTimeMs
}
