package com.spotifyhub.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spotifyhub.library.model.TrackItem
import com.spotifyhub.ui.common.NowPlayingIndicator
import com.spotifyhub.ui.common.bounceOverscroll
import com.spotifyhub.ui.icons.AppIcons

private val BrowseBackground = Color(0xFF171A1F)
private val SpotifyGreen = Color(0xFF1ED760)

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    currentTrackId: String?,
    isPlaybackActive: Boolean,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowseBackground),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = SpotifyGreen,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Something went wrong",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .bounceOverscroll(orientation = Orientation.Vertical),
                ) {
                    /* Back button */
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onBack)
                                    .padding(4.dp),
                            )
                        }
                    }

                    /* Header with artwork and gradient */
                    item {
                        DetailHeader(
                            title = uiState.title,
                            subtitle = uiState.subtitle,
                            description = uiState.description,
                            artworkUrl = uiState.artworkUrl,
                            onPlay = { viewModel.playFromStart() },
                        )
                    }

                    /* Track list */
                    itemsIndexed(
                        items = uiState.tracks,
                        key = { _, track -> track.id },
                    ) { index, track ->
                        TrackRow(
                            track = track,
                            index = index + 1,
                            isNowPlaying = isPlaybackActive && currentTrackId == track.id,
                            onClick = { viewModel.playContext(trackOffset = index) },
                        )
                    }

                    /* Bottom padding */
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    title: String,
    subtitle: String,
    description: String?,
    artworkUrl: String?,
    onPlay: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A2A2A),
                        BrowseBackground,
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            /* Artwork */
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

            /* Title + subtitle + play button */
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                /* Play button */
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(SpotifyGreen)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.play,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Play",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }

        /* Description */
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TrackRow(
    track: TrackItem,
    index: Int,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isNowPlaying) SpotifyGreen.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (isNowPlaying) {
                NowPlayingIndicator()
            } else {
                Text(
                    text = "$index",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        /* Title + artist */
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isNowPlaying) SpotifyGreen else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = track.artist,
                color = if (isNowPlaying) SpotifyGreen.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        /* Duration */
        Text(
            text = formatTrackDuration(track.durationMs),
            color = if (isNowPlaying) SpotifyGreen else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
