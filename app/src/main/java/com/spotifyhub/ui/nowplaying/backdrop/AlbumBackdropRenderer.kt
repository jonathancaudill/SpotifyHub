package com.spotifyhub.ui.nowplaying.backdrop

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal data class BackdropTextureSlot(
    val bitmap: Bitmap,
    val aspectRatio: Float,
    val seed: BackdropSeed,
    val key: String,
)

internal data class BackdropTransitionState(
    val current: BackdropTextureSlot,
    val previous: BackdropTextureSlot?,
    val animate: Boolean,
)

class AlbumBackdropRenderer(
    private val appContext: Context,
) : GLSurfaceView.Renderer {
    private companion object {
        const val OFFSCREEN_SCALE = 0.50f
        // Kawase blur pass offsets — many passes with large offsets to match the
        // reference's BlurFilter(100, 10, 1, 15) which is an enormous blur.
        // Each pass is cheap (5 texture samples), so 16 passes is fine.
        val KAWASE_OFFSETS = floatArrayOf(
            0f, 1f, 2f, 3f, 5f, 7f, 10f, 14f,
            19f, 25f, 32f, 40f, 50f, 62f, 76f, 92f,
        )
    }

    private val quadVertices: FloatBuffer = ByteBuffer
        .allocateDirect(4 * 4 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    -1f, -1f, 0f, 1f,
                    1f, -1f, 1f, 1f,
                    -1f, 1f, 0f, 0f,
                    1f, 1f, 1f, 0f,
                ),
            )
            position(0)
        }

    private var sceneProgram = 0
    private var blurProgram = 0
    private var sourceTextureIds = IntArray(2)
    private var framebufferIds = IntArray(2)
    private var framebufferTextureIds = IntArray(2)
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var offscreenWidth = 1
    private var offscreenHeight = 1
    private var surfaceReady = false

    private var scenePositionHandle = 0
    private var sceneTexCoordHandle = 0
    private var sceneResolutionHandle = 0
    private var sceneTimeHandle = 0
    private var sceneTransitionHandle = 0
    private var sceneCurrentSeedHandle = 0
    private var scenePreviousSeedHandle = 0
    private var sceneCurrentAspectHandle = 0
    private var scenePreviousAspectHandle = 0
    private var sceneCurrentTextureHandle = 0
    private var scenePreviousTextureHandle = 0

    private var blurPositionHandle = 0
    private var blurTexCoordHandle = 0
    private var blurTextureHandle = 0
    private var blurTexelSizeHandle = 0
    private var blurApplyVignetteHandle = 0
    private var blurKawaseOffsetHandle = 0

    private var currentSeed = BackdropSeedFactory.from("idle")
    private var previousSeed = currentSeed
    private var currentAspectRatio = 1f
    private var previousAspectRatio = 1f
    private var transitionStartMs = 0L
    private var transitionDurationMs = 450L
    private var shouldAnimateTransition = false
    @Volatile
    private var blurEnabled = true
    private val startTimeMs = SystemClock.elapsedRealtime()
    private var pendingState: BackdropTransitionState? = null

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        sceneProgram = createProgram(
            vertexSource = appContext.assets.open("shaders/album_backdrop.vert").bufferedReader().use { it.readText() },
            fragmentSource = appContext.assets.open("shaders/album_backdrop_scene.frag").bufferedReader().use { it.readText() },
        )
        blurProgram = createProgram(
            vertexSource = appContext.assets.open("shaders/album_backdrop.vert").bufferedReader().use { it.readText() },
            fragmentSource = appContext.assets.open("shaders/album_backdrop_blur.frag").bufferedReader().use { it.readText() },
        )

        scenePositionHandle = GLES20.glGetAttribLocation(sceneProgram, "aPosition")
        sceneTexCoordHandle = GLES20.glGetAttribLocation(sceneProgram, "aTexCoord")
        sceneResolutionHandle = GLES20.glGetUniformLocation(sceneProgram, "uResolution")
        sceneTimeHandle = GLES20.glGetUniformLocation(sceneProgram, "uTime")
        sceneTransitionHandle = GLES20.glGetUniformLocation(sceneProgram, "uTransition")
        sceneCurrentSeedHandle = GLES20.glGetUniformLocation(sceneProgram, "uCurrentSeed")
        scenePreviousSeedHandle = GLES20.glGetUniformLocation(sceneProgram, "uPreviousSeed")
        sceneCurrentAspectHandle = GLES20.glGetUniformLocation(sceneProgram, "uCurrentAspect")
        scenePreviousAspectHandle = GLES20.glGetUniformLocation(sceneProgram, "uPreviousAspect")
        sceneCurrentTextureHandle = GLES20.glGetUniformLocation(sceneProgram, "uCurrentTexture")
        scenePreviousTextureHandle = GLES20.glGetUniformLocation(sceneProgram, "uPreviousTexture")

        blurPositionHandle = GLES20.glGetAttribLocation(blurProgram, "aPosition")
        blurTexCoordHandle = GLES20.glGetAttribLocation(blurProgram, "aTexCoord")
        blurTextureHandle = GLES20.glGetUniformLocation(blurProgram, "uTexture")
        blurTexelSizeHandle = GLES20.glGetUniformLocation(blurProgram, "uTexelSize")
        blurApplyVignetteHandle = GLES20.glGetUniformLocation(blurProgram, "uApplyVignette")
        blurKawaseOffsetHandle = GLES20.glGetUniformLocation(blurProgram, "uKawaseOffset")

        GLES20.glGenTextures(sourceTextureIds.size, sourceTextureIds, 0)
        sourceTextureIds.forEach(::configureTexture)

        surfaceReady = true
        pendingState?.let {
            applyTransitionState(it)
            pendingState = null
        }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        offscreenWidth = maxOf((width * OFFSCREEN_SCALE).toInt(), 1)
        offscreenHeight = maxOf((height * OFFSCREEN_SCALE).toInt(), 1)
        createOrResizeFramebuffers()
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        if (!surfaceReady) {
            return
        }

        val transition = if (shouldAnimateTransition) {
            ((SystemClock.elapsedRealtime() - transitionStartMs).toFloat() / transitionDurationMs.toFloat())
                .coerceIn(0f, 1f)
                .also { progress ->
                    if (progress >= 1f) {
                        shouldAnimateTransition = false
                    }
                }
        } else {
            1f
        }

        if (blurEnabled) {
            // Pass 1: render scene to FBO 0
            renderScene(
                outputFramebuffer = framebufferIds[0],
                width = offscreenWidth,
                height = offscreenHeight,
                transition = transition,
            )

            // Passes 2+: Kawase blur, ping-ponging between FBO 0 and FBO 1.
            // Each pass reads from one FBO and writes to the other.
            for (i in KAWASE_OFFSETS.indices) {
                val isLastPass = i == KAWASE_OFFSETS.lastIndex
                val inputTexture = framebufferTextureIds[i % 2]
                val outputFbo = if (isLastPass) 0 else framebufferIds[(i + 1) % 2]
                val outputWidth = if (isLastPass) viewportWidth else offscreenWidth
                val outputHeight = if (isLastPass) viewportHeight else offscreenHeight

                kawaseBlurPass(
                    inputTexture = inputTexture,
                    outputFramebuffer = outputFbo,
                    width = outputWidth,
                    height = outputHeight,
                    kawaseOffset = KAWASE_OFFSETS[i],
                    applyVignette = isLastPass,
                )
            }
        } else {
            renderScene(
                outputFramebuffer = 0,
                width = viewportWidth,
                height = viewportHeight,
                transition = transition,
            )
        }
    }

    internal fun setTransitionState(state: BackdropTransitionState) {
        pendingState = state
        if (!surfaceReady) {
            return
        }
        applyTransitionState(state)
    }

    internal fun setBlurEnabled(enabled: Boolean) {
        blurEnabled = enabled
    }

    private fun applyTransitionState(state: BackdropTransitionState) {
        uploadBitmap(sourceTextureIds[0], state.current.bitmap)
        currentAspectRatio = state.current.aspectRatio
        currentSeed = state.current.seed

        val previous = state.previous ?: state.current
        uploadBitmap(sourceTextureIds[1], previous.bitmap)
        previousAspectRatio = previous.aspectRatio
        previousSeed = previous.seed

        shouldAnimateTransition = state.animate && state.previous != null
        transitionStartMs = SystemClock.elapsedRealtime()
    }

    private fun renderScene(
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        transition: Float,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFramebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(sceneProgram)

        bindQuad(scenePositionHandle, sceneTexCoordHandle)

        GLES20.glUniform2f(sceneResolutionHandle, width.toFloat(), height.toFloat())
        GLES20.glUniform1f(sceneTimeHandle, (SystemClock.elapsedRealtime() - startTimeMs) / 1000f)
        GLES20.glUniform1f(sceneTransitionHandle, transition)
        GLES20.glUniform4fv(sceneCurrentSeedHandle, 1, currentSeed.toUniformArray(), 0)
        GLES20.glUniform4fv(scenePreviousSeedHandle, 1, previousSeed.toUniformArray(), 0)
        GLES20.glUniform1f(sceneCurrentAspectHandle, currentAspectRatio)
        GLES20.glUniform1f(scenePreviousAspectHandle, previousAspectRatio)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTextureIds[0])
        GLES20.glUniform1i(sceneCurrentTextureHandle, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTextureIds[1])
        GLES20.glUniform1i(scenePreviousTextureHandle, 1)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun kawaseBlurPass(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        kawaseOffset: Float,
        applyVignette: Boolean,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFramebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(blurProgram)

        bindQuad(blurPositionHandle, blurTexCoordHandle)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(blurTextureHandle, 0)
        GLES20.glUniform2f(blurTexelSizeHandle, 1f / offscreenWidth.toFloat(), 1f / offscreenHeight.toFloat())
        GLES20.glUniform1f(blurApplyVignetteHandle, if (applyVignette) 1f else 0f)
        GLES20.glUniform1f(blurKawaseOffsetHandle, kawaseOffset)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun bindQuad(positionHandle: Int, texCoordHandle: Int) {
        quadVertices.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, quadVertices)
        GLES20.glEnableVertexAttribArray(positionHandle)

        quadVertices.position(2)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, quadVertices)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
    }

    private fun createOrResizeFramebuffers() {
        if (framebufferIds.any { it != 0 }) {
            GLES20.glDeleteFramebuffers(framebufferIds.size, framebufferIds, 0)
            GLES20.glDeleteTextures(framebufferTextureIds.size, framebufferTextureIds, 0)
        }

        framebufferIds = IntArray(2)
        framebufferTextureIds = IntArray(2)
        GLES20.glGenFramebuffers(framebufferIds.size, framebufferIds, 0)
        GLES20.glGenTextures(framebufferTextureIds.size, framebufferTextureIds, 0)

        framebufferTextureIds.forEachIndexed { index, textureId ->
            configureTexture(textureId)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                offscreenWidth,
                offscreenHeight,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null,
            )

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferIds[index])
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                textureId,
                0,
            )
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private fun configureTexture(textureId: Int) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun uploadBitmap(textureId: Int, bitmap: Bitmap) {
        configureTexture(textureId)
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also { createdProgram ->
            GLES20.glAttachShader(createdProgram, vertexShader)
            GLES20.glAttachShader(createdProgram, fragmentShader)
            GLES20.glLinkProgram(createdProgram)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(createdProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(createdProgram)
                GLES20.glDeleteProgram(createdProgram)
                error("Unable to link GL program: $log")
            }
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Unable to compile GL shader: $log")
        }
        return shader
    }
}
