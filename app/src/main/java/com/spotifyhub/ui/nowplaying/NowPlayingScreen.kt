package com.spotifyhub.ui.nowplaying

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spotifyhub.playback.model.PlaybackDevice
import com.spotifyhub.playback.model.PlaybackContentType
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.playback.model.RepeatMode
import com.spotifyhub.theme.SpotifyHubTheme
import com.spotifyhub.ui.icons.AppIcons
import com.spotifyhub.ui.nowplaying.backdrop.AlbumBackdropHost
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/* ── Shape & colour tokens ─────────────────────────────────────────── */

private val SidebarShape = SquircleShape(24.dp)
private val ArtworkShape = SquircleShape(18.dp)

private val SidebarSurface = Color(0x2415181D)
private val SidebarBorder = Color.White.copy(alpha = 0.08f)
private val SurfaceShadow = Color.Black.copy(alpha = 0.20f)
private val SpotifyGreen = Color(0xFF1ED760)

private val PressCircleColor = Color.White.copy(alpha = 0.14f)

/* ── Public entry-point (standalone, legacy) ──────────────────────── */

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
        onSeekBackward = { viewModel.seekBy(-15_000L) },
        onTogglePlayback = viewModel::togglePlayback,
        onSkipNext = viewModel::skipNext,
        onSeekForward = { viewModel.seekBy(30_000L) },
        onToggleSave = viewModel::toggleSaveCurrentItem,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeatMode,
        onSeek = viewModel::seekTo,
    )
}

/* ── Embeddable content (no sidebar — used inside MainScreen) ─────── */

