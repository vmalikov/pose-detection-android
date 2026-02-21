package com.simple.posedetection.domain.port

import com.simple.posedetection.domain.model.VideoFrame
import kotlinx.coroutines.flow.Flow

interface FrameSource {
    val frames: Flow<VideoFrame>
}