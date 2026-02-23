package com.simple.posedetection.domain.processor

import com.simple.posedetection.domain.model.BodyPart
import com.simple.posedetection.domain.model.Keypoint
import com.simple.posedetection.domain.model.PoseFeatures
import com.simple.posedetection.domain.model.PoseResult
import com.simple.posedetection.domain.util.angleDegrees
import kotlin.math.sqrt

/**
 * Converts [PoseResult] into [PoseFeatures] using normalization and [AngleUtils].
 * Returns null when the frame is rejected (critical keypoints below confidence threshold).
 *
 * Normalization: translate origin to hip midpoint, scale by torso length (shoulder-mid to hip-mid).
 * Target: < 0.2 ms per frame.
 */
class PoseProcessor(
    private val config: PoseProcessorConfig = PoseProcessorConfig()
) {
    private val threshold = config.confidenceThreshold

    fun process(pose: PoseResult): PoseFeatures? {
        val k = pose.keypoints
        val leftHip = k[BodyPart.LEFT_HIP]
        val rightHip = k[BodyPart.RIGHT_HIP]
        val leftShoulder = k[BodyPart.LEFT_SHOULDER]
        val rightShoulder = k[BodyPart.RIGHT_SHOULDER]
        if (leftHip == null || rightHip == null || leftShoulder == null || rightShoulder == null) return null
        if (leftHip.score < threshold || rightHip.score < threshold ||
            leftShoulder.score < threshold || rightShoulder.score < threshold
        ) return null

        val hipMid = Pair(
            (leftHip.x + rightHip.x) / 2f,
            (leftHip.y + rightHip.y) / 2f
        )
        val shoulderMid = Pair(
            (leftShoulder.x + rightShoulder.x) / 2f,
            (leftShoulder.y + rightShoulder.y) / 2f
        )
        val torsoLength = distance(shoulderMid, hipMid)
        if (torsoLength < 1e-6f) return null

        fun norm(p: Pair<Float, Float>): Pair<Float, Float> {
            val tx = (p.first - hipMid.first) / torsoLength
            val ty = (p.second - hipMid.second) / torsoLength
            return tx to ty
        }

        fun pt(part: BodyPart): Pair<Float, Float>? {
            val kp = k[part] ?: return null
            if (kp.score < threshold) return null
            return norm(kp.x to kp.y)
        }

        val lHip = pt(BodyPart.LEFT_HIP)!!
        val rHip = pt(BodyPart.RIGHT_HIP)!!
        val lSh = pt(BodyPart.LEFT_SHOULDER)!!
        val rSh = pt(BodyPart.RIGHT_SHOULDER)!!
        val lKn = pt(BodyPart.LEFT_KNEE)
        val rKn = pt(BodyPart.RIGHT_KNEE)
        val lAn = pt(BodyPart.LEFT_ANKLE)
        val rAn = pt(BodyPart.RIGHT_ANKLE)

        val kneeAngleLeft = if (lKn != null && lAn != null) angleDegrees(lHip, lKn, lAn) else null
        val kneeAngleRight = if (rKn != null && rAn != null) angleDegrees(rHip, rKn, rAn) else null
        val hipAngleLeft = if (lKn != null) angleDegrees(lSh, lHip, lKn) else null
        val hipAngleRight = if (rKn != null) angleDegrees(rSh, rHip, rKn) else null

        val ankleAngleLeft = if (lKn != null && lAn != null) {
            val below = Pair(lAn.first, lAn.second + 0.1f)
            angleDegrees(lKn, lAn, below)
        } else null
        val ankleAngleRight = if (rKn != null && rAn != null) {
            val below = Pair(rAn.first, rAn.second + 0.1f)
            angleDegrees(rKn, rAn, below)
        } else null

        val (sx, sy) = norm(shoulderMid)
        val torsoAngle = angleDegrees(
            Pair(sx, sy - 1f),
            Pair(sx, sy),
            Pair(0f, 0f)
        )

        val hipMidY = hipMid.second

        val kneeHorizontalDisplacementLeft = if (lKn != null && lAn != null) lKn.first - lAn.first else null
        val kneeHorizontalDisplacementRight = if (rKn != null && rAn != null) rKn.first - rAn.first else null

        return PoseFeatures(
            kneeAngleLeft = kneeAngleLeft,
            kneeAngleRight = kneeAngleRight,
            hipAngleLeft = hipAngleLeft,
            hipAngleRight = hipAngleRight,
            ankleAngleLeft = ankleAngleLeft,
            ankleAngleRight = ankleAngleRight,
            torsoAngle = torsoAngle,
            hipVerticalDisplacement = hipMidY,
            kneeHorizontalDisplacementLeft = kneeHorizontalDisplacementLeft,
            kneeHorizontalDisplacementRight = kneeHorizontalDisplacementRight,
        )
    }

    private fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val dx = a.first - b.first
        val dy = a.second - b.second
        return sqrt(dx * dx + dy * dy)
    }
}
