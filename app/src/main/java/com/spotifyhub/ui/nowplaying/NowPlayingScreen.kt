package com.spotifyhub.ui.nowplaying

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.spotifyhub.playback.model.PlaybackDevice
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.theme.SpotifyHubTheme
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    isOffline: Boolean,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    NowPlayingScreen(
        uiState = uiState,
        isOffline = isOffline,
        onRefresh = viewModel::refresh,
        onSkipPrevious = viewModel::skipPrevious,
        onTogglePlayback = viewModel::togglePlayback,
        onSkipNext = viewModel::skipNext,
        onToggleSave = viewModel::toggleSaveCurrentItem,
    )
}

@Composable
fun NowPlayingScreen(
    uiState: PlayerUiState,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleSave: () -> Unit,
) {
    val playback = uiState.playback
    val backdrop = rememberLayerBackdrop()
    val backgroundBrush = rememberScreenBackground(playback)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        BackdropArtwork(playback = playback, backdrop = backdrop)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xE60A0C11),
                            Color(0xCC10151F),
                            Color(0xDE111A25),
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SidebarRail(
                modifier = Modifier.fillMaxHeight(),
                backdrop = backdrop,
                isOffline = isOffline,
                onRefresh = onRefresh,
                isBusy = uiState.isRefreshing || uiState.isSendingCommand,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                AmbientGlowLayer()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    HeaderRow(
                        playback = playback,
                        isOffline = isOffline,
                    )

                    MainArtworkRow(
                        playback = playback,
                        isBusy = uiState.isRefreshing,
                        modifier = Modifier.weight(1f),
                    )

                    ProgressSection(playback = playback)

                    ControlDeck(
                        backdrop = backdrop,
                        playback = playback,
                        isCurrentItemSaved = uiState.isCurrentItemSaved == true,
                        enabled = playback?.item != null && !uiState.isSendingCommand,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayback = onTogglePlayback,
                        onSkipNext = onSkipNext,
                        onToggleSave = onToggleSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarRail(
    modifier: Modifier,
    backdrop: LayerBackdrop,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    isBusy: Boolean,
) {
    LiquidSidebarSurface(
        modifier = modifier.width(88.dp),
        backdrop = backdrop,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 18.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = rememberClockLabel(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Color.White,
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1ED760))
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.28f),
                                radius = size.minDimension / 2.8f,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        color = Color(0xFF07120C),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    )
                }

                Text(
                    text = if (isOffline) "OFF" else "LIVE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = if (isOffline) Color(0xFFFF8B8B) else Color(0xFFB9FFD7),
                )
            }

            LiquidIconButton(
                onClick = onRefresh,
                backdrop = backdrop,
                modifier = Modifier.size(52.dp),
                enabled = !isBusy,
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh playback",
                emphasis = LiquidButtonEmphasis.Subtle,
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    playback: PlaybackSnapshot?,
    isOffline: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOffline) Color(0xFFFF8B8B) else Color(0xFF7BFFB2)),
                )
                Text(
                    text = if (playback?.isPlaying == true) "NOW PLAYING" else "SPOTIFY CONTROLLER",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    ),
                )
            }

            playback?.device?.name?.takeIf { it.isNotBlank() }?.let { deviceName ->
                Text(
                    text = deviceName,
                    color = Color(0xFFD7E8FF).copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        AnimatedContent(isOffline, label = "connectivity-banner") { offline ->
            if (offline) {
                Text(
                    text = "No network",
                    color = Color(0xFFFFA7A7),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            } else {
                Text(
                    text = "Spotify Connect",
                    color = Color.White.copy(alpha = 0.64f),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.6.sp),
                )
            }
        }
    }
}

@Composable
private fun MainArtworkRow(
    playback: PlaybackSnapshot?,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkHero(
            artworkUrl = playback?.item?.artworkUrl,
            title = playback?.item?.title,
            isBusy = isBusy,
            modifier = Modifier
                .weight(0.36f)
                .aspectRatio(1f),
        )

        MetadataBlock(
            playback = playback,
            modifier = Modifier.weight(0.64f),
        )
    }
}

@Composable
private fun ArtworkHero(
    artworkUrl: String?,
    title: String?,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x33FFFFFF),
                        Color(0x18FFFFFF),
                        Color(0x08FFFFFF),
                    ),
                    start = Offset.Zero,
                    end = Offset(720f, 720f),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUrl != null) {
            Crossfade(
                targetState = artworkUrl,
                animationSpec = tween(durationMillis = 550),
                label = "artwork-crossfade",
            ) { model ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(model)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        } else if (isBusy) {
            CircularProgressIndicator(color = Color.White)
        } else {
            ArtworkPlaceholder()
        }
    }
}

