package com.flam.edgeviewer.nativebridge

import com.flam.edgeviewer.model.ProcessingMode

object NativeBridge {

    init {
        System.loadLibrary("edge_native")
    }

    private var configuredSize: Pair<Int, Int>? = null

    fun ensureConfigured(width: Int, height: Int) {
        val (cachedW, cachedH) = configuredSize ?: (0 to 0)
        if (configuredSize == null || cachedW != width || cachedH != height) {
            nativeConfigure(width, height)
            configuredSize = width to height
        }
    }

    fun processFrame(frame: ByteArray, width: Int, height: Int, timestampNs: Long): ByteArray {
        return nativeProcessFrame(frame, width, height, timestampNs)
    }

    fun setMode(mode: ProcessingMode) {
        nativeSetMode(mode == ProcessingMode.EDGE)
    }

    fun release() {
        nativeRelease()
        configuredSize = null
    }

    private external fun nativeConfigure(width: Int, height: Int)
    private external fun nativeProcessFrame(
        frame: ByteArray,
        width: Int,
        height: Int,
        timestampNs: Long
    ): ByteArray

    private external fun nativeSetMode(edgeMode: Boolean)
    private external fun nativeRelease()
}
