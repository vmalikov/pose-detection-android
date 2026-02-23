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
import com.simple.posedetection.domain.model.DeviceCapabilities
import com.simple.posedetection.domain.model.Keypoint
import com.simple.posedetection.domain.model.PoseResult

@Composable
fun PoseOverlay(
    modifier: Modifier,
    poseResult: PoseResult?,
    // Display-space frame dimensions (after rotation applied).
    // Required to correctly remap keypoints when PreviewView uses FILL_CENTER (crop).
    frameWidth: Int = 0,
    frameHeight: Int = 0,
    inferenceTimeMs: Long = 0L,
    capabilities: DeviceCapabilities? = null,
    confidenceThreshold: Float = 0.3f
) {
    Box(modifier = modifier) {
        if (poseResult != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSkeleton(poseResult, frameWidth, frameHeight, confidenceThreshold)
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

    // PreviewView uses FILL_CENTER (ContentScale.Crop): the frame is scaled to fill the canvas
    // and the excess dimension is cropped symmetrically from both sides.
    //
    // Keypoints are in full-frame normalized [0,1] space. We need to remap them to the
    // visible cropped portion that actually appears on screen.
    //
    //   frameAspect > canvasAspect → frame is "wider" → scale by canvas height, crop L/R sides
    //   frameAspect < canvasAspect → frame is "taller" → scale by canvas width, crop T/B sides
    fun Keypoint.toOffset(): Offset {
        if (imageWidth <= 0 || imageHeight <= 0) {
            // No frame dimensions available — fall back to 1:1 mapping
            return Offset(x * size.width, y * size.height)
        }
        val frameAspect = imageWidth.toFloat() / imageHeight
        val canvasAspect = size.width / size.height
        return if (frameAspect > canvasAspect) {
            // Crop left/right: visible x fraction of the full frame width
            val visibleW = canvasAspect / frameAspect
            val x0 = (1f - visibleW) / 2f
            Offset(
                x = ((x - x0) / visibleW * size.width).coerceIn(0f, size.width),
                y = y * size.height
            )
        } else {
            // Crop top/bottom: visible y fraction of the full frame height
            val visibleH = frameAspect / canvasAspect
            val y0 = (1f - visibleH) / 2f
            Offset(
                x = x * size.width,
                y = ((y - y0) / visibleH * size.height).coerceIn(0f, size.height)
            )
        }
    }

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
            val center = keypoint.toOffset()
            drawCircle(color = lineColor.copy(alpha = 0.4f), radius = 8.dp.toPx(), center = center)
            drawCircle(color = pointColor, radius = 5.dp.toPx(), center = center)
        }
    }
}
