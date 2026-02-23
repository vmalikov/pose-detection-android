package com.simple.posedetection.domain.port

import android.graphics.Bitmap
import com.simple.posedetection.domain.model.DeviceCapabilities
import com.simple.posedetection.domain.model.PoseResult

interface PoseDetector {
    val capabilities: DeviceCapabilities
    fun detect(bitmap: Bitmap): PoseResult
    fun close()
}
