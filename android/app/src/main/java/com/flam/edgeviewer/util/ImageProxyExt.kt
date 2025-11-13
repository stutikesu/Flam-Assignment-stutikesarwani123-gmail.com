package com.flam.edgeviewer.util

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.min

fun ImageProxy.toNv21(): ByteArray? {
    val yPlane = planes.getOrNull(0) ?: return null
    val uPlane = planes.getOrNull(1) ?: return null
    val vPlane = planes.getOrNull(2) ?: return null

    val ySize = width * height
    val byteArray = ByteArray(ySize + (ySize / 2))

    val yBuffer = yPlane.buffer.duplicate().apply { position(0) }
    var outputPos = 0
    val rowStride = yPlane.rowStride
    for (row in 0 until height) {
        val rowBytes = min(width, yBuffer.remaining())
        yBuffer.get(byteArray, outputPos, rowBytes)
        if (rowBytes < width) {
            byteArray.fill(0, outputPos + rowBytes, outputPos + width)
        }
        outputPos += width
        if (row < height - 1) {
            val skip = rowStride - rowBytes
            if (skip > 0 && yBuffer.remaining() >= skip) {
                yBuffer.position(yBuffer.position() + skip)
            }
        }
    }

    val uBuffer = uPlane.buffer.duplicate().apply { position(0) }
    val vBuffer = vPlane.buffer.duplicate().apply { position(0) }
    var outputOffset = ySize

    val chromaHeight = height / 2
    val chromaWidth = width / 2
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
            val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
            byteArray[outputOffset++] = vBuffer.getSafely(vIndex)
            byteArray[outputOffset++] = uBuffer.getSafely(uIndex)
        }
    }
    return byteArray
}

private fun ByteBuffer.getSafely(index: Int): Byte {
    return if (index < 0 || index >= limit()) 0 else get(index)
}
