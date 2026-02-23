package com.simple.posedetection.ui.camera

import com.simple.posedetection.domain.model.DeviceCapabilities
import com.simple.posedetection.domain.model.ExerciseFrameResult
import com.simple.posedetection.domain.model.PoseResult

sealed class PoseUiState {
    data object Initializing : PoseUiState()
    data class Active(
        val pose: PoseResult,
        val frameWidth: Int,
        val frameHeight: Int,
        val inferenceTimeMs: Long,
        val capabilities: DeviceCapabilities,
        val exerciseResult: ExerciseFrameResult? = null
    ) : PoseUiState()
    data class Error(val message: String) : PoseUiState()
}