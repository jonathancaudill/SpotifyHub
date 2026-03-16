package com.spotifyhub.ui.nowplaying.backdrop

import android.opengl.GLSurfaceView
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AlbumBackdropHost(
    artworkUrl: String?,
    artworkKey: String?,
    blurEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
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
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                GLSurfaceView(viewContext).apply {
                    setEGLContextClientVersion(2)
                    preserveEGLContextOnPause = true
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    controller.bindRenderer(renderer)
                    controller.attachRenderQueue { task -> queueEvent(task) }
                    glSurfaceView = this
                }
            },
            update = { surfaceView ->
                controller.bindRenderer(renderer)
                controller.attachRenderQueue { task -> surfaceView.queueEvent(task) }
                glSurfaceView = surfaceView
            },
        )
    }

    LaunchedEffect(artworkUrl, artworkKey) {
        controller.setArtwork(artworkUrl = artworkUrl, artworkKey = artworkKey)
    }

    LaunchedEffect(glSurfaceView, blurEnabled) {
        glSurfaceView?.queueEvent {
            renderer.setBlurEnabled(blurEnabled)
        }
    }

    LaunchedEffect(glSurfaceView) {
        while (isActive) {
            glSurfaceView?.requestRender()
            delay(33L)
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    controller.onStart()
                    glSurfaceView?.onResume()
                }

                Lifecycle.Event.ON_STOP -> {
                    controller.onStop()
                    glSurfaceView?.onPause()
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            controller.onStart()
            glSurfaceView?.onResume()
        }

        onDispose {
            lifecycle.removeObserver(observer)
            controller.onStop()
            glSurfaceView?.onPause()
        }
    }
}
