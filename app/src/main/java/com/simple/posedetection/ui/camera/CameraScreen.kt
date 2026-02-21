package com.simple.posedetection.ui.camera

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simple.posedetection.ui.pose.PoseOverlay

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: PoseViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {

        // PreviewView is created inside factory so it gets proper layout params.
        // Without MATCH_PARENT the view may measure to 0×0.
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    viewModel.startCamera(lifecycleOwner, surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        when (val state = uiState) {
            is PoseUiState.Initializing -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            is PoseUiState.Active -> {
                viewModel.capabilities?.let { caps ->
                    PoseOverlay(
                        modifier = Modifier.fillMaxSize(),
                        poseResult = state.result.pose,
                        inferenceTimeMs = state.result.inferenceTimeMs,
                        capabilities = caps,
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
