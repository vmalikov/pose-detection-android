package com.simple.posedetection.domain.model

import android.graphics.Bitmap

data class VideoFrame(
    val bitmap: Bitmap,
    val timestampMs: Long,
    val rotationDegrees: Int
)