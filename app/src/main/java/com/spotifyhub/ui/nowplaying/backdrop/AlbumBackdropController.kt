package com.spotifyhub.ui.nowplaying.backdrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumBackdropController(
    private val appContext: Context,
    private val scope: CoroutineScope,
) {
    private val imageLoader = ImageLoader.Builder(appContext)
        .allowHardware(false)
        .build()
    private val bitmapCache = LruCache<String, Bitmap>(8)
    private val idleBitmap = createIdleBitmap()
    private val idleSlot = BackdropTextureSlot(
        bitmap = idleBitmap,
        aspectRatio = 1f,
        seed = BackdropSeedFactory.from("idle"),
        key = "idle",
    )

    private var renderer: AlbumBackdropRenderer? = null
    private var renderQueue: (((() -> Unit) -> Unit))? = null
    private var loadJob: Job? = null
    private var currentSlot: BackdropTextureSlot = idleSlot
    private var currentArtworkUrl: String? = null
    private var isStarted = false

    fun bindRenderer(renderer: AlbumBackdropRenderer) {
        this.renderer = renderer
        pushState(previous = null, animate = false)
    }

    fun attachRenderQueue(renderQueue: ((() -> Unit) -> Unit)) {
        this.renderQueue = renderQueue
        pushState(previous = null, animate = false)
    }

    fun onStart() {
        isStarted = true
        pushState(previous = null, animate = false)
    }

    fun onStop() {
        isStarted = false
        loadJob?.cancel()
    }

    fun setArtwork(artworkUrl: String?, artworkKey: String?) {
        if (artworkUrl == currentArtworkUrl) {
            return
        }

        currentArtworkUrl = artworkUrl
        loadJob?.cancel()
        val previousSlot = currentSlot
        val seedKey = artworkKey?.takeIf(String::isNotBlank) ?: artworkUrl ?: "idle"

        if (artworkUrl.isNullOrBlank()) {
            currentSlot = idleSlot.copy(seed = BackdropSeedFactory.from(seedKey))
            pushState(previous = previousSlot.takeUnless { it.key == currentSlot.key }, animate = previousSlot.key != currentSlot.key)
            return
        }

        loadJob = scope.launch {
            val bitmap = loadBitmap(artworkUrl) ?: idleBitmap
            val nextSlot = BackdropTextureSlot(
                bitmap = bitmap,
                aspectRatio = max(bitmap.width, 1).toFloat() / max(bitmap.height, 1).toFloat(),
                seed = BackdropSeedFactory.from(seedKey),
                key = artworkUrl,
            )
            currentSlot = nextSlot
            pushState(previous = previousSlot.takeUnless { it.key == nextSlot.key }, animate = previousSlot.key != nextSlot.key)
        }
    }

    private fun pushState(previous: BackdropTextureSlot?, animate: Boolean) {
        if (!isStarted) {
            return
        }

        val renderer = renderer ?: return
        val renderQueue = renderQueue ?: return
        val state = BackdropTransitionState(
            current = currentSlot,
            previous = previous,
            animate = animate,
        )
        renderQueue {
            renderer.setTransitionState(state)
        }
    }

    private suspend fun loadBitmap(artworkUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache.get(artworkUrl)?.let { return@withContext it }

        val request = ImageRequest.Builder(appContext)
            .data(artworkUrl)
            .allowHardware(false)
            .size(512, 512)
            .build()

        val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null
        val bitmap = result.drawable.toBitmap(
            width = 512,
            height = 512,
            config = Bitmap.Config.ARGB_8888,
        )
        bitmapCache.put(artworkUrl, bitmap)
        bitmap
    }

    private fun createIdleBitmap(): Bitmap {
        return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    512f,
                    512f,
                    intArrayOf(
                        android.graphics.Color.rgb(18, 20, 24),
                        android.graphics.Color.rgb(34, 36, 41),
                        android.graphics.Color.rgb(10, 12, 16),
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, 512f, 512f, backgroundPaint)

            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    180f,
                    168f,
                    240f,
                    intArrayOf(
                        android.graphics.Color.argb(180, 230, 230, 230),
                        android.graphics.Color.argb(90, 120, 120, 120),
                        android.graphics.Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.38f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(180f, 168f, 240f, glowPaint)

            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    362f,
                    344f,
                    224f,
                    intArrayOf(
                        android.graphics.Color.argb(160, 188, 188, 188),
                        android.graphics.Color.argb(70, 90, 90, 90),
                        android.graphics.Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.42f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(362f, 344f, 224f, accentPaint)
        }
    }
}
