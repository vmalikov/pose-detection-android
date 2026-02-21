package com.simple.posedetection.domain.model

data class PoseFrameResult(
    val frame: VideoFrame,
    val pose: PoseResult,
    val inferenceTimeMs: Long
)