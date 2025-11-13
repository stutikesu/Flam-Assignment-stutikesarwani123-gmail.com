package com.flam.edgeviewer.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.flam.edgeviewer.gl.EdgeGlSurfaceView
import com.flam.edgeviewer.model.FrameMetadata
import com.flam.edgeviewer.model.ProcessingMode
import com.flam.edgeviewer.nativebridge.NativeBridge
import com.flam.edgeviewer.util.toNv21
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class CameraPipeline(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val glSurfaceView: EdgeGlSurfaceView,
    private val listener: FrameListener
) {

    interface FrameListener {
        fun onFrameMetadata(metadata: FrameMetadata)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var mode: ProcessingMode = ProcessingMode.EDGE
    private var lastTimestampNs: Long = -1L

    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                try {
                    handleFrame(proxy)
                } catch (t: Throwable) {
                    Log.e(TAG, "Frame processing failed", t)
                } finally {
                    proxy.close()
                }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun handleFrame(proxy: ImageProxy) {
        val nv21 = proxy.toNv21() ?: return
        NativeBridge.ensureConfigured(proxy.width, proxy.height)
        val rgba = NativeBridge.processFrame(
            nv21,
            proxy.width,
            proxy.height,
            proxy.imageInfo.timestamp
        )
        glSurfaceView.pushFrame(rgba, proxy.width, proxy.height)
        val fps = computeFps(proxy.imageInfo.timestamp)
        val metadata = FrameMetadata(
            fps = fps,
            width = proxy.width,
            height = proxy.height,
            mode = mode
        )
        mainExecutor.execute {
            listener.onFrameMetadata(metadata)
        }
    }

    private fun computeFps(currentTimestampNs: Long): Double {
        if (lastTimestampNs <= 0L) {
            lastTimestampNs = currentTimestampNs
            return 0.0
        }
        val delta = currentTimestampNs - lastTimestampNs
        lastTimestampNs = currentTimestampNs
        val fps = if (delta == 0L) 0.0 else 1_000_000_000.0 / delta
        return max(0.0, fps)
    }

    fun toggleMode(): ProcessingMode {
        mode = if (mode == ProcessingMode.EDGE) ProcessingMode.RAW else ProcessingMode.EDGE
        NativeBridge.setMode(mode)
        return mode
    }

    fun shutdown() {
        NativeBridge.release()
        executor.shutdown()
    }

    companion object {
        private const val TAG = "CameraPipeline"
    }
}
