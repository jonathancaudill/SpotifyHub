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
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumBackdropController(
    private val appContext: Context,
    private val scope: CoroutineScope,
) {
    private companion object {
        const val BITMAP_SIZE = 512
        const val BITMAP_CACHE_BYTES = 4 * BITMAP_SIZE * BITMAP_SIZE * 4
        const val MAX_LOAD_ATTEMPTS = 3
        const val LOAD_RETRY_DELAY_MS = 350L
    }

    private val imageLoader = appContext.imageLoader
    private val bitmapCache = object : LruCache<String, Bitmap>(BITMAP_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
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
    private var currentArtworkKey: String? = null
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
        if (artworkUrl == currentArtworkUrl && artworkKey == currentArtworkKey) {
            return
        }

        currentArtworkUrl = artworkUrl
        currentArtworkKey = artworkKey
        loadJob?.cancel()
        val previousSlot = currentSlot
        val seedKey = artworkKey?.takeIf(String::isNotBlank) ?: artworkUrl ?: "idle"

        if (artworkUrl.isNullOrBlank()) {
            currentSlot = idleSlot.copy(seed = BackdropSeedFactory.from(seedKey))
            pushState(previous = previousSlot.takeUnless { it.key == currentSlot.key }, animate = previousSlot.key != currentSlot.key)
            return
        }

        loadJob = scope.launch {
            var bitmap: Bitmap? = null
            for (attempt in 0 until MAX_LOAD_ATTEMPTS) {
                bitmap = loadBitmap(artworkUrl)
                if (bitmap != null || !isActive || attempt == MAX_LOAD_ATTEMPTS - 1) {
                    break
                }
                delay(LOAD_RETRY_DELAY_MS)
            }

            val resolvedBitmap = bitmap
            if (resolvedBitmap == null) {
                if (currentArtworkUrl == artworkUrl && currentArtworkKey == artworkKey && previousSlot.key == idleSlot.key) {
                    currentSlot = idleSlot.copy(seed = BackdropSeedFactory.from(seedKey))
                    pushState(previous = null, animate = false)
                }
                return@launch
            }

            val nextSlot = BackdropTextureSlot(
                bitmap = resolvedBitmap,
                aspectRatio = max(resolvedBitmap.width, 1).toFloat() / max(resolvedBitmap.height, 1).toFloat(),
                seed = BackdropSeedFactory.from(seedKey),
                key = artworkUrl,
            )
            currentSlot = nextSlot
            pushState(previous = previousSlot.takeUnless { it.key == nextSlot.key }, animate = previousSlot.key != nextSlot.key)
        }
    }

    fun refreshCurrentState() {
        pushState(previous = null, animate = false)
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
            .size(BITMAP_SIZE, BITMAP_SIZE)
            .build()

        val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null
        val bitmap = result.drawable.toBitmap(
            width = BITMAP_SIZE,
            height = BITMAP_SIZE,
            config = Bitmap.Config.ARGB_8888,
        )
        bitmapCache.put(artworkUrl, bitmap)
        bitmap
    }

    private fun createIdleBitmap(): Bitmap {
        return Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    BITMAP_SIZE.toFloat(),
                    BITMAP_SIZE.toFloat(),
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
