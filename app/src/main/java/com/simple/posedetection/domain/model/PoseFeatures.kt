package com.simple.posedetection.domain.model

/**
 * Biomechanical features derived from a normalized pose.
 * Normalization: origin at hip midpoint, scale by torso length (shoulder-mid to hip-mid).
 * Keypoints with score < [PoseProcessorConfig.defaultConfidenceThreshold] are excluded.
 * All values are null when the feature cannot be computed (e.g. low confidence).
 */
data class PoseFeatures(
    val kneeAngleLeft: Float?,
    val kneeAngleRight: Float?,
    val hipAngleLeft: Float?,
    val hipAngleRight: Float?,
    val ankleAngleLeft: Float?,
    val ankleAngleRight: Float?,
    val torsoAngle: Float?,
    /** Hip midpoint Y in original normalized frame space [0,1]; state machine can diff vs first frame for displacement. */
    val hipVerticalDisplacement: Float?,
    /** Per-leg: knee X minus ankle X in normalized space (positive = knee forward of ankle). Left, then right. */
    val kneeHorizontalDisplacementLeft: Float?,
    val kneeHorizontalDisplacementRight: Float?,
) {
    /** Average knee angle (for squat depth); null if both sides null. */
    fun kneeAngleAvg(): Float? {
        val l = kneeAngleLeft
        val r = kneeAngleRight
        return when {
            l != null && r != null -> (l + r) / 2f
            l != null -> l
            r != null -> r
            else -> null
        }
    }

    /**
     * Max of the two knee angles. Use for standing detection in side view:
     * when at least one leg is extended (max >= threshold), treat as standing.
     * Avoids occluded/far leg dragging the average down.
     */
    fun kneeAngleMax(): Float? {
        val l = kneeAngleLeft
        val r = kneeAngleRight
        return when {
            l != null && r != null -> maxOf(l, r)
            l != null -> l
            r != null -> r
            else -> null
        }
    }
}