@Composable
fun NowPlayingContent(
    viewModel: PlayerViewModel,
    isOffline: Boolean,
    renderBackdrop: Boolean = true,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val playback = uiState.playback
    val maxPasses = 8
    var blurPassCount by rememberSaveable { mutableStateOf(maxPasses) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        if (renderBackdrop) {
            /* Dynamic album-art backdrop */
            AlbumBackdropHost(
                artworkUrl = playback?.item?.artworkUrl,
                artworkKey = playback?.item?.id,
                blurPassCount = blurPassCount,
                modifier = Modifier.fillMaxSize(),
            )

            /* Subtle gradient overlay */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x2808090C),
                                Color(0x10090A0C),
                                Color(0x32060709),
                            ),
                        ),
                    ),
            )

            /* Debug: blur pass stepper */
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(SquircleShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(6.dp))
                        .clickable(enabled = blurPassCount > 0) {
                            blurPassCount = (blurPassCount - 1).coerceAtLeast(0)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "\u2212",
                        color = if (blurPassCount > 0) Color.White else Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "BLUR $blurPassCount/$maxPasses",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(6.dp))
                        .clickable(enabled = blurPassCount < maxPasses) {
                            blurPassCount = (blurPassCount + 1).coerceAtMost(maxPasses)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "+",
                        color = if (blurPassCount < maxPasses) Color.White else Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        /* Content — no sidebar, just the main area */
        MainContent(
            playback = playback,
            isBusy = uiState.isRefreshing,
            isCurrentItemSaved = uiState.isCurrentItemSaved == true,
            transportEnabled = playback?.item != null && !uiState.isSendingCommand,
            saveEnabled = playback?.item != null && !uiState.isSendingCommand,
            utilityEnabled = playback?.device != null && !uiState.isSendingCommand,
            onSkipPrevious = viewModel::skipPrevious,
            onSeekBackward = { viewModel.seekBy(-15_000L) },
            onTogglePlayback = viewModel::togglePlayback,
            onSkipNext = viewModel::skipNext,
            onSeekForward = { viewModel.seekBy(30_000L) },
            onToggleSave = viewModel::toggleSaveCurrentItem,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeatMode,
            onSeek = viewModel::seekTo,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

/* ── Root layout ───────────────────────────────────────────────────── */

@Composable
fun NowPlayingScreen(
    uiState: PlayerUiState,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekForward: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val playback = uiState.playback
    val maxPasses = 8
    var blurPassCount by rememberSaveable { mutableStateOf(maxPasses) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090B)),
    ) {
        /* Dynamic album-art backdrop (unchanged) */
        AlbumBackdropHost(
            artworkUrl = playback?.item?.artworkUrl,
            artworkKey = playback?.item?.id,
            blurPassCount = blurPassCount,
            modifier = Modifier.fillMaxSize(),
        )

        /* Subtle gradient overlay */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x2808090C),
                            Color(0x10090A0C),
                            Color(0x32060709),
                        ),
                    ),
                ),
        )

        /* Debug: blur pass stepper */
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(SquircleShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(SquircleShape(6.dp))
                    .clickable(enabled = blurPassCount > 0) {
                        blurPassCount = (blurPassCount - 1).coerceAtLeast(0)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "\u2212",
                    color = if (blurPassCount > 0) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "BLUR $blurPassCount/$maxPasses",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Box(
                modifier = Modifier
                    .clip(SquircleShape(6.dp))
                    .clickable(enabled = blurPassCount < maxPasses) {
                        blurPassCount = (blurPassCount + 1).coerceAtMost(maxPasses)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "+",
                    color = if (blurPassCount < maxPasses) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        /* Content */
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SidebarRail(
                isOffline = isOffline,
                modifier = Modifier.fillMaxHeight(),
            )

            MainContent(
                playback = playback,
                isBusy = uiState.isRefreshing,
                isCurrentItemSaved = uiState.isCurrentItemSaved == true,
                transportEnabled = playback?.item != null && !uiState.isSendingCommand,
                saveEnabled = playback?.item != null && !uiState.isSendingCommand,
                utilityEnabled = playback?.device != null && !uiState.isSendingCommand,
                onSkipPrevious = onSkipPrevious,
                onSeekBackward = onSeekBackward,
                onTogglePlayback = onTogglePlayback,
                onSkipNext = onSkipNext,
                onSeekForward = onSeekForward,
                onToggleSave = onToggleSave,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onSeek = onSeek,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/* ── Sidebar (minimal) ─────────────────────────────────────────────── */

@Composable
private fun SidebarRail(
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(62.dp)
            .shadow(16.dp, SidebarShape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(SidebarShape)
            .background(SidebarSurface)
            .border(1.dp, SidebarBorder, SidebarShape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            /* Top: clock */
            Text(
                text = rememberClockLabel(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                ),
                color = Color.White,
            )

            /* Center: Spotify logo */
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        color = Color(0xFF08110B),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    )
                }

                Text(
                    text = if (isOffline) "OFF" else "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = if (isOffline) Color(0xFFFF8B8B) else SpotifyGreen,
                )
            }

            /* Bottom spacer to balance layout */
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/* ── Main content area ─────────────────────────────────────────────── */

@Composable
private fun MainContent(
    playback: PlaybackSnapshot?,
    isBusy: Boolean,
    isCurrentItemSaved: Boolean,
    transportEnabled: Boolean,
    saveEnabled: Boolean,
    utilityEnabled: Boolean,
    onSkipPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekForward: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        /* Hero zone: left column (metadata + transport) | right column (artwork) */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            /* Left: metadata stacked above transport */
            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.Center,
            ) {
                val item = playback?.item

                Text(
                    text = item?.title ?: "Not Playing",
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item?.artist ?: "Connect to start listening",
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )

                item?.album?.takeIf { it.isNotBlank() }?.let { album ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album,
                        color = Color.White.copy(alpha = 0.50f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            letterSpacing = 0.3.sp,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                /* Transport controls — same column, below metadata */
                TransportRow(
                    item = item,
                    isPlaying = playback?.isPlaying == true,
                    enabled = transportEnabled,
                    onSkipPrevious = onSkipPrevious,
                    onSeekBackward = onSeekBackward,
                    onTogglePlayback = onTogglePlayback,
                    onSkipNext = onSkipNext,
                    onSeekForward = onSeekForward,
                )
            }

            /* Right: album artwork */
            ArtworkHero(
                artworkUrl = playback?.item?.artworkUrl,
                title = playback?.item?.title,
                isBusy = isBusy,
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(1f),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        /* Progress bar — full width */
        ProgressSection(playback = playback, onSeek = onSeek)

        Spacer(modifier = Modifier.height(6.dp))

        /* Utility row: shuffle, repeat, save */
        UtilityRow(
            playback = playback,
            isCurrentItemSaved = isCurrentItemSaved,
            saveEnabled = saveEnabled,
            utilityEnabled = utilityEnabled,
            onToggleSave = onToggleSave,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
        )
    }
}

/* ── Artwork ───────────────────────────────────────────────────────── */

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
            .shadow(24.dp, ArtworkShape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(ArtworkShape)
            .background(Color(0x2014191F))
            .border(1.dp, Color.White.copy(alpha = 0.08f), ArtworkShape),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUrl != null) {
            Crossfade(
                targetState = artworkUrl,
                animationSpec = tween(durationMillis = 450),
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
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        } else {
            ArtworkPlaceholder()
        }
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
                        Color(0xFF30343C),
                        Color(0xFF171A1F),
                        Color(0xFF090A0D),
                    ),
                    radius = 620f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "S",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            )
        }
    }
}

/* ── Transport controls ────────────────────────────────────────────── */

@Composable
private fun TransportRow(
    item: PlaybackItem?,
    isPlaying: Boolean,
    enabled: Boolean,
    onSkipPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekForward: () -> Unit,
) {
    val useRelativeSeekControls = item?.contentType == PlaybackContentType.Podcast ||
        item?.contentType == PlaybackContentType.Audiobook

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TouchFeedbackButton(
            onClick = if (useRelativeSeekControls) onSeekBackward else onSkipPrevious,
            enabled = enabled,
            icon = if (useRelativeSeekControls) {
                AppIcons.skip15Backward
            } else {
                AppIcons.skipTrackBackward
            },
            contentDescription = if (useRelativeSeekControls) "Back 15 seconds" else "Previous track",
            iconSize = 52.dp,
            modifier = Modifier.size(72.dp),
            badgeText = if (useRelativeSeekControls) "15" else null,
            badgeInsideIcon = useRelativeSeekControls,
        )

        Spacer(modifier = Modifier.width(24.dp))

        TouchFeedbackButton(
            onClick = onTogglePlayback,
            enabled = enabled,
            icon = if (isPlaying) AppIcons.pause else AppIcons.play,
            contentDescription = if (isPlaying) "Pause" else "Play",
            iconSize = 64.dp,
            modifier = Modifier.size(88.dp),
        )

        Spacer(modifier = Modifier.width(24.dp))

        TouchFeedbackButton(
            onClick = if (useRelativeSeekControls) onSeekForward else onSkipNext,
            enabled = enabled,
            icon = if (useRelativeSeekControls) {
                AppIcons.skip30Forward
            } else {
                AppIcons.skipTrackForward
            },
            contentDescription = if (useRelativeSeekControls) "Forward 30 seconds" else "Next track",
            iconSize = 52.dp,
            modifier = Modifier.size(72.dp),
            badgeText = if (useRelativeSeekControls) "30" else null,
            badgeInsideIcon = useRelativeSeekControls,
        )
    }
}

/* ── Utility row (shuffle, repeat, save) ───────────────────────────── */

@Composable
private fun UtilityRow(
    playback: PlaybackSnapshot?,
    isCurrentItemSaved: Boolean,
    saveEnabled: Boolean,
    utilityEnabled: Boolean,
    onToggleSave: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val repeatMode = playback?.repeatMode ?: RepeatMode.Off

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TouchFeedbackButton(
            onClick = onToggleShuffle,
            enabled = utilityEnabled && playback?.item != null,
            icon = AppIcons.shuffle,
            contentDescription = if (playback?.isShuffleEnabled == true) "Disable shuffle" else "Enable shuffle",
            iconSize = 30.dp,
            modifier = Modifier.size(52.dp),
            activeColor = if (playback?.isShuffleEnabled == true) Color.White else Color.White.copy(alpha = 0.55f),
        )

        Spacer(modifier = Modifier.width(32.dp))

        TouchFeedbackButton(
            onClick = onCycleRepeat,
            enabled = utilityEnabled && playback?.item != null,
            icon = if (repeatMode == RepeatMode.Track) AppIcons.repeatOne else AppIcons.repeat,
            contentDescription = when (repeatMode) {
                RepeatMode.Off -> "Enable repeat"
                RepeatMode.Context -> "Repeat playlist or album"
                RepeatMode.Track -> "Repeat current track"
            },
            iconSize = 30.dp,
            modifier = Modifier.size(52.dp),
            activeColor = if (repeatMode != RepeatMode.Off) Color.White else Color.White.copy(alpha = 0.55f),
            badgeText = if (repeatMode == RepeatMode.Track) "1" else null,
        )

        Spacer(modifier = Modifier.width(32.dp))

        TouchFeedbackButton(
            onClick = onToggleSave,
            enabled = saveEnabled,
            icon = if (isCurrentItemSaved) AppIcons.trackSaved else AppIcons.saveTrack,
            contentDescription = if (isCurrentItemSaved) "Remove from library" else "Save to library",
            iconSize = 30.dp,
            modifier = Modifier.size(52.dp),
            activeColor = if (isCurrentItemSaved) SpotifyGreen else Color.White.copy(alpha = 0.55f),
        )
    }
}

