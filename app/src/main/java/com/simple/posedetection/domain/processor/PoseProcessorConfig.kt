package com.simple.posedetection.domain.processor

/**
 * Configuration for pose processing and normalization.
 * Confidence threshold: keypoints below this score are ignored.
 * Recommended default 0.4 for device/camera independence.
 */
data class PoseProcessorConfig(
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.4f
    }
}
