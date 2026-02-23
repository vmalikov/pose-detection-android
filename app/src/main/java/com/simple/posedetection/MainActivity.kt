package com.simple.posedetection

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.simple.posedetection.ui.camera.CameraScreen
import com.simple.posedetection.ui.theme.PoseDetectionTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraPermissionGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            PoseDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (cameraPermissionGranted) {
                        CameraScreen(modifier = Modifier.padding(innerPadding).fillMaxSize())
                    } else {
                        Box(
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Camera permission is required")
                        }
                    }
                }
            }
        }
    }
}
