package com.flam.edgeviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.flam.edgeviewer.camera.CameraPipeline
import com.flam.edgeviewer.databinding.ActivityMainBinding
import com.flam.edgeviewer.model.FrameMetadata
import com.flam.edgeviewer.model.ProcessingMode

class MainActivity : AppCompatActivity(), CameraPipeline.FrameListener {

    private lateinit var binding: ActivityMainBinding
    private var pipeline: CameraPipeline? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraPipeline()
            } else {
                binding.modeLabel.text = getString(R.string.permission_rationale)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toggleModeButton.setOnClickListener {
            val mode = pipeline?.toggleMode() ?: return@setOnClickListener
            renderMode(mode)
        }

        if (hasCameraPermission()) {
            startCameraPipeline()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.edgeSurface.onResume()
    }

    override fun onPause() {
        binding.edgeSurface.onPause()
        super.onPause()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraPipeline() {
        if (pipeline != null) return
        pipeline = CameraPipeline(
            context = this,
            lifecycleOwner = this,
            previewView = binding.previewView,
            glSurfaceView = binding.edgeSurface,
            listener = this
        ).also { it.start() }
    }

    override fun onFrameMetadata(metadata: FrameMetadata) {
        binding.fpsLabel.text = String.format("%.1f FPS", metadata.fps)
        binding.resolutionLabel.text = "${metadata.width} x ${metadata.height}"
        renderMode(metadata.mode)
    }

    private fun renderMode(mode: ProcessingMode) {
        val labelRes = if (mode == ProcessingMode.EDGE) {
            R.string.mode_edge
        } else {
            R.string.mode_raw
        }
        binding.modeLabel.text = getString(labelRes)
    }

    override fun onDestroy() {
        super.onDestroy()
        pipeline?.shutdown()
    }
}
