package com.spotifyhub.ui.nowplaying.backdrop

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.SurfaceView
import android.view.View
import android.view.Choreographer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

private const val BACKDROP_TARGET_FPS = 20f
private const val SURFACE_BUFFER_SCALE = 1.0f
private const val FIXED_BLUR_PASS_COUNT = 7

@Composable
fun AlbumBackdropHost(
    artworkUrl: String?,
    blurPassCount: Int = 8,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val isInspecting = LocalInspectionMode.current

    if (isInspecting) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF11141A),
                            Color(0xFF0A0D11),
                            Color(0xFF050608),
                        ),
                    ),
                ),
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val controller = remember {
        AlbumBackdropController(
            appContext = context.applicationContext,
            scope = scope,
        )
    }
    val renderer = remember {
        AlbumBackdropRenderer(context.applicationContext)
    }
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
    var isStarted by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            GLSurfaceView(viewContext).apply {
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 0, 0, 0)
                holder.setFormat(PixelFormat.OPAQUE)
                preserveEGLContextOnPause = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setSurfaceLifecycle(SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_VISIBILITY)
                }
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _, _ ->
                    updateSurfaceBufferSize(
                        surfaceView = view as GLSurfaceView,
                        isVisible = view.visibility == View.VISIBLE,
                    )
                }
                controller.bindRenderer(renderer)
                controller.attachRenderQueue { task -> queueEvent(task) }
                glSurfaceView = this
                post {
                    updateSurfacePresentation(
                        surfaceView = this,
                        isVisible = isVisible,
                    )
                }
                queueEvent {
                    renderer.resetTime()
                }
            }
        },
        update = { view ->
            updateSurfacePresentation(
                surfaceView = view,
                isVisible = isVisible,
            )
        },
    )

    LaunchedEffect(artworkUrl) {
        controller.setArtwork(artworkUrl = artworkUrl)
    }

    LaunchedEffect(isVisible, isStarted) {
        if (isVisible && isStarted) {
            controller.refreshCurrentState()
        }
    }

    LaunchedEffect(glSurfaceView, blurPassCount) {
        glSurfaceView?.queueEvent {
            renderer.setBlurPassCount(FIXED_BLUR_PASS_COUNT)
        }
    }

    DisposableEffect(glSurfaceView, isVisible, isStarted) {
        val surfaceView = glSurfaceView
        if (surfaceView == null || !isVisible || !isStarted) {
            onDispose { }
        } else {
            val choreographer = Choreographer.getInstance()
            val minFrameIntervalNanos = (1_000_000_000L / BACKDROP_TARGET_FPS).toLong()
            var lastRenderFrameTimeNanos = 0L

            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (isStarted &&
                        surfaceView.hasWindowFocus() &&
                        surfaceView.windowVisibility == View.VISIBLE &&
                        surfaceView.isShown &&
                        (lastRenderFrameTimeNanos == 0L || frameTimeNanos - lastRenderFrameTimeNanos >= minFrameIntervalNanos)
                    ) {
                        lastRenderFrameTimeNanos = frameTimeNanos
                        surfaceView.requestRender()
                    }
                    choreographer.postFrameCallback(this)
                }
            }

            choreographer.postFrameCallback(callback)

            onDispose {
                choreographer.removeFrameCallback(callback)
            }
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isStarted = true
                    controller.onStart()
                    glSurfaceView?.onResume()
                    glSurfaceView?.queueEvent {
                        renderer.resetTime()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    isStarted = false
                    controller.onStop()
                    glSurfaceView?.queueEvent {
                        renderer.release()
                    }
                    glSurfaceView?.onPause()
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            isStarted = true
            controller.onStart()
            glSurfaceView?.onResume()
        }

        onDispose {
            isStarted = false
            lifecycle.removeObserver(observer)
            controller.onStop()
            glSurfaceView?.queueEvent {
                renderer.release()
            }
            glSurfaceView?.onPause()
        }
    }
}

private fun updateSurfacePresentation(
    surfaceView: GLSurfaceView,
    isVisible: Boolean,
) {
    val visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    if (surfaceView.visibility != visibility) {
        surfaceView.visibility = visibility
    }
    updateSurfaceBufferSize(surfaceView = surfaceView, isVisible = isVisible)
}

private fun updateSurfaceBufferSize(
    surfaceView: GLSurfaceView,
    isVisible: Boolean,
) {
    val targetWidth: Int
    val targetHeight: Int

    if (!isVisible) {
        targetWidth = 1
        targetHeight = 1
    } else {
        val width = surfaceView.width
        val height = surfaceView.height
        if (width <= 0 || height <= 0) {
            return
        }
        targetWidth = maxOf((width * SURFACE_BUFFER_SCALE).roundToInt(), 1)
        targetHeight = maxOf((height * SURFACE_BUFFER_SCALE).roundToInt(), 1)
    }

    val surfaceFrame = surfaceView.holder.surfaceFrame
    if (surfaceFrame.width() != targetWidth || surfaceFrame.height() != targetHeight) {
        surfaceView.holder.setFixedSize(targetWidth, targetHeight)
    }
}