@Composable
private fun MetadataBlock(
    playback: PlaybackSnapshot?,
    modifier: Modifier = Modifier,
) {
    val item = playback?.item
    val volumeLabel = playback?.device?.volumePercent?.let { "$it%" }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = if (playback?.isPlaying == true) "CURRENT SESSION" else "READY WHEN YOU ARE",
                color = Color(0xFFBFE9FF).copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                ),
            )

            Text(
                text = item?.title ?: "Start playback on another device",
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Black,
                ),
            )

            Text(
                text = item?.artist ?: "Spotify Connect will mirror the active session here.",
                color = Color(0xFFE8EEF8).copy(alpha = 0.86f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            item?.album?.takeIf { it.isNotBlank() }?.let { album ->
                Text(
                    text = album,
                    color = Color.White.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.6.sp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MetadataStat(
                    label = "SESSION",
                    value = if (playback?.item != null) "Remote" else "Idle",
                )

                playback?.device?.type?.takeIf { it.isNotBlank() }?.let { type ->
                    MetadataStat(
                        label = "OUTPUT",
                        value = type,
                    )
                }

                volumeLabel?.let { volume ->
                    MetadataStat(
                        label = "VOLUME",
                        value = volume,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataStat(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.42f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            ),
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.84f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun ArtworkPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B2458).copy(alpha = 0.88f),
                        Color(0xFF1B365A).copy(alpha = 0.62f),
                        Color(0xFF0D1018),
                    ),
                    radius = 620f,
                ),
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.34f,
                    center = Offset(size.width * 0.46f, size.height * 0.48f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10.dp.toPx()),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.20f,
                    center = Offset(size.width * 0.46f, size.height * 0.48f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                )
            }

            Text(
                text = "Waiting for artwork",
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun ProgressSection(playback: PlaybackSnapshot?) {
    val progressMs = rememberInterpolatedProgress(playback)
    val durationMs = max(playback?.durationMs ?: 0L, 1L)
    val progressFraction by animateFloatAsState(
        targetValue = (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "playback-progress",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.18f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(progressMs),
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatDuration(playback?.durationMs ?: 0L),
                color = Color.White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ControlDeck(
    backdrop: LayerBackdrop,
    playback: PlaybackSnapshot?,
    isCurrentItemSaved: Boolean,
    enabled: Boolean,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (playback?.isPlaying == true) "Streaming" else "Paused",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = playback?.device?.name ?: "Waiting for active device",
                color = Color.White.copy(alpha = 0.56f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        TransportRow(
            backdrop = backdrop,
            isPlaying = playback?.isPlaying == true,
            enabled = enabled,
            onSkipPrevious = onSkipPrevious,
            onTogglePlayback = onTogglePlayback,
            onSkipNext = onSkipNext,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiquidIconButton(
                onClick = onToggleSave,
                backdrop = backdrop,
                modifier = Modifier.size(54.dp),
                enabled = enabled,
                icon = if (isCurrentItemSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (isCurrentItemSaved) "Remove from library" else "Save to library",
                emphasis = LiquidButtonEmphasis.Subtle,
            ) {
                AnimatedContent(
                    targetState = isCurrentItemSaved,
                    label = "save-button-icon",
                ) { isSaved ->
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (enabled) {
                            if (isSaved) Color(0xFFB9FFD7) else Color.White
                        } else {
                            Color.White.copy(alpha = 0.42f)
                        },
                    )
                }
            }
            LiquidIconButton(
                onClick = {},
                backdrop = backdrop,
                modifier = Modifier.size(54.dp),
                enabled = false,
                icon = Icons.Rounded.Speaker,
                contentDescription = "Devices",
                emphasis = LiquidButtonEmphasis.Subtle,
            )
        }
    }
}

@Composable
private fun TransportRow(
    backdrop: Backdrop,
    isPlaying: Boolean,
    enabled: Boolean,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidIconButton(
            onClick = onSkipPrevious,
            backdrop = backdrop,
            modifier = Modifier.size(72.dp),
            enabled = enabled,
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous track",
            emphasis = LiquidButtonEmphasis.Subtle,
        )

        LiquidIconButton(
            onClick = onTogglePlayback,
            backdrop = backdrop,
            modifier = Modifier.size(96.dp),
            enabled = enabled,
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause playback" else "Play playback",
            emphasis = LiquidButtonEmphasis.Primary,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = Color.White,
            )
        }

        LiquidIconButton(
            onClick = onSkipNext,
            backdrop = backdrop,
            modifier = Modifier.size(72.dp),
            enabled = enabled,
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next track",
            emphasis = LiquidButtonEmphasis.Subtle,
        )
    }
}

@Composable
private fun BackdropArtwork(
    playback: PlaybackSnapshot?,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val artworkUrl = playback?.item?.artworkUrl

    if (artworkUrl == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2A1B1E),
                            Color(0xFF11151C),
                            Color(0xFF080B10),
                        ),
                    ),
                ),
        )
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(artworkUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .graphicsLayer {
                alpha = 0.88f
                scaleX = 1.08f
                scaleY = 1.08f
            },
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun LiquidSidebarSurface(
    modifier: Modifier,
    backdrop: LayerBackdrop,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(6.dp.toPx())
                lens(12.dp.toPx(), 24.dp.toPx())
                opacity(0.18f)
            },
            highlight = { Highlight.Plain.copy(alpha = 0.92f) },
            shadow = {
                Shadow(
                    radius = 16.dp,
                    color = Color.Black.copy(alpha = 0.16f),
                )
            },
            innerShadow = {
                InnerShadow(
                    radius = 4.dp,
                    alpha = 0.28f,
                )
            },
            onDrawSurface = {
                drawRect(Color.White.copy(alpha = 0.09f))
            },
        ),
    ) {
        content()
    }
}

