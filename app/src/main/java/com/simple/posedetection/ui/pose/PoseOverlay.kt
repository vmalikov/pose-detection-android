package com.simple.posedetection.ui.pose

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.simple.posedetection.detector.Keypoint
import com.simple.posedetection.detector.PoseResult
import com.simple.posedetection.detector.SkeletonConnections

@Composable
fun PoseOverlay(
    modifier: Modifier,
    bitmap: Bitmap,
    poseResult: PoseResult?,
    confidenceThreshold: Float = 0.3f
) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier.fillMaxSize(),
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Pose detection source",
            contentScale = ContentScale.Fit,
        )

        if (poseResult != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSkeleton(poseResult, bitmap.width, bitmap.height, confidenceThreshold)
            }
        }
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
