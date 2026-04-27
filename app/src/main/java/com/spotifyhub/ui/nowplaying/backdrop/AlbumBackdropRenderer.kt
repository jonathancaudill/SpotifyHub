package com.spotifyhub.ui.nowplaying.backdrop

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal data class BackdropTextureSlot(
    val bitmap: Bitmap,
    val aspectRatio: Float,
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
        const val OFFSCREEN_SCALE = 1.0f
        const val SATURATION = 2.25f
        const val TWIST_ANGLE = -1.85f
        const val TWIST_CROP = 1.22f
        const val SCENE_ZOOM = 1.22f
        const val TRANSITION_DURATION_MS = 450L
        const val TAU = (Math.PI * 2.0).toFloat()

        val SPRITE_SIZE_FACTORS = floatArrayOf(1.25f, 0.80f, 0.50f, 0.25f)
        val SPRITE_ROTATION_SPEEDS = floatArrayOf(0.05f, -0.12f, -0.09f, 0.06f)

        val KAWASE_OFFSETS = floatArrayOf(6f, 10f, 16f, 24f, 34f, 46f, 60f, 78f)
    }

    private val fullscreenQuad: FloatBuffer = ByteBuffer
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

    private val spriteQuad: FloatBuffer = ByteBuffer
        .allocateDirect(4 * 4 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var spriteProgram = 0
    private var twistProgram = 0
    private var blurProgram = 0
    private var presentProgram = 0

    private var sourceTextureIds = IntArray(2)
    private var framebufferIds = IntArray(3)
    private var framebufferTextureIds = IntArray(3)

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var offscreenWidth = 1
    private var offscreenHeight = 1
    private var surfaceReady = false

    private var spritePositionHandle = 0
    private var spriteTexCoordHandle = 0
    private var spriteTextureHandle = 0
    private var spriteAspectHandle = 0

    private var twistPositionHandle = 0
    private var twistTexCoordHandle = 0
    private var twistPreviousTextureHandle = 0
    private var twistCurrentTextureHandle = 0
    private var twistResolutionHandle = 0
    private var twistTransitionHandle = 0
    private var twistAngleHandle = 0
    private var twistRadiusHandle = 0
    private var twistOffsetHandle = 0
    private var twistCropHandle = 0

    private var blurPositionHandle = 0
    private var blurTexCoordHandle = 0
    private var blurTextureHandle = 0
    private var blurTexelSizeHandle = 0
    private var blurApplyVignetteHandle = 0
    private var blurKawaseOffsetHandle = 0
    private var blurSaturationHandle = 0

    private var presentPositionHandle = 0
    private var presentTexCoordHandle = 0
    private var presentTextureHandle = 0
    private var presentSaturationHandle = 0

    private val sceneSeed = BackdropSeedFactory.from("album-backdrop-scene")
    private var currentAspectRatio = 1f
    private var previousAspectRatio = 1f
    private var transitionStartMs = 0L
    private var shouldAnimateTransition = false
    @Volatile
    private var blurPassCount = KAWASE_OFFSETS.size
    @Volatile
    private var startTimeMs = SystemClock.elapsedRealtime()

    private val timeWrapPeriod = (2.0 * Math.PI * 100.0).toFloat()
    private var pendingState: BackdropTransitionState? = null

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        spriteProgram = createProgram(
            vertexSource = appContext.assets.open("shaders/album_backdrop.vert").bufferedReader().use { it.readText() },
            fragmentSource = appContext.assets.open("shaders/album_backdrop_sprite.frag").bufferedReader().use { it.readText() },
        )
        twistProgram = createProgram(
            vertexSource = appContext.assets.open("shaders/album_backdrop.vert").bufferedReader().use { it.readText() },
            fragmentSource = appContext.assets.open("shaders/album_backdrop_twist.frag").bufferedReader().use { it.readText() },
        )
        blurProgram = createProgram(
            vertexSource = appContext.assets.open("shaders/album_backdrop.vert").bufferedReader().use { it.readText() },
            fragmentSource = appContext.assets.open("shaders/album_backdrop_blur.frag").bufferedReader().use { it.readText() },
        )
        presentProgram = createProgram(
            vertexSource = appContext.assets.open("shaders/album_backdrop.vert").bufferedReader().use { it.readText() },
            fragmentSource = appContext.assets.open("shaders/album_backdrop_present.frag").bufferedReader().use { it.readText() },
        )

        spritePositionHandle = GLES20.glGetAttribLocation(spriteProgram, "aPosition")
        spriteTexCoordHandle = GLES20.glGetAttribLocation(spriteProgram, "aTexCoord")
        spriteTextureHandle = GLES20.glGetUniformLocation(spriteProgram, "uTexture")
        spriteAspectHandle = GLES20.glGetUniformLocation(spriteProgram, "uTextureAspect")

        twistPositionHandle = GLES20.glGetAttribLocation(twistProgram, "aPosition")
        twistTexCoordHandle = GLES20.glGetAttribLocation(twistProgram, "aTexCoord")
        twistPreviousTextureHandle = GLES20.glGetUniformLocation(twistProgram, "uPreviousTexture")
        twistCurrentTextureHandle = GLES20.glGetUniformLocation(twistProgram, "uCurrentTexture")
        twistResolutionHandle = GLES20.glGetUniformLocation(twistProgram, "uResolution")
        twistTransitionHandle = GLES20.glGetUniformLocation(twistProgram, "uTransition")
        twistAngleHandle = GLES20.glGetUniformLocation(twistProgram, "uAngle")
        twistRadiusHandle = GLES20.glGetUniformLocation(twistProgram, "uRadius")
        twistOffsetHandle = GLES20.glGetUniformLocation(twistProgram, "uOffset")
        twistCropHandle = GLES20.glGetUniformLocation(twistProgram, "uCrop")

        blurPositionHandle = GLES20.glGetAttribLocation(blurProgram, "aPosition")
        blurTexCoordHandle = GLES20.glGetAttribLocation(blurProgram, "aTexCoord")
        blurTextureHandle = GLES20.glGetUniformLocation(blurProgram, "uTexture")
        blurTexelSizeHandle = GLES20.glGetUniformLocation(blurProgram, "uTexelSize")
        blurApplyVignetteHandle = GLES20.glGetUniformLocation(blurProgram, "uApplyVignette")
        blurKawaseOffsetHandle = GLES20.glGetUniformLocation(blurProgram, "uKawaseOffset")
        blurSaturationHandle = GLES20.glGetUniformLocation(blurProgram, "uSaturation")

        presentPositionHandle = GLES20.glGetAttribLocation(presentProgram, "aPosition")
        presentTexCoordHandle = GLES20.glGetAttribLocation(presentProgram, "aTexCoord")
        presentTextureHandle = GLES20.glGetUniformLocation(presentProgram, "uTexture")
        presentSaturationHandle = GLES20.glGetUniformLocation(presentProgram, "uSaturation")

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
            ((SystemClock.elapsedRealtime() - transitionStartMs).toFloat() / TRANSITION_DURATION_MS.toFloat())
                .coerceIn(0f, 1f)
                .also { progress ->
                    if (progress >= 1f) {
                        shouldAnimateTransition = false
                    }
                }
        } else {
            1f
        }

        val timeSeconds = ((SystemClock.elapsedRealtime() - startTimeMs) / 1000f) % timeWrapPeriod

        composeArtworkField(
            outputFramebuffer = framebufferIds[1],
            textureId = sourceTextureIds[0],
            aspectRatio = currentAspectRatio,
            seed = sceneSeed,
            width = offscreenWidth,
            height = offscreenHeight,
            timeSeconds = timeSeconds,
        )

        val needsPreviousCompose = transition < 0.999f
        if (needsPreviousCompose) {
            composeArtworkField(
                outputFramebuffer = framebufferIds[0],
                textureId = sourceTextureIds[1],
                aspectRatio = previousAspectRatio,
                seed = sceneSeed,
                width = offscreenWidth,
                height = offscreenHeight,
                timeSeconds = timeSeconds,
            )
        }

        twistAndMixPass(
            previousTexture = if (needsPreviousCompose) framebufferTextureIds[0] else framebufferTextureIds[1],
            currentTexture = framebufferTextureIds[1],
            outputFramebuffer = framebufferIds[2],
            width = offscreenWidth,
            height = offscreenHeight,
            transition = transition,
        )

        val passCount = blurPassCount.coerceIn(0, KAWASE_OFFSETS.size)
        var inputTexture = framebufferTextureIds[2]
        var outputIndex = 0

        for (i in 0 until passCount) {
            kawaseBlurPass(
                inputTexture = inputTexture,
                outputFramebuffer = framebufferIds[outputIndex],
                width = offscreenWidth,
                height = offscreenHeight,
                kawaseOffset = KAWASE_OFFSETS[i],
                applyVignette = false,
                saturation = 1f,
            )
            inputTexture = framebufferTextureIds[outputIndex]
            outputIndex = if (outputIndex == 0) 1 else 0
        }

        presentPass(
            inputTexture = inputTexture,
            saturation = SATURATION,
        )
    }

    internal fun setTransitionState(state: BackdropTransitionState) {
        pendingState = state
        if (!surfaceReady) {
            return
        }
        applyTransitionState(state)
    }

    internal fun setBlurPassCount(count: Int) {
        blurPassCount = count.coerceIn(0, KAWASE_OFFSETS.size)
    }

    internal val maxBlurPasses: Int get() = KAWASE_OFFSETS.size

    internal fun release() {
        if (!surfaceReady) {
            return
        }

        if (spriteProgram != 0) {
            GLES20.glDeleteProgram(spriteProgram)
            spriteProgram = 0
        }
        if (twistProgram != 0) {
            GLES20.glDeleteProgram(twistProgram)
            twistProgram = 0
        }
        if (blurProgram != 0) {
            GLES20.glDeleteProgram(blurProgram)
            blurProgram = 0
        }
        if (presentProgram != 0) {
            GLES20.glDeleteProgram(presentProgram)
            presentProgram = 0
        }
        if (sourceTextureIds.any { it != 0 }) {
            GLES20.glDeleteTextures(sourceTextureIds.size, sourceTextureIds, 0)
            sourceTextureIds = IntArray(2)
        }
        if (framebufferIds.any { it != 0 }) {
            GLES20.glDeleteFramebuffers(framebufferIds.size, framebufferIds, 0)
            framebufferIds = IntArray(3)
        }
        if (framebufferTextureIds.any { it != 0 }) {
            GLES20.glDeleteTextures(framebufferTextureIds.size, framebufferTextureIds, 0)
            framebufferTextureIds = IntArray(3)
        }

        surfaceReady = false
    }

    internal fun resetTime() {
        startTimeMs = SystemClock.elapsedRealtime()
    }

    private fun applyTransitionState(state: BackdropTransitionState) {
        uploadBitmap(sourceTextureIds[0], state.current.bitmap)
        currentAspectRatio = state.current.aspectRatio

        val previous = state.previous ?: state.current
        uploadBitmap(sourceTextureIds[1], previous.bitmap)
        previousAspectRatio = previous.aspectRatio

        shouldAnimateTransition = state.animate && state.previous != null
        transitionStartMs = SystemClock.elapsedRealtime()
    }

    private fun composeArtworkField(
        outputFramebuffer: Int,
        textureId: Int,
        aspectRatio: Float,
        seed: BackdropSeed,
        width: Int,
        height: Int,
        timeSeconds: Float,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFramebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(spriteProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(spriteTextureHandle, 0)
        GLES20.glUniform1f(spriteAspectHandle, aspectRatio)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        val centerX = widthF * 0.5f
        val centerY = heightF * 0.5f
        val orbitRadius = widthF * 0.25f
        val phase0 = seed.x * TAU
        val phase1 = seed.y * TAU
        val phase2 = seed.z * TAU
        val phase3 = seed.w * TAU

        val rot0 = phase0 + SPRITE_ROTATION_SPEEDS[0] * timeSeconds
        val rot1 = phase1 + SPRITE_ROTATION_SPEEDS[1] * timeSeconds
        val rot2 = phase2 + SPRITE_ROTATION_SPEEDS[2] * timeSeconds
        val rot3 = phase3 + SPRITE_ROTATION_SPEEDS[3] * timeSeconds

        drawSprite(width, height, centerX, centerY, widthF * SPRITE_SIZE_FACTORS[0] * SCENE_ZOOM, rot0)
        drawSprite(width, height, widthF / 2.5f, heightF / 2.5f, widthF * SPRITE_SIZE_FACTORS[1] * SCENE_ZOOM, rot1)
        drawSprite(
            width,
            height,
            centerX + orbitRadius * cos(rot2 * 0.75f),
            centerY + orbitRadius * sin(rot2 * 0.75f),
            widthF * SPRITE_SIZE_FACTORS[2] * SCENE_ZOOM,
            rot2,
        )
        drawSprite(
            width,
            height,
            centerX + widthF * 0.05f + orbitRadius * cos(rot3 * 0.75f),
            centerY + widthF * 0.05f + orbitRadius * sin(rot3 * 0.75f),
            widthF * SPRITE_SIZE_FACTORS[3] * SCENE_ZOOM,
            rot3,
        )
    }

    private fun twistAndMixPass(
        previousTexture: Int,
        currentTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        transition: Float,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFramebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(twistProgram)

        bindQuad(
            buffer = fullscreenQuad,
            positionHandle = twistPositionHandle,
            texCoordHandle = twistTexCoordHandle,
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previousTexture)
        GLES20.glUniform1i(twistPreviousTextureHandle, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentTexture)
        GLES20.glUniform1i(twistCurrentTextureHandle, 1)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        GLES20.glUniform2f(twistResolutionHandle, widthF, heightF)
        GLES20.glUniform1f(twistTransitionHandle, transition)
        GLES20.glUniform1f(twistAngleHandle, TWIST_ANGLE)
        GLES20.glUniform1f(twistRadiusHandle, maxOf(widthF, heightF) * 0.72f)
        GLES20.glUniform2f(twistOffsetHandle, widthF * 0.5f, heightF * 0.5f)
        GLES20.glUniform1f(twistCropHandle, TWIST_CROP)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun kawaseBlurPass(
        inputTexture: Int,
        outputFramebuffer: Int,
        width: Int,
        height: Int,
        kawaseOffset: Float,
        applyVignette: Boolean,
        saturation: Float,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFramebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(blurProgram)

        bindQuad(
            buffer = fullscreenQuad,
            positionHandle = blurPositionHandle,
            texCoordHandle = blurTexCoordHandle,
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(blurTextureHandle, 0)
        GLES20.glUniform2f(blurTexelSizeHandle, 1f / offscreenWidth.toFloat(), 1f / offscreenHeight.toFloat())
        GLES20.glUniform1f(blurApplyVignetteHandle, if (applyVignette) 1f else 0f)
        GLES20.glUniform1f(blurKawaseOffsetHandle, kawaseOffset)
        GLES20.glUniform1f(blurSaturationHandle, saturation)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun presentPass(
        inputTexture: Int,
        saturation: Float,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(presentProgram)

        bindQuad(
            buffer = fullscreenQuad,
            positionHandle = presentPositionHandle,
            texCoordHandle = presentTexCoordHandle,
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture)
        GLES20.glUniform1i(presentTextureHandle, 0)
        GLES20.glUniform1f(presentSaturationHandle, saturation)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun drawSprite(
        viewportWidth: Int,
        viewportHeight: Int,
        centerX: Float,
        centerY: Float,
        size: Float,
        rotation: Float,
    ) {
        val half = size * 0.5f

        val topLeft = rotatePoint(-half, -half, rotation)
        val topRight = rotatePoint(half, -half, rotation)
        val bottomLeft = rotatePoint(-half, half, rotation)
        val bottomRight = rotatePoint(half, half, rotation)

        spriteQuad.position(0)
        spriteQuad.put(
            floatArrayOf(
                toNdcX(centerX + bottomLeft.first, viewportWidth), toNdcY(centerY + bottomLeft.second, viewportHeight), 0f, 1f,
                toNdcX(centerX + bottomRight.first, viewportWidth), toNdcY(centerY + bottomRight.second, viewportHeight), 1f, 1f,
                toNdcX(centerX + topLeft.first, viewportWidth), toNdcY(centerY + topLeft.second, viewportHeight), 0f, 0f,
                toNdcX(centerX + topRight.first, viewportWidth), toNdcY(centerY + topRight.second, viewportHeight), 1f, 0f,
            ),
        )
        spriteQuad.position(0)

        bindQuad(
            buffer = spriteQuad,
            positionHandle = spritePositionHandle,
            texCoordHandle = spriteTexCoordHandle,
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun rotatePoint(x: Float, y: Float, angle: Float): Pair<Float, Float> {
        val s = sin(angle)
        val c = cos(angle)
        return Pair(
            x * c - y * s,
            x * s + y * c,
        )
    }

    private fun toNdcX(x: Float, viewportWidth: Int): Float = (x / viewportWidth.toFloat()) * 2f - 1f

    private fun toNdcY(y: Float, viewportHeight: Int): Float = 1f - (y / viewportHeight.toFloat()) * 2f

    private fun bindQuad(
        buffer: FloatBuffer,
        positionHandle: Int,
        texCoordHandle: Int,
    ) {
        buffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, buffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        buffer.position(2)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, buffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
    }

    private fun createOrResizeFramebuffers() {
        if (framebufferIds.any { it != 0 }) {
            GLES20.glDeleteFramebuffers(framebufferIds.size, framebufferIds, 0)
            GLES20.glDeleteTextures(framebufferTextureIds.size, framebufferTextureIds, 0)
        }

        framebufferIds = IntArray(3)
        framebufferTextureIds = IntArray(3)
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