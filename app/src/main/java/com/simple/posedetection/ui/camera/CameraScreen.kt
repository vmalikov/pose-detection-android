package com.simple.posedetection.ui.camera

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simple.posedetection.ui.pose.ExerciseHud
import com.simple.posedetection.ui.pose.PoseOverlay
import com.simple.posedetection.domain.model.BodyPart
import com.simple.posedetection.ui.pose.invalidResultsToHighlightParts

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: PoseViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    val previewView = remember {
        PreviewView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lifecycleOwner) {
        viewModel.startCamera(lifecycleOwner, previewView.surfaceProvider)
    }

    Box(modifier = modifier.fillMaxSize()) {

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        when (val state = uiState) {
            is PoseUiState.Initializing -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            is PoseUiState.Active -> {
                val highlightParts = state.exerciseResult?.let { r ->
                    invalidResultsToHighlightParts(r.validationResults)
                } ?: emptySet<BodyPart>()
                PoseOverlay(
                    modifier = Modifier.fillMaxSize(),
                    poseResult = state.pose,
                    frameWidth = state.frameWidth,
                    frameHeight = state.frameHeight,
                    inferenceTimeMs = state.inferenceTimeMs,
                    capabilities = state.capabilities,
                    highlightParts = highlightParts,
                )
                state.exerciseResult?.let { exerciseResult ->
                    ExerciseHud(
                        exerciseResult = exerciseResult,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            is PoseUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
