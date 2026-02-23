package com.simple.posedetection.domain.processor

import com.simple.posedetection.domain.model.BodyPart
import com.simple.posedetection.domain.model.Keypoint
import com.simple.posedetection.domain.model.PoseResult
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PoseProcessorTest {

    private val processor = PoseProcessor(PoseProcessorConfig(confidenceThreshold = 0.4f))

    private fun keypoint(x: Float, y: Float, score: Float = 0.8f) = Keypoint(x, y, score)

    @Test
    fun process_validPose_returnsPoseFeatures() {
        val keypoints = mapOf(
            BodyPart.LEFT_HIP to keypoint(0.4f, 0.6f),
            BodyPart.RIGHT_HIP to keypoint(0.6f, 0.6f),
            BodyPart.LEFT_SHOULDER to keypoint(0.4f, 0.35f),
            BodyPart.RIGHT_SHOULDER to keypoint(0.6f, 0.35f),
            BodyPart.LEFT_KNEE to keypoint(0.4f, 0.75f),
            BodyPart.RIGHT_KNEE to keypoint(0.6f, 0.75f),
            BodyPart.LEFT_ANKLE to keypoint(0.4f, 0.9f),
            BodyPart.RIGHT_ANKLE to keypoint(0.6f, 0.9f),
        )
        val pose = PoseResult(keypoints)
        val features = processor.process(pose)
        assertNotNull(features)
        assertNotNull(features!!.kneeAngleAvg())
        assertNotNull(features.kneeAngleMax())
        assertNotNull(features.torsoAngle)
    }

    @Test
    fun process_lowConfidenceHip_returnsNull() {
        val keypoints = mapOf(
            BodyPart.LEFT_HIP to keypoint(0.4f, 0.6f, 0.8f),
            BodyPart.RIGHT_HIP to keypoint(0.6f, 0.6f, 0.2f),
            BodyPart.LEFT_SHOULDER to keypoint(0.4f, 0.35f),
            BodyPart.RIGHT_SHOULDER to keypoint(0.6f, 0.35f),
        )
        val pose = PoseResult(keypoints)
        val features = processor.process(pose)
        assertNull(features)
    }

    @Test
    fun process_missingShoulder_returnsNull() {
        val keypoints = mapOf(
            BodyPart.LEFT_HIP to keypoint(0.4f, 0.6f),
            BodyPart.RIGHT_HIP to keypoint(0.6f, 0.6f),
            BodyPart.LEFT_SHOULDER to keypoint(0.4f, 0.35f),
        )
        val pose = PoseResult(keypoints)
        val features = processor.process(pose)
        assertNull(features)
    }
}
