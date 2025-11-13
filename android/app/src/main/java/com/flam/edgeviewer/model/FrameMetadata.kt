package com.flam.edgeviewer.model

data class FrameMetadata(
    val fps: Double,
    val width: Int,
    val height: Int,
    val mode: ProcessingMode
)

enum class ProcessingMode {
    EDGE,
    RAW
}