/* ── Progress bar ──────────────────────────────────────────────────── */

@Composable
private fun ProgressSection(
    playback: PlaybackSnapshot?,
    onSeek: (Long) -> Unit,
) {
    val interpolatedMs = rememberInterpolatedProgress(playback)
    val durationMs = max(playback?.durationMs ?: 0L, 1L)

    /* Dragging state — while the user drags we show their position, not the live one */
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isDragging) {
        dragFraction
    } else {
        (interpolatedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
    val displayMs = if (isDragging) {
        (dragFraction * durationMs).toLong().coerceIn(0L, durationMs)
    } else {
        interpolatedMs
    }
    val remainingMs = max(durationMs - displayMs, 0L)

    /* Track width in px for pointer math */
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    /* Thumb animation — grows when dragging */
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "thumb-size",
    )
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 4.dp,
        animationSpec = tween(durationMillis = 150),
        label = "track-height",
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        /* Custom seek bar */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp) /* generous touch target */
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        val seekMs = (fraction * durationMs).toLong()
                        onSeek(seekMs)
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val seekMs = (dragFraction * durationMs).toLong().coerceIn(0L, durationMs)
                            onSeek(seekMs)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragFraction = (dragFraction + dragAmount / trackWidthPx).coerceIn(0f, 1f)
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            /* Track background */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(SquircleShape(99.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
            )

            /* Track filled */
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = displayFraction)
                    .height(trackHeight)
                    .clip(SquircleShape(99.dp))
                    .background(Color.White.copy(alpha = 0.90f)),
            )

            /* Thumb — only visible when dragging / hovering */
            if (thumbSize > 0.dp) {
                val density = LocalDensity.current
                val thumbOffsetX = with(density) {
                    (displayFraction * trackWidthPx).roundToInt() - (thumbSize / 2).roundToPx()
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetX, 0) }
                        .size(thumbSize)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(displayMs),
                color = Color.White.copy(alpha = 0.70f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "-${formatDuration(remainingMs)}",
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/* ── iOS-style touch feedback button ───────────────────────────────── */

@Composable
private fun TouchFeedbackButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    badgeText: String? = null,
    badgeInsideIcon: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 60 else 200),
        label = "press-circle-alpha",
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                if (pressAlpha > 0f) {
                    drawCircle(
                        color = PressCircleColor.copy(alpha = PressCircleColor.alpha * pressAlpha),
                        radius = size.minDimension / 2f,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.18f * pressAlpha),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.5f),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) activeColor else activeColor.copy(alpha = 0.30f),
        )
        if (!badgeText.isNullOrBlank()) {
            Text(
                text = badgeText,
                color = if (enabled) activeColor else activeColor.copy(alpha = 0.30f),
                style = if (badgeInsideIcon) {
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.sp,
                    )
                } else {
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.sp,
                    )
                },
                modifier = if (badgeInsideIcon) {
                    Modifier.align(Alignment.Center)
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-6).dp)
                },
            )
        }
    }
}

