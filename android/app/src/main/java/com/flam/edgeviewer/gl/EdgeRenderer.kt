package com.flam.edgeviewer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EdgeRenderer : GLSurfaceView.Renderer {

    private var programId = 0
    private var textureId = 0

    private val quadCoords = floatArrayOf(
        -1f, 1f,
        -1f, -1f,
        1f, 1f,
        1f, -1f
    )
    private val texCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(quadCoords.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadCoords)
            .apply { position(0) }

    private val texBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(texCoords.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
            .apply { position(0) }

    private val pendingFrame = AtomicReference<FrameData?>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        programId = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        textureId = createTexture()
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        pendingFrame.getAndSet(null)?.let { uploadFrame(it) }
        renderQuad()
    }

    private fun renderQuad() {
        GLES20.glUseProgram(programId)
        val positionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        val texHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
        val samplerHandle = GLES20.glGetUniformLocation(programId, "uTexture")

        vertexBuffer.position(0)
        texBuffer.position(0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(
            texHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texBuffer
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(samplerHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texHandle)
    }

    private fun uploadFrame(frame: FrameData) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            frame.width,
            frame.height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            frame.pixelBuffer.apply { position(0) }
        )
    }

    fun submitFrame(data: FrameData) {
        pendingFrame.set(data)
    }

    fun buildFrame(bytes: ByteArray, width: Int, height: Int): FrameData {
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.position(0)
        return FrameData(width, height, buffer)
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        return id
    }

    private fun buildProgram(vertex: String, fragment: String): Int {
        val vertexId = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
        val fragmentId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexId)
        GLES20.glAttachShader(program, fragmentId)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    data class FrameData(
        val width: Int,
        val height: Int,
        val pixelBuffer: ByteBuffer
    )

    companion object {
        private const val FLOAT_SIZE = 4

        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                vTexCoord = aTexCoord;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
