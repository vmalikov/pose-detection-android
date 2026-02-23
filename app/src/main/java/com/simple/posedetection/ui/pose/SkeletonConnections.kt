package com.simple.posedetection.ui.pose

import com.simple.posedetection.domain.model.BodyPart

// Rendering-only data: which joints to connect when drawing the skeleton overlay.
internal object SkeletonConnections {
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
