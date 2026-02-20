package com.simple.posedetection

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.simple.posedetection.detector.PoseDetector
import com.simple.posedetection.detector.PoseResult
import com.simple.posedetection.ui.pose.PoseOverlay
import com.simple.posedetection.ui.theme.PoseDetectionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var detector: PoseDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        detector = PoseDetector(this)
        val testBitmap = BitmapFactory.decodeStream(assets.open("test_person.jpg"))

        setContent {
            var poseResult by remember { mutableStateOf<PoseResult?>(null) }
            var inferenceTimeMs by remember { mutableStateOf(0L) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.Default) {
                    poseResult = detector.detect(testBitmap)
                    inferenceTimeMs = detector.lastInferenceTimeMs
                }
            }

            PoseDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        PoseOverlay(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = testBitmap,
                            poseResult = poseResult,
                            inferenceTimeMs = inferenceTimeMs,
                            capabilities = detector.capabilities,
                        )

                        // Show a loading indicator until inference completes
                        if (poseResult == null) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.close()
    }
}