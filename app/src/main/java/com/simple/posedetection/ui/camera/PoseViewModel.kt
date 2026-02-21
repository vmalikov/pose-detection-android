package com.simple.posedetection.ui.camera

import android.app.Application
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.simple.posedetection.data.camera.CameraFrameSource
import com.simple.posedetection.data.detector.PoseDetector
import com.simple.posedetection.domain.model.PoseFrameResult
import com.simple.posedetection.domain.pipeline.FramePipeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PoseViewModel"

sealed class PoseUiState {
    object Initializing : PoseUiState()
    data class Active(val result: PoseFrameResult) : PoseUiState()
    data class Error(val message: String) : PoseUiState()
}

class PoseViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PoseUiState>(PoseUiState.Initializing)
    val uiState: StateFlow<PoseUiState> = _uiState.asStateFlow()

    @Volatile private var detector: PoseDetector? = null
    val capabilities get() = detector?.capabilities

    private val detectorReady = CompletableDeferred<PoseDetector>()
    private var cameraJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Detector init started on ${Thread.currentThread().name}")
            try {
                val det = PoseDetector(getApplication())
                detector = det
                detectorReady.complete(det)
                Log.d(TAG, "Detector ready: gpu=${det.capabilities.hasGpu}, threads=${det.capabilities.optimalThreadCount}")
            } catch (t: Throwable) {
                Log.e(TAG, "Detector init FAILED: ${t.javaClass.simpleName}: ${t.message}", t)
                detectorReady.completeExceptionally(t)
                _uiState.value = PoseUiState.Error("Detector init failed: ${t.message}")
            }
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        cameraJob?.cancel()
        cameraJob = viewModelScope.launch {
            Log.d(TAG, "startCamera: waiting for detector...")
            try {
                val det = detectorReady.await()
                Log.d(TAG, "startCamera: detector ready, starting pipeline...")

                // CameraFrameSource owns all camera binding (Preview + Analysis).
                // Do NOT bind anything here separately — that would cause double-binding.
                val source = CameraFrameSource(
                    context = getApplication(),
                    lifecycleOwner = lifecycleOwner,
                    previewSurfaceProvider = surfaceProvider
                )
                val pipeline = FramePipeline(source, det)

                Log.d(TAG, "startCamera: pipeline created, collecting results...")
                pipeline.results.collect { result ->
                    _uiState.value = PoseUiState.Active(result)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t  // must not swallow cancellation
                Log.e(TAG, "startCamera error: ${t.javaClass.simpleName}: ${t.message}", t)
                _uiState.value = PoseUiState.Error("${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector?.close()
        Log.d(TAG, "ViewModel cleared, detector closed")
    }
}
