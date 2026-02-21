package com.simple.posedetection.ui.pose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.posedetection.data.device.DeviceCapabilities
import com.simple.posedetection.domain.model.Keypoint
import com.simple.posedetection.domain.model.PoseResult
import com.simple.posedetection.domain.model.SkeletonConnections

@Composable
fun PoseOverlay(
    modifier: Modifier,
    poseResult: PoseResult?,
    inferenceTimeMs: Long = 0L,
    capabilities: DeviceCapabilities? = null,
    confidenceThreshold: Float = 0.3f
) {
    Box(modifier = modifier) {
        if (poseResult != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSkeleton(poseResult, size.width.toInt(), size.height.toInt(), confidenceThreshold)
            }
        }

        // Performance HUD — top-left corner
        if (inferenceTimeMs > 0 && capabilities != null) {
            PerformanceHud(
                inferenceTimeMs = inferenceTimeMs,
                capabilities = capabilities,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PerformanceHud(
    inferenceTimeMs: Long,
    capabilities: DeviceCapabilities,
    modifier: Modifier = Modifier
) {
    val backend = if (capabilities.hasGpu) "GPU" else "CPU ×${capabilities.optimalThreadCount}"
    val fps = if (inferenceTimeMs > 0) 1000f / inferenceTimeMs else 0f
    val hudColor = when {
        inferenceTimeMs < 33  -> Color(0xFF00E676)   // green  — 30fps capable
        inferenceTimeMs < 66  -> Color(0xFFFFD740)   // amber  — 15fps range
        else                  -> Color(0xFFFF5252)   // red    — too slow for live use
    }

    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$inferenceTimeMs ms  |  ${"%.1f".format(fps)} fps  |  $backend",
            color = hudColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun DrawScope.drawSkeleton(
    poseResult: PoseResult,
    imageWidth: Int,
    imageHeight: Int,
    confidenceThreshold: Float
) {
    val lineColor = Color(0xFF00E5FF)
    val pointColor = Color(0xFFFF4081)

    // ContentScale.Fit centers the image and preserves aspect ratio — it does NOT fill the
    // canvas. Calculate the actual image rect so keypoints land on the image, not the bars.
    val imageAspect = imageWidth.toFloat() / imageHeight
    val canvasAspect = size.width / size.height

    val drawWidth: Float
    val drawHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (imageAspect > canvasAspect) {
        // Image is wider than the canvas → fits width, bars on top/bottom
        drawWidth = size.width
        drawHeight = size.width / imageAspect
        offsetX = 0f
        offsetY = (size.height - drawHeight) / 2f
    } else {
        // Image is taller than the canvas → fits height, bars on left/right
        drawHeight = size.height
        drawWidth = size.height * imageAspect
        offsetX = (size.width - drawWidth) / 2f
        offsetY = 0f
    }

    fun Keypoint.toOffset() = Offset(offsetX + x * drawWidth, offsetY + y * drawHeight)

    // Draw lines first so keypoint dots render on top
    SkeletonConnections.connections.forEach { (startPart, endPart) ->
        val start = poseResult.keypoints[startPart]
        val end = poseResult.keypoints[endPart]

        if (start != null && end != null &&
            start.score > confidenceThreshold && end.score > confidenceThreshold
        ) {
            drawLine(
                color = lineColor,
                start = start.toOffset(),
                end = end.toOffset(),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // Draw keypoint circles on top of lines
    poseResult.keypoints.values.forEach { keypoint ->
        if (keypoint.score > confidenceThreshold) {
            drawCircle(
                color = lineColor.copy(alpha = 0.4f),
                radius = 8.dp.toPx(),
                center = keypoint.toOffset()
            )
            drawCircle(
                color = pointColor,
                radius = 5.dp.toPx(),
                center = keypoint.toOffset()
            )
        }
    }
}
