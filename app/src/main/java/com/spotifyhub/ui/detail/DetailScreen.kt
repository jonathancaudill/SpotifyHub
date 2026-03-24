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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.spotifyhub.artist.model.ArtistBio
import com.spotifyhub.artist.model.ArtistDetail
import com.spotifyhub.artist.model.ArtistPlaylistSummary
import com.spotifyhub.artist.model.ArtistReleaseSummary
import com.spotifyhub.library.model.TrackItem
import com.spotifyhub.ui.common.NowPlayingIndicator
import com.spotifyhub.ui.common.bounceOverscroll
import com.spotifyhub.ui.icons.AppIcons
import sv.lib.squircleshape.SquircleShape

private val BrowseBackground = Color(0xFF171A1F)
private val SpotifyGreen = Color(0xFF1ED760)
private val CardShape = SquircleShape(14.dp)

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

            uiState.content != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .bounceOverscroll(orientation = Orientation.Vertical),
                ) {
                    item {
                        DetailBackButton(onBack = onBack)
                    }

                    when (val content = uiState.content) {
                        is DetailContent.Album -> {
                            item {
                                CollectionHeader(
                                    title = content.title,
                                    subtitle = content.subtitle,
                                    description = null,
                                    artworkUrl = content.artworkUrl,
                                    onPlay = { viewModel.playFromStart() },
                                )
                            }
                            itemsIndexed(content.tracks, key = { _, track -> track.id }) { index, track ->
                                TrackRow(
                                    track = track,
                                    index = index + 1,
                                    isNowPlaying = isPlaybackActive && currentTrackId == track.id,
                                    onClick = { viewModel.playContext(trackOffset = index) },
                                )
                            }
                        }

                        is DetailContent.Playlist -> {
                            item {
                                CollectionHeader(
                                    title = content.title,
                                    subtitle = content.subtitle,
                                    description = content.description,
                                    artworkUrl = content.artworkUrl,
                                    onPlay = { viewModel.playFromStart() },
                                )
                            }
                            itemsIndexed(content.tracks, key = { _, track -> track.id }) { index, track ->
                                TrackRow(
                                    track = track,
                                    index = index + 1,
                                    isNowPlaying = isPlaybackActive && currentTrackId == track.id,
                                    onClick = { viewModel.playContext(trackOffset = index) },
                                )
                            }
                        }

                        is DetailContent.Artist -> {
                            item {
                                ArtistHeader(detail = content.detail)
                            }
                            artistSections(
                                detail = content.detail,
                                onReleaseClick = viewModel::openArtistRelease,
                                onPlaylistClick = viewModel::openArtistPlaylist,
                            )
                        }

                        null -> Unit
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.artistSections(
    detail: ArtistDetail,
    onReleaseClick: (ArtistReleaseSummary) -> Unit,
    onPlaylistClick: (ArtistPlaylistSummary) -> Unit,
) {
    if (detail.albums.isNotEmpty()) {
        item {
            DetailSectionHeader("Albums")
        }
        item {
            ReleaseRow(items = detail.albums, onClick = onReleaseClick)
        }
    }

    if (detail.singlesAndEps.isNotEmpty()) {
        item {
            DetailSectionHeader("Singles & EPs")
        }
        item {
            ReleaseRow(items = detail.singlesAndEps, onClick = onReleaseClick)
        }
    }

    if (detail.featuredOn.isNotEmpty()) {
        item {
            DetailSectionHeader("Featured On")
        }
        item {
            ReleaseRow(items = detail.featuredOn, onClick = onReleaseClick)
        }
    }

    if (detail.curatedPlaylists.isNotEmpty()) {
        item {
            DetailSectionHeader("Curated Playlists")
        }
        item {
            PlaylistRow(items = detail.curatedPlaylists, onClick = onPlaylistClick)
        }
    }

    detail.bio?.let { bio ->
        item {
            DetailSectionHeader("Bio")
        }
        item {
            ArtistBioCard(bio = bio)
        }
    }
}

@Composable
private fun DetailBackButton(onBack: () -> Unit) {
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
                .clip(SquircleShape(16.dp))
                .clickable(onClick = onBack)
                .padding(4.dp),
        )
    }
}

@Composable
private fun CollectionHeader(
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
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .size(140.dp)
                    .clip(SquircleShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

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

                Row(
                    modifier = Modifier
                        .clip(SquircleShape(24.dp))
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
private fun ArtistHeader(detail: ArtistDetail) {
    val context = LocalContext.current
    val metadataLine = buildList {
        if (detail.genres.isNotEmpty()) {
            add(detail.genres.take(2).joinToString(", "))
        }
        detail.followersTotal?.let { add("${formatCount(it)} followers") }
        detail.popularity?.let { add("Popularity $it") }
    }.joinToString(" • ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF253240),
                        BrowseBackground,
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(detail.artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = detail.name,
                modifier = Modifier
                    .size(156.dp)
                    .clip(SquircleShape(18.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Artist",
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = detail.name,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    ),
                )
                if (metadataLine.isNotBlank()) {
                    Text(
                        text = metadataLine,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ReleaseRow(
    items: List<ArtistReleaseSummary>,
    onClick: (ArtistReleaseSummary) -> Unit,
) {
    LazyRow(
        modifier = Modifier.bounceOverscroll(orientation = Orientation.Horizontal),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            MediaCard(
                title = item.title,
                subtitle = item.subtitle,
                artworkUrl = item.artworkUrl,
                meta = item.totalTracks?.let { "$it tracks" },
                onClick = { onClick(item) },
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    items: List<ArtistPlaylistSummary>,
    onClick: (ArtistPlaylistSummary) -> Unit,
) {
    LazyRow(
        modifier = Modifier.bounceOverscroll(orientation = Orientation.Horizontal),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            MediaCard(
                title = item.title,
                subtitle = item.subtitle,
                artworkUrl = item.artworkUrl,
                meta = "Playlist",
                onClick = { onClick(item) },
            )
        }
    }
}

@Composable
private fun MediaCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    meta: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(152.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(SquircleShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                color = SpotifyGreen.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ArtistBioCard(bio: ArtistBio) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
    ) {
        Text(
            text = bio.summary,
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium,
        )
        bio.sourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
            Text(
                text = url,
                color = SpotifyGreen.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
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
            .clip(SquircleShape(10.dp))
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

private fun formatCount(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
        value >= 1_000 -> String.format("%.1fK", value / 1_000f)
        else -> value.toString()
    }
}