/* ── Helpers ────────────────────────────────────────────────────────── */

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

/* ── Previews ──────────────────────────────────────────────────────── */

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
                    isShuffleEnabled = true,
                    repeatMode = RepeatMode.Context,
                    progressMs = 102_000L,
                    durationMs = 230_000L,
                    fetchedAtEpochMs = System.currentTimeMillis() - 800L,
                    item = PlaybackItem(
                        id = "preview-track-1",
                        title = "Starboy",
                        artist = "The Weeknd, Daft Punk",
                        album = "Starboy",
                        artworkUrl = null,
                        releaseDate = "2016-11-25",
                        uri = "spotify:track:preview-1",
                        contentType = PlaybackContentType.Track,
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
                    isShuffleEnabled = false,
                    repeatMode = RepeatMode.Track,
                    progressMs = 201_000L,
                    durationMs = 259_000L,
                    fetchedAtEpochMs = System.currentTimeMillis() - 15_000L,
                    item = PlaybackItem(
                        id = "preview-track-2",
                        title = "Blinding Lights",
                        artist = "The Weeknd",
                        album = "After Hours",
                        artworkUrl = null,
                        releaseDate = "2020-03-20",
                        uri = "spotify:track:preview-2",
                        contentType = PlaybackContentType.Track,
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

@Preview(name = "Now Playing - Wide", widthDp = 900, heightDp = 500, showBackground = true, backgroundColor = 0xFF090B10)
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
            onSeekBackward = {},
            onTogglePlayback = {},
            onSkipNext = {},
            onSeekForward = {},
            onToggleSave = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
            onSeek = {},
        )
    }
}
