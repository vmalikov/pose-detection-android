package com.simple.posedetection.domain.pipeline

import com.simple.posedetection.domain.exercise.ExerciseDefinition
import com.simple.posedetection.domain.exercise.ExerciseStateMachine
import com.simple.posedetection.domain.model.ExerciseFrameResult
import com.simple.posedetection.domain.model.ProcessedPoseFrameResult
import com.simple.posedetection.domain.model.PoseFrameResult
import com.simple.posedetection.domain.port.FrameSource
import com.simple.posedetection.domain.port.PoseDetector
import com.simple.posedetection.domain.processor.PoseProcessor
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
    private val poseProcessor: PoseProcessor? = null,
    private val exerciseDefinition: ExerciseDefinition? = null,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val inferenceDispatcher: CoroutineDispatcher = Dispatchers.Default,
    stopTimeoutMillis: Long = 500L
) : java.io.Closeable {

    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val stateMachine = exerciseDefinition?.let { ExerciseStateMachine(it.config) }
    private val validators = exerciseDefinition?.validators ?: emptyList()

    val results: Flow<ProcessedPoseFrameResult> = frameSource.frames
        .conflate()
        .mapNotNull { frame ->
            withContext(inferenceDispatcher) {
                val startMs = System.currentTimeMillis()
                val pose = detector.detect(frame.bitmap)
                val poseFrameResult = PoseFrameResult(
                    frame = frame,
                    pose = pose,
                    inferenceTimeMs = System.currentTimeMillis() - startMs
                )
                val exerciseResult = if (poseProcessor != null && stateMachine != null) {
                    val features = poseProcessor.process(pose)
                    val state = stateMachine.update(features)
                    val results = features?.let { f ->
                        validators.map { it.validate(f, state.currentPhase) }
                    } ?: emptyList()
                    ExerciseFrameResult(
                        phase = state.currentPhase,
                        repCount = state.repCount,
                        validationResults = results
                    )
                } else null
                ProcessedPoseFrameResult(poseFrameResult, exerciseResult)
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