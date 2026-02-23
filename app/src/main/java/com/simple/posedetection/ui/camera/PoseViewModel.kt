package com.simple.posedetection.ui.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.posedetection.data.camera.CameraFrameSource
import com.simple.posedetection.data.detector.PoseDetector
import com.simple.posedetection.domain.model.PoseFrameResult
import com.simple.posedetection.domain.pipeline.FramePipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PoseViewModel"

sealed class PoseUiState {
    object Initializing : PoseUiState()
    data class Active(val result: PoseFrameResult) : PoseUiState()
    data class Error(val message: String) : PoseUiState()
}

@HiltViewModel
class PoseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    // dagger.Lazy defers PoseDetector construction to first .get() call.
    // We call it from Dispatchers.Default so the expensive GPU init never blocks Main.
    private val detectorLazy: dagger.Lazy<PoseDetector>
) : ViewModel() {

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
                val det = detectorLazy.get()
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

                val source = CameraFrameSource(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewSurfaceProvider = surfaceProvider
                )
                val pipeline = FramePipeline(source, det)

                Log.d(TAG, "startCamera: pipeline created, collecting results...")
                pipeline.results.collect { result ->
                    _uiState.value = PoseUiState.Active(result)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
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
