package com.simple.posedetection.domain.pipeline

import android.graphics.Bitmap
import android.graphics.Matrix
import com.simple.posedetection.data.detector.PoseDetector
import com.simple.posedetection.domain.model.PoseFrameResult
import com.simple.posedetection.domain.port.FrameSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

class FramePipeline(
    private val frameSource: FrameSource,
    private val detector: PoseDetector,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    stopTimeoutMillis: Long = 500L
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    val results: Flow<PoseFrameResult> = frameSource.frames
        .mapNotNull { frame ->
            // Run inference on the Default dispatcher — scope runs on Main for CameraX,
            // so we explicitly switch here for the CPU/GPU-bound inference work.
            withContext(Dispatchers.Default) {
                // Camera sensor delivers a landscape frame; rotate it to match display orientation
                // so keypoint coordinates are in the same space as the preview.
                val inputBitmap = frame.bitmap.rotatedBy(frame.rotationDegrees)
                val startMs = System.currentTimeMillis()
                val pose = detector.detect(inputBitmap)
                val inferenceMs = System.currentTimeMillis() - startMs

                PoseFrameResult(
                    frame = frame,
                    pose = pose,
                    inferenceTimeMs = inferenceMs
                )
            }
        }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            replay = 1
        )
}

private fun Bitmap.rotatedBy(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}