private enum class LiquidButtonEmphasis {
    Primary,
    Subtle,
}

@Composable
private fun LiquidIconButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    emphasis: LiquidButtonEmphasis,
    content: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "liquid-button-scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(
                        refractionHeight = if (emphasis == LiquidButtonEmphasis.Primary) 14.dp.toPx() else 10.dp.toPx(),
                        refractionAmount = if (emphasis == LiquidButtonEmphasis.Primary) 22.dp.toPx() else 16.dp.toPx(),
                    )
                },
                highlight = {
                    if (emphasis == LiquidButtonEmphasis.Primary) {
                        Highlight.Plain.copy(alpha = if (enabled) 1f else 0.45f)
                    } else {
                        Highlight.Ambient.copy(alpha = if (enabled) 0.75f else 0.38f)
                    }
                },
                shadow = {
                    Shadow(
                        radius = if (emphasis == LiquidButtonEmphasis.Primary) 14.dp else 10.dp,
                        color = Color.Black.copy(alpha = 0.14f),
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = if (isPressed) 10.dp else 4.dp,
                        alpha = if (enabled) 0.30f else 0.18f,
                    )
                },
                onDrawSurface = {
                    val tint = when (emphasis) {
                        LiquidButtonEmphasis.Primary -> Color(0xFFC8D7FF)
                        LiquidButtonEmphasis.Subtle -> Color.White
                    }
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(
                        color = tint.copy(alpha = if (emphasis == LiquidButtonEmphasis.Primary) 0.14f else 0.08f),
                    )
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(38.dp),
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.42f),
            )
        }
    }
}

@Composable
private fun AmbientGlowLayer() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6B2B4E).copy(alpha = 0.24f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.22f, size.height * 0.38f),
                        radius = size.width * 0.34f,
                    ),
                    radius = size.width * 0.34f,
                    center = Offset(size.width * 0.22f, size.height * 0.38f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2A78D3).copy(alpha = 0.20f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.72f),
                        radius = size.width * 0.30f,
                    ),
                    radius = size.width * 0.30f,
                    center = Offset(size.width * 0.78f, size.height * 0.72f),
                )
            },
    )
}

@Composable
private fun rememberScreenBackground(playback: PlaybackSnapshot?): Brush {
    return remember(playback?.item?.id) {
        val seed = (playback?.item?.id ?: "spotifyhub").hashCode()
        val left = Color(0xFF11141D).mix(Color(0xFF6F233B), seed, 0)
        val center = Color(0xFF111923).mix(Color(0xFF2C4E9E), seed, 1)
        val right = Color(0xFF091019).mix(Color(0xFF15324D), seed, 2)

        Brush.linearGradient(colors = listOf(left, center, right))
    }
}

