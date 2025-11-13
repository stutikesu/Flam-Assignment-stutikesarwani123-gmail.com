package com.flam.edgeviewer.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class EdgeGlSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = EdgeRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        preserveEGLContextOnPause = true
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun pushFrame(bytes: ByteArray, width: Int, height: Int) {
        val frameData = renderer.buildFrame(bytes, width, height)
        queueEvent {
            renderer.submitFrame(frameData)
        }
        requestRender()
    }
}
