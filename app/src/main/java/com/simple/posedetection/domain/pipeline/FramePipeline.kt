package com.simple.posedetection.domain.pipeline

import com.simple.posedetection.domain.model.PoseFrameResult
import com.simple.posedetection.domain.port.FrameSource
import com.simple.posedetection.domain.port.PoseDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

class FramePipeline(
    private val frameSource: FrameSource,
    private val detector: PoseDetector,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val inferenceDispatcher: CoroutineDispatcher = Dispatchers.Default,
    stopTimeoutMillis: Long = 500L
) : java.io.Closeable {

    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    val results: Flow<PoseFrameResult> = frameSource.frames
        // Keep only the latest frame — drops buffered frames when inference is slower than
        // the camera frame rate, preventing the skeleton from lagging behind a moving camera.
        .conflate()
        .mapNotNull { frame ->
            withContext(inferenceDispatcher) {
                val startMs = System.currentTimeMillis()
                val pose = detector.detect(frame.bitmap)
                PoseFrameResult(
                    frame = frame,
                    pose = pose,
                    inferenceTimeMs = System.currentTimeMillis() - startMs
                )
            }
        }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            replay = 1
        )

    override fun close() {
        scope.cancel()
    }
}