@Composable
private fun rememberInterpolatedProgress(playback: PlaybackSnapshot?): Long {
    val progress by produceState(
        initialValue = playback?.progressMs ?: 0L,
        playback?.item?.id,
        playback?.progressMs,
        playback?.isPlaying,
        playback?.durationMs,
        playback?.fetchedAtEpochMs,
    ) {
        if (playback == null) {
            value = 0L
            return@produceState
        }

        while (true) {
            val interpolated = if (playback.isPlaying) {
                val elapsed = max(System.currentTimeMillis() - playback.fetchedAtEpochMs, 0L)
                min(playback.progressMs + elapsed, playback.durationMs)
            } else {
                playback.progressMs
            }
            value = interpolated
            delay(180L)
        }
    }
    return progress
}

@Composable
private fun rememberClockLabel(): String {
    val context = LocalContext.current
    val label by produceState(initialValue = "", key1 = context) {
        while (true) {
            value = DateFormat.format("h:mm", System.currentTimeMillis()).toString()
            delay(30_000L)
        }
    }
    return label
}

private fun formatDuration(durationMs: Long): String {
    val safeDuration = max(durationMs / 1000L, 0L)
    val minutes = safeDuration / 60
    val seconds = safeDuration % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun Color.mix(other: Color, seed: Int, salt: Int): Color {
    val mix = (((seed shr (salt * 4)) and 0xFF) / 255f).coerceIn(0.18f, 0.82f)
    return Color(
        red = (red * (1f - mix)) + (other.red * mix),
        green = (green * (1f - mix)) + (other.green * mix),
        blue = (blue * (1f - mix)) + (other.blue * mix),
        alpha = 1f,
    )
}

private data class NowPlayingPreviewState(
    val name: String,
    val uiState: PlayerUiState,
    val isOffline: Boolean,
) {
    override fun toString(): String = name
}

private class NowPlayingPreviewStateProvider : PreviewParameterProvider<NowPlayingPreviewState> {
    override val values: Sequence<NowPlayingPreviewState> = sequenceOf(
        NowPlayingPreviewState(
            name = "Playing",
            uiState = PlayerUiState(
                playback = PlaybackSnapshot(
                    isPlaying = true,
                    progressMs = 102_000L,
                    durationMs = 230_000L,
                    fetchedAtEpochMs = System.currentTimeMillis() - 800L,
                    item = PlaybackItem(
                        id = "preview-track-1",
                        title = "Starboy",
                        artist = "The Weeknd, Daft Punk",
                        album = "Starboy",
                        artworkUrl = null,
                        uri = "spotify:track:preview-1",
                    ),
                    device = PlaybackDevice(
                        id = "preview-device-1",
                        name = "Desk Speaker",
                        type = "Speaker",
                        volumePercent = 48,
                    ),
                ),
                isCurrentItemSaved = false,
                isRefreshing = false,
                isSendingCommand = false,
            ),
            isOffline = false,
        ),
        NowPlayingPreviewState(
            name = "Paused Offline",
            uiState = PlayerUiState(
                playback = PlaybackSnapshot(
                    isPlaying = false,
                    progressMs = 201_000L,
                    durationMs = 259_000L,
                    fetchedAtEpochMs = System.currentTimeMillis() - 15_000L,
                    item = PlaybackItem(
                        id = "preview-track-2",
                        title = "Blinding Lights",
                        artist = "The Weeknd",
                        album = "After Hours",
                        artworkUrl = null,
                        uri = "spotify:track:preview-2",
                    ),
                    device = PlaybackDevice(
                        id = "preview-device-2",
                        name = "Living Room TV",
                        type = "TV",
                        volumePercent = 36,
                    ),
                ),
                isCurrentItemSaved = true,
                isRefreshing = false,
                isSendingCommand = true,
            ),
            isOffline = true,
        ),
        NowPlayingPreviewState(
            name = "Empty",
            uiState = PlayerUiState(
                playback = null,
                isCurrentItemSaved = null,
                isRefreshing = false,
                isSendingCommand = false,
            ),
            isOffline = false,
        ),
    )
}

@Preview(name = "Now Playing - Wide", widthDp = 960, heightDp = 480, showBackground = true, backgroundColor = 0xFF090B10)
@Composable
private fun NowPlayingWidePreview(
    @PreviewParameter(NowPlayingPreviewStateProvider::class) preview: NowPlayingPreviewState,
) {
    SpotifyHubTheme {
        NowPlayingScreen(
            uiState = preview.uiState,
            isOffline = preview.isOffline,
            onRefresh = {},
            onSkipPrevious = {},
            onTogglePlayback = {},
            onSkipNext = {},
            onToggleSave = {},
        )
    }
}
