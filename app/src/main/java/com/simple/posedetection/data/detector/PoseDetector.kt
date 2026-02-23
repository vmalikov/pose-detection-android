package com.simple.posedetection.data.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.simple.posedetection.data.device.DeviceCapabilityDetector
import com.simple.posedetection.domain.model.BodyPart
import com.simple.posedetection.domain.model.Keypoint
import com.simple.posedetection.domain.model.PoseResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class PoseDetector(context: Context) {

    private val interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null

    // Stores timing from the last inference call - read this from UI
    var lastInferenceTimeMs: Long = 0L
        private set

    val capabilities = DeviceCapabilityDetector.detect()

    companion object {
        // MoveNet Lightning expects exactly 192x192 input
        // Thunder expects 256x256
        private val MODEL_INPUT_SIZE = 192
        private const val MODEL_NAME = "movenet-singlepose-lightning -4.tflite"
    }

    init {
        Log.d("PoseDetector", "init: capabilities probed — hasGpu=${capabilities.hasGpu}, threads=${capabilities.optimalThreadCount}, thread=${Thread.currentThread().name}")
        Log.d("PoseDetector", "init: loading model file...")
        val modelFile = loadModelFile(context, MODEL_NAME)
        Log.d("PoseDetector", "init: model loaded (${modelFile.limit()} bytes), building interpreter options...")
        val options = buildInterpreterOptions()
        Log.d("PoseDetector", "init: creating Interpreter...")
        interpreter = Interpreter(modelFile, options)
        Log.d("PoseDetector", "init: complete. ${buildCapabilityReport()}")
    }

    private fun buildInterpreterOptions(): Interpreter.Options = Interpreter.Options().apply {
        if (capabilities.hasGpu) {
            try {
                gpuDelegate = GpuDelegate()
                addDelegate(gpuDelegate!!)
                // GPU handles its own parallelism — numThreads has no effect here
                Log.d("PoseDetector", "Interpreter: GPU delegate active")
                return@apply
            } catch (t: Throwable) {
                // Probe passed but delegate creation failed at interpreter time.
                // Rare, but possible if driver state changed (e.g. thermal throttle, reboot).
                Log.w("PoseDetector", "GPU delegate failed at interpreter init, falling back to CPU: ${t.message}")
                gpuDelegate = null
            }
        }
        numThreads = capabilities.optimalThreadCount
        Log.d("PoseDetector", "Interpreter: CPU ×${capabilities.optimalThreadCount} threads")
    }

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        return FileInputStream(fileDescriptor.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun detect(bitmap: Bitmap): PoseResult {
        // Step 1: Letterbox the bitmap to a square before scaling.
        // Without this, a portrait/landscape bitmap gets squished into 192x192,
        // which distorts the image and hurts model accuracy.
        val (letterboxed, padX, padY) = letterbox(bitmap)
        val scaled = letterboxed.scale(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)

        // Step 2: Pack pixels into a UINT8 ByteBuffer — [H, W, C] order
        val inputBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        scaled.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
        for (pixel in pixels) {
            inputBuffer.put(((pixel shr 16) and 0xFF).toByte()) // R
            inputBuffer.put(((pixel shr 8) and 0xFF).toByte())  // G
            inputBuffer.put((pixel and 0xFF).toByte())          // B
        }
        inputBuffer.rewind()

        // Step 3: Prepare output — MoveNet output shape [1, 1, 17, 3]
        val outputBuffer = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }

        // Measure just the inference itself, not preprocessing
        // You want this number specifically — preprocessing time is
        // a separate optimization concern from model performance
        val startTime = SystemClock.elapsedRealtimeNanos()

        // Step 4: Run inference
        interpreter.run(inputBuffer, outputBuffer)

        lastInferenceTimeMs = (SystemClock.elapsedRealtimeNanos() - startTime) / 1_000_000

        // Step 5: Parse and remap coordinates back to the original (un-padded) image space
        return parseOutput(outputBuffer, padX, padY)
    }

    // Pads the shorter side so the bitmap becomes square, centered.
    // Returns the padded bitmap and the padding fraction on each axis:
    //   padX = fraction of the square width that is padding on ONE side (left or right)
    //   padY = fraction of the square height that is padding on ONE side (top or bottom)
    private fun letterbox(bitmap: Bitmap): Triple<Bitmap, Float, Float> {
        val size = maxOf(bitmap.width, bitmap.height)
        val padded = createBitmap(size, size)
        Canvas(padded).drawBitmap(
            bitmap,
            (size - bitmap.width) / 2f,
            (size - bitmap.height) / 2f,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        val padX = (size - bitmap.width).toFloat() / (2 * size)
        val padY = (size - bitmap.height).toFloat() / (2 * size)
        return Triple(padded, padX, padY)
    }

    fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }

    private fun parseOutput(
        raw: Array<Array<Array<FloatArray>>>,
        padX: Float,
        padY: Float
    ): PoseResult {
        val keypoints = mutableMapOf<BodyPart, Keypoint>()

        BodyPart.entries.forEachIndexed { index, bodyPart ->
            val kp = raw[0][0][index]
            // MoveNet returns [y, x, score] normalized to the letterboxed square.
            // Remap [padX, 1-padX] → [0, 1] to get coordinates relative to the original image.
            keypoints[bodyPart] = Keypoint(
                x = ((kp[1] - padX) / (1f - 2f * padX)).coerceIn(0f, 1f),
                y = ((kp[0] - padY) / (1f - 2f * padY)).coerceIn(0f, 1f),
                score = kp[2]
            )
        }
        return PoseResult(keypoints = keypoints)
    }

    private fun buildCapabilityReport(): String {
        return """
            |=== PoseDetector Capabilities ===
            |Performance tier : ${capabilities.performanceTier}
            |GPU delegate      : ${capabilities.hasGpu}
            |CPU threads       : ${capabilities.optimalThreadCount}
            |=================================
        """.trimMargin()
    }
}