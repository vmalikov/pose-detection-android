# Pose Detection

A real-time human pose estimation Android app that runs **MoveNet SinglePose Lightning** on device using TensorFlow Lite (LiteRT), with optional GPU acceleration. Built as a clean-architecture sample with Jetpack Compose, CameraX, and Hilt.

---

## Features

- **Live camera pose estimation** — Back camera feed with skeleton overlay (17 COCO keypoints).
- **GPU when available** — Uses LiteRT GPU delegate on supported devices; falls back to multi-threaded CPU.
- **Device-aware tuning** — Probes GPU support and picks CPU thread count (1–4) to avoid UI jank.
- **Performance HUD** — On-screen inference time (ms), FPS, and backend (GPU / CPU ×N).
- **Letterboxed inference** — Frames are letterboxed to square before 192×192 resize to avoid distortion.
- **Correct overlay mapping** — Keypoints are remapped for `FILL_CENTER` preview (crop) so the skeleton aligns with the visible frame.

---

## Architecture

The app follows a **domain-centric** structure:

| Layer   | Role |
|--------|------|
| **UI**   | Compose screens (`CameraScreen`, `PoseOverlay`), `PoseViewModel`, `PoseUiState`. |
| **Domain** | Ports (`PoseDetector`, `FrameSource`), models (`PoseResult`, `Keypoint`, `BodyPart`, `VideoFrame`, `DeviceCapabilities`), and `FramePipeline` that connects frame source → inference → results. |
| **Data**  | `PoseDetectorImpl` (LiteRT/TFLite + MoveNet), `CameraFrameSource` (CameraX → `Flow<VideoFrame>`), `DeviceCapabilityDetector`. |

- **Frame pipeline:** `FrameSource` (camera) → `Flow<VideoFrame>` → `conflate()` → inference on `Dispatchers.Default` → `Flow<PoseFrameResult>` → UI. Lazy `PoseDetector` init keeps GPU load off the main thread.
- **DI:** Hilt; `PoseDetector` and `CameraFrameSource.Factory` provided in `AppModule` / constructor.

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

Grant **Camera** permission when prompted. The back camera will show the live feed with the pose overlay and performance HUD.

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
│   │   │   │   └── AppModule.kt                        # Provides PoseDetector (and deps)
│   │   │   ├── domain/
│   │   │   │   ├── model/                              # VideoFrame, PoseResult, Keypoint, BodyPart, etc.
│   │   │   │   ├── port/                               # PoseDetector, FrameSource
│   │   │   │   └── pipeline/
│   │   │   │       └── FramePipeline.kt               # frameSource → inference → Flow<PoseFrameResult>
│   │   │   ├── data/
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraFrameSource.kt            # CameraX → Flow<VideoFrame>
│   │   │   │   ├── detector/
│   │   │   │   │   └── PoseDetectorImpl.kt             # LiteRT interpreter, letterbox, 17 keypoints
│   │   │   │   └── device/
│   │   │   │       └── DeviceCapabilityDetector.kt    # GPU probe, thread count, PerformanceTier
│   │   │   └── ui/
│   │   │       ├── camera/                             # CameraScreen, PoseViewModel, PoseUiState
│   │   │       ├── pose/                               # PoseOverlay, SkeletonConnections
│   │   │       └── theme/
│   │   ├── res/                                        # strings, themes, icons
│   │   └── AndroidManifest.xml
│   ├── src/androidTest/...                             # Instrumented tests
│   ├── src/test/...                                    # Unit tests
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                              # Dependency versions and catalog
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Key Components

- **`FramePipeline`** — Subscribes to `FrameSource.frames`, runs `PoseDetector.detect()` on a background dispatcher, emits `PoseFrameResult` with timing. Uses `conflate()` to keep only the latest frame when inference is slower than camera rate.
- **`PoseDetectorImpl`** — Loads `movenet-singlepose-lightning-4.tflite`, configures GPU delegate (if available) or CPU threads, letterboxes input to square, scales to 192×192, runs inference, returns `PoseResult` with normalized keypoints.
- **`CameraFrameSource`** — CameraX `ImageAnalysis` (480×640, YUV) + `Preview`; converts frames to `VideoFrame` (bitmap + timestamp) and exposes `Flow<VideoFrame>`.
- **`DeviceCapabilityDetector`** — Checks LiteRT GPU delegate support and picks CPU thread count; classifies device as HIGH / MID / LOW.
- **`PoseOverlay`** — Draws skeleton (COCO connections) and keypoint circles; remaps normalized keypoints to canvas when preview uses `FILL_CENTER`; shows Performance HUD (ms, FPS, backend).

---

## Testing

- **Unit tests** (JVM): `./gradlew test` or **Run → Run 'Tests in PoseDetection'** for `app/src/test`.
- **Instrumented tests** (device/emulator): `./gradlew connectedAndroidTest` or run the `androidTest` run configuration.

The default templates include:

- **Unit:** `ExampleUnitTest` — simple assertion (e.g. `addition_isCorrect`).
- **Instrumented:** `ExampleInstrumentedTest` — checks app context package name.

For an assessment, you can extend these with:

- Unit tests for domain logic (e.g. coordinate remapping, `DeviceCapabilityDetector` behavior, or pipeline flow behavior with a fake `FrameSource`/`PoseDetector`).
- Instrumented tests for camera permission flow, or a smoke test that launches the main screen and collects at least one pose result (if feasible with test doubles or a short timeout).

---

## Permissions

- **`android.permission.CAMERA`** — Required for live pose estimation. Requested at runtime in `MainActivity`; the UI shows “Camera permission is required” until granted.

---

## License

This project is provided as-is for evaluation and open-source use. If you use or adapt it, please comply with the licenses of its dependencies (Android, Kotlin, Jetpack, TensorFlow/LiteRT, etc.) and any terms that apply to the MoveNet model.




