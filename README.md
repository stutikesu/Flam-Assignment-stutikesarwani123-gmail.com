# EdgeVision

A minimal R&D playground that combines a native Android camera pipeline (CameraX + OpenCV + OpenGL ES via JNI) with a TypeScript-based web viewer for quick inspection of processed frames.

## Features

### Android
- CameraX preview stream using `PreviewView` plus `ImageAnalysis` for zero-copy buffers.
- JNI bridge that forwards NV21 frames into C++ where OpenCV performs grayscale + Canny edge detection.
- OpenGL ES 2.0 renderer that uploads processed RGBA textures to a dedicated `GLSurfaceView` for low-latency playback.
- Mode toggle (raw <> edge) wired through JNI so the native processor can short-circuit processing when needed.
- Real-time HUD that surfaces FPS + resolution stats calculated in the analyzer thread.

### Web
- Stand-alone TypeScript viewer that accepts `data:image/...` payloads or dropped PNGs exported from the Android run.
- Simulated FPS/resolution metadata to mirror what the native layer reports.
- Drag-and-drop zone plus textarea to validate transport payloads before wiring a real socket/HTTP bridge.
- Modern glassmorphic UI tuned for dark environments, easy to brand-match with the Android overlay.

## Screenshots / GIFs

- Android (Edge mode): `docs/screenshots/android-edge.png`
- Android (Raw mode): `docs/screenshots/android-raw.png`
- Web viewer: `docs/screenshots/web-viewer.png`

## Project Structure

```
.
|-- android/                # Android Studio project (app + JNI)
|   \-- app/src/main/cpp    # CMake + OpenCV native sources
|-- web/                    # TypeScript viewer (tsc + vanilla DOM)
\-- docs/screenshots        # Drop screenshots/GIFs for the README
```

## Setup Instructions

### Android
1. **Prerequisites**
   - Android Studio Iguana or newer
   - Android SDK 34, build-tools 34.0.0
   - NDK r26.1 (`ndkVersion` is pinned inside `app/build.gradle`)
   - OpenCV Android SDK (4.8+). Download from [opencv.org/releases](https://opencv.org/releases/) and unzip, e.g. to `C:/SDKs/OpenCV-4.9.0-android-sdk`.
2. **Wire OpenCV into CMake**
   - Set an environment variable before syncing Gradle: `OPENCV_ANDROID_SDK=C:\SDKs\OpenCV-4.9.0-android-sdk`
   - or add `opencv.sdk.dir=C:\SDKs\OpenCV-4.9.0-android-sdk` to `android/local.properties` and tweak `CMakeLists.txt` to read it (see comments inside the file).
3. **Sync & Build**
   - `cd android`
   - Open the folder in Android Studio and run Sync Project with Gradle Files.
   - Attach a device (or run an API 33+ emulator with camera passthrough) and hit Run.
4. **Controls**
   - The floating button toggles Raw <> Edge mode.
   - Stats panel updates FPS/resolution in real time.

### Web Viewer
1. `cd web`
2. `npm install`
3. `npm run build` (writes compiled assets to `web/dist`)
4. Open `dist/index.html` in a browser, paste a `data:image/...` payload from the Android logger, or drop a PNG to visualize it.

## Architecture Overview

1. CameraX acquisition - `ImageAnalysis` streams YUV frames without extra copies.
2. JNI bridge - `NativeBridge` copies NV21 planes into C++ and keeps track of processing mode.
3. OpenCV processing - `EdgeProcessor` converts NV21 to RGBA, runs Canny (or passes raw RGBA back) and returns a packed byte array.
4. OpenGL renderer - `EdgeGlSurfaceView` uploads the RGBA buffer into a GL texture per frame and composites it over the live preview.
5. Telemetry - Analyzer thread computes instantaneous FPS/res stats and pushes them to the UI/main thread for display.
6. Web viewer - Lightweight TypeScript UI emulates the transport target that will eventually receive frames via WebSocket/HTTP for debugging or remote monitoring.

## Development Notes

- Gradle wrapper plus `local.properties` are intentionally light right now. After wiring OpenCV locally, commit the updated wrapper plus configuration to keep builds reproducible.
- The JNI module expects OpenCV to be discoverable via the `OpenCV_DIR` CMake hint. If your SDK lives elsewhere, set the `OpenCV_DIR` CMake cache entry accordingly.
- The web viewer ships with a generated SVG frame so it is immediately interactive even before connecting it to a live pipeline.

## Next Steps

1. Capture real device footage and replace the sample PNG/GIFs in `docs/screenshots/` with actual recordings.
2. Add a WebSocket bridge (mock or real) that forwards processed frames and metadata to the TypeScript viewer automatically.
3. Profile the native layer to reuse RGBA buffers instead of reallocating per frame, lowering GC pressure in the analyzer thread.
4. Experiment with additional OpenGL shaders (grayscale, invert, LUT) and expose toggles in both Android plus web layers.
