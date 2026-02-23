# Pose Detection

A real-time human pose estimation Android app that runs **MoveNet SinglePose Lightning** on device using TensorFlow Lite (LiteRT), with optional GPU acceleration. It includes an **exercise engine** for rep counting and form validation (e.g. side-view squat). Built as a clean-architecture sample with Jetpack Compose, CameraX, and Hilt.

---

## Features

- **Live camera pose estimation** — Back camera feed with skeleton overlay (17 COCO keypoints).
- **Exercise mode (side-view squat)** — Rep counter and phase detection (START → DESCENDING → BOTTOM → ASCENDING → START) with configurable depth and standing thresholds.
- **Form validation** — Rule-based validators: depth at bottom, torso angle (upright), knee-over-toe. Optional red highlight of incorrect joints on the skeleton.
- **Fitness vs rehab configs** — Loose thresholds (fitness) or strict (rehab) for depth, torso tolerance, and knee-over-toe; all configurable per exercise.
- **GPU when available** — Uses LiteRT GPU delegate on supported devices; falls back to multi-threaded CPU.
- **Device-aware tuning** — Probes GPU support and picks CPU thread count (1–4) to avoid UI jank.
- **Performance HUD** — On-screen inference time (ms), FPS, and backend (GPU / CPU ×N). Exercise HUD shows reps, current phase, and validation messages.
- **Letterboxed inference** — Frames are letterboxed to square before 192×192 resize to avoid distortion.
- **Correct overlay mapping** — Keypoints are remapped for `FILL_CENTER` preview (crop) so the skeleton aligns with the visible frame.

---

## Architecture

The app follows a **domain-centric** structure:

| Layer   | Role |
|--------|------|
| **UI**   | Compose screens (`CameraScreen`, `PoseOverlay`, `ExerciseHud`), `PoseViewModel`, `PoseUiState`. |
| **Domain** | Ports (`PoseDetector`, `FrameSource`), models (`PoseResult`, `PoseFeatures`, `ExerciseDefinition`, etc.), `FramePipeline`, **PoseProcessor**, **ExerciseStateMachine**, **Validators**. |
| **Data**  | `PoseDetectorImpl` (LiteRT/TFLite + MoveNet), `CameraFrameSource` (CameraX → `Flow<VideoFrame>`), `DeviceCapabilityDetector`. |

**Data flow:** Camera → `FrameSource` → `Flow<VideoFrame>` → `conflate()` → inference on `Dispatchers.Default` → **PoseProcessor** (normalize, extract angles) → **ExerciseStateMachine** (phase + rep count) → **Validators** → `Flow<ProcessedPoseFrameResult>` → UI. All post-inference work runs on the same background dispatcher; inference is unchanged.

- **Frame pipeline:** After `PoseDetector.detect()`, the pipeline optionally runs the exercise layer (processor → state machine → validators) and emits `ProcessedPoseFrameResult` (pose + optional `ExerciseFrameResult` with phase, rep count, validation results).
- **DI:** Hilt; `PoseDetector`, `PoseProcessor`, `ExerciseDefinition` (squat with fitness config + validators), and `CameraFrameSource.Factory` provided in `AppModule`.

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3 |
| Camera | CameraX (Preview + ImageAnalysis, YUV_420_888, 480×640) |
| ML | LiteRT 1.4 (TFLite-compatible API), LiteRT GPU delegate; MoveNet SinglePose Lightning (192×192) |
| DI | Hilt |
| Concurrency | Kotlin Coroutines, Flow |
| Min SDK | 24 · Target/Compile SDK 36 |

---

## Requirements

- **Android Studio** (recommended; or CLI with Android SDK).
- **JDK 11** (or as required by the project).
- **Android SDK** with platform 36 and build-tools.
- **Device or emulator** with camera (API 24+). For GPU path, use a device/emulator with OpenCL/Vulkan support.

---

## Getting Started

### 1. Clone and open

```bash
git clone <repository-url>
cd PoseDetection
```

Open the project in Android Studio (**File → Open** → select the project root).

### 2. Model file

The app expects the MoveNet model in assets:

- **Path:** `app/src/main/assets/movenet-singlepose-lightning-4.tflite`

