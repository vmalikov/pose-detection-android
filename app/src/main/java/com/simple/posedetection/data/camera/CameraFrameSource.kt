package com.simple.posedetection.data.camera

import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CameraFrameSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    private val targetResolution: Size = Size(480, 640),
    private val previewSurfaceProvider: Preview.SurfaceProvider? = null
) : FrameSource {

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    override val frames: Flow<VideoFrame> = callbackFlow {

        // Get the camera provider — .get() is blocking, so switch off Main briefly
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }

        // -- Analysis use case ---
        val imageAnalysis = ImageAnalysis.Builder()
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetResolution(targetResolution)
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

        // --- optional preview use case ---
        val useCases = buildList {
            if (previewSurfaceProvider != null) {
                add(Preview.Builder().build().also {
                    it.surfaceProvider = previewSurfaceProvider
                })
            }
            add(imageAnalysis)
        }

        // callbackFlow runs on Main (FramePipeline.scope uses Dispatchers.Main),
        // so bindToLifecycle is always called on the main thread — safe for LEGACY devices.
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            *useCases.toTypedArray()
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
        val bitmap = toBitmapSafe()
        return VideoFrame(
            bitmap = bitmap,
            timestampMs = System.currentTimeMillis(),
            rotationDegrees = imageInfo.rotationDegrees
        )
    }

    /**
     * Converts YUV_420_888 ImageProxy to an ARGB_8888 Bitmap.
     *
     * Using [ImageProxy.toBitmap] (CameraX 1.3+) is the simplest path.
     * For older CameraX or finer control over the conversion you can do the
     * YUV→RGB math manually, but in practice toBitmap() is well-optimised.
     */
    private fun ImageProxy.toBitmapSafe(): android.graphics.Bitmap {
        return toBitmap()
    }
}
