package com.simple.posedetection.detector


// Each detected joint with normalized coordinates (0.0 to 1.0)
// and a confidence score. Normalized coords are important because
// they're resolution-independent — the camera frame might be a
// different size than the model input, and you don't want to
// re-map coordinates in 3 different places later.
data class Keypoint(
    val x: Float,
    val y: Float,
    val score: Float
)

// The 17 COCO keypoints in order — MoveNet always returns them in this order
enum class BodyPart {
    NOSE, LEFT_EYE, RIGHT_EYE, LEFT_EAR, RIGHT_EAR,
    LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST, LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE
}

data class PoseResult(val keypoints: Map<BodyPart, Keypoint>)


object SkeletonConnections {
    val connections = listOf(
        // Head
        BodyPart.LEFT_EAR to BodyPart.LEFT_EYE,
        BodyPart.LEFT_EYE to BodyPart.NOSE,
        BodyPart.NOSE to BodyPart.RIGHT_EYE,
        BodyPart.RIGHT_EYE to BodyPart.RIGHT_EAR,

        // Torso
        BodyPart.LEFT_SHOULDER to BodyPart.RIGHT_SHOULDER,
        BodyPart.LEFT_SHOULDER to BodyPart.LEFT_HIP,
        BodyPart.RIGHT_SHOULDER to BodyPart.RIGHT_HIP,
        BodyPart.LEFT_HIP to BodyPart.RIGHT_HIP,

        // Left arm
        BodyPart.LEFT_SHOULDER to BodyPart.LEFT_ELBOW,
        BodyPart.LEFT_ELBOW to BodyPart.LEFT_WRIST,

        // Right arm
        BodyPart.RIGHT_SHOULDER to BodyPart.RIGHT_ELBOW,
        BodyPart.RIGHT_ELBOW to BodyPart.RIGHT_WRIST,

        // Left leg
        BodyPart.LEFT_HIP to BodyPart.LEFT_KNEE,
        BodyPart.LEFT_KNEE to BodyPart.LEFT_ANKLE,

        // Right leg
        BodyPart.RIGHT_HIP to BodyPart.RIGHT_KNEE,
        BodyPart.RIGHT_KNEE to BodyPart.RIGHT_ANKLE
    )
}