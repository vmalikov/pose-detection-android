package com.simple.posedetection.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class PoseDetector(context: Context) {

    private val interpreter: Interpreter

    // MoveNet Lightning expects exactly 192x192 input
    // Thunder expects 256x256
    private val MODEL_INPUT_SIZE = 192

    init {
        val modelFile = loadModelFile(context, "movenet-singlepose-lightning -4.tflite")
        val options = Interpreter.Options().apply {
            // Start with 2 threads on CPU - safe default for any device
            // Can add GPU delegate here later
            numThreads = 2
        }
        interpreter = Interpreter(modelFile, options)
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

        // Step 4: Run inference
        interpreter.run(inputBuffer, outputBuffer)

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

    fun close() {
        interpreter.close()
    }
}
