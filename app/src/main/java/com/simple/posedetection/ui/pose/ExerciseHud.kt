package com.simple.posedetection.ui.pose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.posedetection.domain.exercise.ValidationCode
import com.simple.posedetection.domain.model.BodyPart
import com.simple.posedetection.domain.model.ExerciseFrameResult

@Composable
fun ExerciseHud(
    exerciseResult: ExerciseFrameResult,
    modifier: Modifier = Modifier
) {
    val invalidMessages = exerciseResult.validationResults
        .filter { !it.isValid && it.message != null }
        .map { it.message!! }
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = "Reps: ${exerciseResult.repCount}",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "Phase: ${exerciseResult.phase.name}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
            invalidMessages.forEach { msg ->
                Text(
                    text = msg,
                    color = Color(0xFFFFAB40),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Maps invalid validation results to body parts to highlight (e.g. in red on skeleton).
 * Uses [ValidationResult.code] for stable mapping; falls back to message for legacy results without code.
 */
fun invalidResultsToHighlightParts(validationResults: List<com.simple.posedetection.domain.exercise.ValidationResult>): Set<BodyPart> {
    val parts = mutableSetOf<BodyPart>()
    validationResults.filter { !it.isValid }.forEach { r ->
        when (r.code) {
            ValidationCode.DEPTH_NOT_REACHED -> {
                parts.add(BodyPart.LEFT_KNEE)
                parts.add(BodyPart.RIGHT_KNEE)
            }
            ValidationCode.TORSO_DEVIATION -> {
                parts.add(BodyPart.LEFT_SHOULDER)
                parts.add(BodyPart.RIGHT_SHOULDER)
                parts.add(BodyPart.LEFT_HIP)
                parts.add(BodyPart.RIGHT_HIP)
            }
            ValidationCode.KNEE_OVER_TOE -> {
                parts.add(BodyPart.LEFT_KNEE)
                parts.add(BodyPart.RIGHT_KNEE)
            }
            null -> {}
        }
    }
    return parts
}
