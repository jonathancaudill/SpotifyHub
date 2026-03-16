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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
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
import com.spotifyhub.BuildConfig
import com.spotifyhub.playback.model.PlaybackDevice
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.playback.model.RepeatMode
import com.spotifyhub.theme.SpotifyHubTheme
import com.spotifyhub.ui.nowplaying.backdrop.AlbumBackdropHost
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private val SidebarShape = RoundedCornerShape(28.dp)
private val HeroShape = RoundedCornerShape(30.dp)
private val SecondaryButtonShape = CircleShape
private val UtilityChipShape = RoundedCornerShape(999.dp)
private val SidebarSurface = Color(0x3015181D)
private val SidebarBorder = Color.White.copy(alpha = 0.10f)
private val ButtonSurface = Color(0x2E15191F)
private val ButtonSurfacePressed = Color(0x4D20252D)
private val ButtonBorder = Color.White.copy(alpha = 0.12f)
private val ButtonActiveSurface = Color(0x29FFFFFF)
private val ButtonActiveBorder = Color.White.copy(alpha = 0.24f)
private val ButtonPrimarySurface = Color(0x40FFFFFF)
private val SurfaceShadow = Color.Black.copy(alpha = 0.26f)
private val SpotifyGreen = Color(0xFF1ED760)

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
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeatMode,
        onVolumeDown = { viewModel.adjustVolume(deltaPercent = -5) },
        onVolumeUp = { viewModel.adjustVolume(deltaPercent = 5) },
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
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
) {
    val playback = uiState.playback
    var blurEnabled by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090B)),
    ) {
        AlbumBackdropHost(
            artworkUrl = playback?.item?.artworkUrl,
            artworkKey = playback?.item?.id,
            blurEnabled = blurEnabled,
            modifier = Modifier.fillMaxSize(),
        )

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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SidebarRail(
                modifier = Modifier.fillMaxHeight(),
                isOffline = isOffline,
                onRefresh = onRefresh,
                isBusy = uiState.isRefreshing || uiState.isSendingCommand,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    HeaderRow(
                        playback = playback,
                        isOffline = isOffline,
                        blurEnabled = blurEnabled,
                        onToggleBlur = if (BuildConfig.DEBUG) {
                            { blurEnabled = !blurEnabled }
                        } else {
                            null
                        },
                    )

                    MainArtworkRow(
                        playback = playback,
                        isBusy = uiState.isRefreshing,
                        modifier = Modifier.weight(1f),
                    )

                    ProgressSection(playback = playback)

                    ControlDeck(
                        playback = playback,
                        isCurrentItemSaved = uiState.isCurrentItemSaved == true,
                        transportEnabled = playback?.item != null && !uiState.isSendingCommand,
                        saveEnabled = playback?.item != null && !uiState.isSendingCommand,
                        utilityEnabled = playback?.device != null && !uiState.isSendingCommand,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayback = onTogglePlayback,
                        onSkipNext = onSkipNext,
                        onToggleSave = onToggleSave,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onVolumeDown = onVolumeDown,
                        onVolumeUp = onVolumeUp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarRail(
    modifier: Modifier,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    isBusy: Boolean,
) {
    SidebarRailSurface(
        modifier = modifier.width(88.dp),
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
                        .background(SpotifyGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        color = Color(0xFF08110B),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    )
                }

                Text(
                    text = if (isOffline) "OFF" else "LIVE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = if (isOffline) Color(0xFFFF8B8B) else SpotifyGreen,
                )
            }

            SecondaryControlButton(
                onClick = onRefresh,
                enabled = !isBusy,
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh playback",
                modifier = Modifier.size(52.dp),
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
private fun SidebarRailSurface(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(24.dp, SidebarShape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(SidebarShape)
            .background(SidebarSurface)
            .border(1.dp, SidebarBorder, SidebarShape),
    ) {
        content()
    }
}

@Composable
private fun HeaderRow(
    playback: PlaybackSnapshot?,
    isOffline: Boolean,
    blurEnabled: Boolean,
    onToggleBlur: (() -> Unit)?,
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
                        .background(if (isOffline) Color(0xFFFF8B8B) else SpotifyGreen),
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
                    color = Color.White.copy(alpha = 0.94f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        AnimatedContent(isOffline, label = "connectivity-banner") { offline ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (offline) {
                    Text(
                        text = "No network",
                        color = Color(0xFFFFA7A7),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                } else {
                    Text(
                        text = "Spotify Connect",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.6.sp),
                    )
                }

                onToggleBlur?.let {
                    ToggleChip(
                        label = if (blurEnabled) "Blur On" else "Blur Off",
                        onClick = it,
                    )
                }
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
            .shadow(32.dp, HeroShape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(HeroShape)
            .background(Color(0x2614191F))
            .border(1.dp, Color.White.copy(alpha = 0.10f), HeroShape),
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
                color = Color.White.copy(alpha = 0.68f),
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
                color = Color.White.copy(alpha = 0.84f),
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
                    color = Color.White.copy(alpha = 0.54f),
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
            color = Color.White.copy(alpha = 0.40f),
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
                        Color(0xFF30343C),
                        Color(0xFF171A1F),
                        Color(0xFF090A0D),
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
            color = Color.White.copy(alpha = 0.96f),
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
    playback: PlaybackSnapshot?,
    isCurrentItemSaved: Boolean,
    transportEnabled: Boolean,
    saveEnabled: Boolean,
    utilityEnabled: Boolean,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
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
            isPlaying = playback?.isPlaying == true,
            enabled = transportEnabled,
            onSkipPrevious = onSkipPrevious,
            onTogglePlayback = onTogglePlayback,
            onSkipNext = onSkipNext,
        )

        UtilityTray(
            playback = playback,
            isCurrentItemSaved = isCurrentItemSaved,
            saveEnabled = saveEnabled,
            utilityEnabled = utilityEnabled,
            onToggleSave = onToggleSave,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onVolumeDown = onVolumeDown,
            onVolumeUp = onVolumeUp,
        )
    }
}

@Composable
private fun TransportRow(
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
        SecondaryControlButton(
            onClick = onSkipPrevious,
            enabled = enabled,
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous track",
            modifier = Modifier.size(72.dp),
        )

        PrimaryTransportButton(
            onClick = onTogglePlayback,
            enabled = enabled,
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause playback" else "Play playback",
            modifier = Modifier.size(96.dp),
        )

        SecondaryControlButton(
            onClick = onSkipNext,
            enabled = enabled,
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next track",
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun UtilityTray(
    playback: PlaybackSnapshot?,
    isCurrentItemSaved: Boolean,
    saveEnabled: Boolean,
    utilityEnabled: Boolean,
    onToggleSave: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
) {
    val repeatMode = playback?.repeatMode ?: RepeatMode.Off
    val volumeLabel = playback?.device?.volumePercent?.let { "$it%" } ?: "--"

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryControlButton(
                onClick = onToggleShuffle,
                enabled = utilityEnabled && playback?.item != null,
                icon = Icons.Rounded.Shuffle,
                contentDescription = if (playback?.isShuffleEnabled == true) "Disable shuffle" else "Enable shuffle",
                modifier = Modifier.size(50.dp),
                active = playback?.isShuffleEnabled == true,
            )

            SecondaryControlButton(
                onClick = onCycleRepeat,
                enabled = utilityEnabled && playback?.item != null,
                icon = if (repeatMode == RepeatMode.Track) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = when (repeatMode) {
                    RepeatMode.Off -> "Enable repeat"
                    RepeatMode.Context -> "Repeat playlist or album"
                    RepeatMode.Track -> "Repeat current track"
                },
                modifier = Modifier.size(50.dp),
                active = repeatMode != RepeatMode.Off,
            )

            SecondaryControlButton(
                onClick = onToggleSave,
                enabled = saveEnabled,
                icon = if (isCurrentItemSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (isCurrentItemSaved) "Remove from library" else "Save to library",
                modifier = Modifier.size(50.dp),
                active = isCurrentItemSaved,
                iconTint = if (isCurrentItemSaved) SpotifyGreen else Color.White,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryControlButton(
                onClick = onVolumeDown,
                enabled = utilityEnabled && playback?.device?.volumePercent != null,
                icon = Icons.AutoMirrored.Rounded.VolumeDown,
                contentDescription = "Lower volume",
                modifier = Modifier.size(46.dp),
            )

            UtilityChip(label = volumeLabel)

            SecondaryControlButton(
                onClick = onVolumeUp,
                enabled = utilityEnabled && playback?.device?.volumePercent != null,
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Raise volume",
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

@Composable
private fun PrimaryTransportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    SurfaceButton(
        onClick = onClick,
        enabled = enabled,
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = CircleShape,
        active = true,
        primary = true,
        iconTint = Color.White,
    )
}

@Composable
private fun SecondaryControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconTint: Color = Color.White,
    content: (@Composable () -> Unit)? = null,
) {
    SurfaceButton(
        onClick = onClick,
        enabled = enabled,
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = SecondaryButtonShape,
        active = active,
        primary = false,
        iconTint = iconTint,
        content = content,
    )
}

@Composable
private fun SurfaceButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    active: Boolean,
    primary: Boolean,
    iconTint: Color,
    content: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "surface-button-scale",
    )

    val fill = when {
        primary -> ButtonPrimarySurface
        active -> ButtonActiveSurface
        isPressed && enabled -> ButtonSurfacePressed
        else -> ButtonSurface
    }
    val border = when {
        primary || active -> ButtonActiveBorder
        else -> ButtonBorder
    }

    Box(
        modifier = modifier
            .shadow(18.dp, shape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (primary) 0.14f else 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.34f, size.height * 0.26f),
                        radius = size.maxDimension * 0.76f,
                    ),
                    radius = size.maxDimension * 0.76f,
                    center = Offset(size.width * 0.34f, size.height * 0.26f),
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (primary) 46.dp else 32.dp),
                tint = if (enabled) iconTint else iconTint.copy(alpha = 0.42f),
            )
        }
    }
}

@Composable
private fun UtilityChip(label: String) {
    Box(
        modifier = Modifier
            .clip(UtilityChipShape)
            .background(Color(0x2215191F))
            .border(1.dp, Color.White.copy(alpha = 0.10f), UtilityChipShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.84f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(UtilityChipShape)
            .background(Color(0x2A15191F))
            .border(1.dp, Color.White.copy(alpha = 0.14f), UtilityChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
        )
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
            onToggleShuffle = {},
            onCycleRepeat = {},
            onVolumeDown = {},
            onVolumeUp = {},
        )
    }
}