If the file is missing, download the TFLite model (e.g. from [TensorFlow Hub](https://tfhub.dev/google/movenet/singlepose/lightning/4)) and place it at the path above. The project already includes this file in the assets folder.

### 3. Build and run

- **Android Studio:** Use **Run** (green triangle) with a connected device or emulator.
- **CLI:**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant **Camera** permission when prompted. The back camera will show the live feed with the pose overlay, performance HUD, and exercise HUD (reps, phase, form messages). For squat detection, stand **sideways** to the camera so one side of the body is visible.

---

## Project Structure

```
PoseDetection/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── movenet-singlepose-lightning-4.tflite   # MoveNet Lightning model
│   │   ├── java/com/simple/posedetection/
│   │   │   ├── MainActivity.kt                         # Entry: permission, Compose setContent
│   │   │   ├── PoseDetectionApp.kt                     # @HiltAndroidApp Application
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                        # PoseDetector, PoseProcessor, ExerciseDefinition
│   │   │   ├── domain/
│   │   │   │   ├── exercise/                           # Phase, Validator, ValidationResult, ExerciseConfig,
│   │   │   │   │   │                                   # ExerciseStateMachine, squat/SquatDefinition,
│   │   │   │   │   │                                   # validator/DepthValidator, BackAngleValidator, KneeForwardValidator
│   │   │   │   ├── model/                              # PoseResult, PoseFeatures, VideoFrame, ExerciseFrameResult,
│   │   │   │   │   │                                   # ProcessedPoseFrameResult, DeviceCapabilities, etc.
│   │   │   │   ├── pipeline/
│   │   │   │   │   └── FramePipeline.kt               # inference + optional exercise layer → ProcessedPoseFrameResult
│   │   │   │   ├── port/                               # PoseDetector, FrameSource
│   │   │   │   ├── processor/                          # PoseProcessor, PoseProcessorConfig
│   │   │   │   └── util/
│   │   │   │       └── AngleUtils.kt                   # angleDegrees (three-point angle)
│   │   │   ├── data/
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraFrameSource.kt            # CameraX → Flow<VideoFrame>
│   │   │   │   ├── detector/
│   │   │   │   │   └── PoseDetectorImpl.kt             # LiteRT interpreter, letterbox, 17 keypoints
│   │   │   │   └── device/
│   │   │   │       └── DeviceCapabilityDetector.kt    # GPU probe, thread count, PerformanceTier
│   │   │   └── ui/
│   │   │       ├── camera/                             # CameraScreen, PoseViewModel, PoseUiState
│   │   │       ├── pose/                               # PoseOverlay, ExerciseHud, SkeletonConnections
│   │   │       └── theme/
│   │   ├── res/                                        # strings, themes, icons
│   │   └── AndroidManifest.xml
│   ├── src/androidTest/...                             # Instrumented tests
│   ├── src/test/...                                    # Unit tests (AngleUtils, PoseFeatures, ExerciseStateMachine,
│   │   │                                               # validators, PoseProcessor)
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                              # Dependency versions and catalog
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Key Components

- **`FramePipeline`** — Subscribes to `FrameSource.frames`, runs `PoseDetector.detect()` on a background dispatcher, then optionally runs `PoseProcessor` → `ExerciseStateMachine` → validators, and emits `ProcessedPoseFrameResult` (pose + optional exercise phase, rep count, validation results). Uses `conflate()` to keep only the latest frame when processing is slower than camera rate.
- **`PoseDetectorImpl`** — Loads `movenet-singlepose-lightning-4.tflite`, configures GPU delegate (if available) or CPU threads, letterboxes input to square, scales to 192×192, runs inference, returns `PoseResult` with normalized keypoints.
- **`PoseProcessor`** — Normalizes keypoints (origin at hip midpoint, scale by torso length), filters by confidence, and computes `PoseFeatures`: knee/hip/ankle/torso angles, knee horizontal displacement. Returns `null` when critical keypoints are below threshold.
- **`ExerciseStateMachine`** — Deterministic phase machine for squat: START ↔ DESCENDING ↔ BOTTOM ↔ ASCENDING; uses knee angles (max for standing, avg for depth) and debounced transitions. Counts reps only on full cycle (optionally requiring depth at bottom).
- **Validators** — `DepthValidator`, `BackAngleValidator`, `KneeForwardValidator`; rule-based, return `ValidationResult` (isValid, message, severity, optional `ValidationCode`) for UI and joint highlighting.
- **`ExerciseHud`** — Overlay showing rep count, current phase, and validation messages; `invalidResultsToHighlightParts()` maps validation codes to body parts for red highlight on `PoseOverlay`.
- **`CameraFrameSource`** — CameraX `ImageAnalysis` (480×640, YUV) + `Preview`; converts frames to `VideoFrame` (bitmap + timestamp) and exposes `Flow<VideoFrame>`.
- **`DeviceCapabilityDetector`** — Checks LiteRT GPU delegate support and picks CPU thread count; classifies device as HIGH / MID / LOW.
- **`PoseOverlay`** — Draws skeleton (COCO connections) and keypoint circles; remaps normalized keypoints to canvas when preview uses `FILL_CENTER`; shows Performance HUD (ms, FPS, backend). Optionally highlights invalid joints in red from exercise validation.

---

## Testing

- **Unit tests** (JVM): `./gradlew :app:testDebugUnitTest` or **Run → Run 'Tests in PoseDetection'** for `app/src/test`.
- **Instrumented tests** (device/emulator): `./gradlew connectedAndroidTest` or run the `androidTest` run configuration.

Unit tests cover:

- **`AngleUtilsTest`** — Three-point angle (0°, 45°, 90°, 180°, degenerate).
- **`PoseFeaturesTest`** — `kneeAngleAvg()`, `kneeAngleMax()` with various left/right combinations.
- **`ExerciseStateMachineTest`** — Phase transitions, debounce (1 and 2 frames), rep count (full cycle, no bottom, `minDepthAtBottom` false), standing vs depth knee logic.
- **`DepthValidatorTest`**, **`BackAngleValidatorTest`**, **`KneeForwardValidatorTest`** — Valid/invalid by phase and thresholds, `ValidationCode` when invalid.
- **`PoseProcessorTest`** — Valid pose returns non-null `PoseFeatures` with angles; null when confidence too low or keypoints missing.

---

## Permissions

- **`android.permission.CAMERA`** — Required for live pose estimation. Requested at runtime in `MainActivity`; the UI shows “Camera permission is required” until granted.

---

## License

This project is provided as-is for evaluation and open-source use. If you use or adapt it, please comply with the licenses of its dependencies (Android, Kotlin, Jetpack, TensorFlow/LiteRT, etc.) and any terms that apply to the MoveNet model.




