package com.simple.posedetection.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.simple.posedetection.domain.model.VideoFrame
import com.simple.posedetection.domain.port.FrameSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CameraFrameSource @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val previewSurfaceProvider: Preview.SurfaceProvider
) : FrameSource {

    @AssistedFactory
    interface Factory {
        fun create(
            lifecycleOwner: LifecycleOwner,
            previewSurfaceProvider: Preview.SurfaceProvider
        ): CameraFrameSource
    }

    private companion object {
        val CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
        val TARGET_RESOLUTION = Size(480, 640)
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    override val frames: Flow<VideoFrame> = callbackFlow {

        // Get the camera provider — .get() is blocking, so switch off Main briefly
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }

        // --- Analysis use case ---
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(TARGET_RESOLUTION)
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            // YUV_420_888 is the fastest path for bitmap conversion on most devices
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    // Only send if the channel is open (collector is still active)
                    if (!isClosedForSend) {
                        val frame = imageProxy.toVideoFrame()
                        trySend(frame) // non-blocking; drops the frame if buffer is full
                    }
                    imageProxy.close()
                }
            }

        // --- Preview use case ---
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewSurfaceProvider
        }

        // callbackFlow runs on Main (FramePipeline.scope uses Dispatchers.Main),
        // so bindToLifecycle is always called on the main thread — safe for LEGACY devices.
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CAMERA_SELECTOR,
            preview,
            imageAnalysis
        )

        // When the collector cancels, release everything.
        // awaitClose also runs on Main (same scope), so unbindAll is safe here too.
        awaitClose {
            cameraProvider.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    // ── Extension: ImageProxy → VideoFrame ────────────────────────────────────

    private fun ImageProxy.toVideoFrame(): VideoFrame {
        val degrees = imageInfo.rotationDegrees
        val raw = toBitmap()
        val rotated = if (degrees != 0) {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                .also { raw.recycle() }
        } else raw
        return VideoFrame(bitmap = rotated, timestampMs = System.currentTimeMillis())
    }
}
