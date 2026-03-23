package com.spotifyhub.ui.library

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
import androidx.compose.foundation.lazy.rememberLazyListState
import sv.lib.squircleshape.SquircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spotifyhub.library.model.LibraryItem
import com.spotifyhub.library.model.LibraryItemType
import com.spotifyhub.ui.common.NowPlayingIndicator
import com.spotifyhub.ui.common.bounceOverscroll
import com.spotifyhub.ui.detail.DetailViewModel

private val BrowseBackground = Color(0xFF171A1F)
private val SpotifyGreen = Color(0xFF1ED760)
private val ChipBackground = Color(0xFF2A2A2A)
private val ChipSelectedBackground = Color(0xFF3A3A3A)

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    detailViewModel: DetailViewModel,
    currentTrackId: String?,
    isPlaybackActive: Boolean,
    onNavigateToDetail: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowseBackground),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        /* Category chips */
        Text(
            text = "Your Library",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.bounceOverscroll(orientation = Orientation.Horizontal),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val categories = listOf(
                LibraryCategory.Playlists to "Playlists",
                LibraryCategory.Albums to "Albums",
                LibraryCategory.Tracks to "Liked Songs",
            )
            items(categories, key = { it.first }) { (category, label) ->
                CategoryChip(
                    label = label,
                    isSelected = uiState.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        /* Content */
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error ?: "Something went wrong",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nothing here yet",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                val listState = rememberLazyListState()

                /* Load more when near the end */
                LaunchedEffect(listState) {
                    snapshotFlow {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = listState.layoutInfo.totalItemsCount
                        lastVisible >= totalItems - 5
                    }.collect { nearEnd ->
                        if (nearEnd) {
                            viewModel.loadMore()
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .bounceOverscroll(orientation = Orientation.Vertical),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        LibraryItemRow(
                            item = item,
                            isNowPlaying = isPlaybackActive &&
                                item.type == LibraryItemType.Track &&
                                currentTrackId == item.id,
                            onClick = {
                                when (item.type) {
                                    LibraryItemType.Playlist -> {
                                        detailViewModel.loadPlaylist(item.id)
                                        onNavigateToDetail()
                                    }

                                    LibraryItemType.Album -> {
                                        detailViewModel.loadAlbum(
                                            albumId = item.id,
                                            albumName = item.name,
                                            artistName = item.subtitle,
                                            artworkUrl = item.artworkUrl,
                                            albumUri = item.uri,
                                        )
                                        onNavigateToDetail()
                                    }

                                    LibraryItemType.Track -> {
                                        detailViewModel.playTrack(item.uri)
                                    }
                                }
                            },
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = SpotifyGreen,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(SquircleShape(20.dp))
            .background(if (isSelected) ChipSelectedBackground else ChipBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun LibraryItemRow(
    item: LibraryItem,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape(8.dp))
            .background(if (isNowPlaying) SpotifyGreen.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.type == LibraryItemType.Track) {
            Box(
                modifier = Modifier.width(20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (isNowPlaying) {
                    NowPlayingIndicator(modifier = Modifier.width(12.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            modifier = Modifier
                .size(52.dp)
                .clip(SquircleShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = if (isNowPlaying) SpotifyGreen else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
            Row {
                Text(
                    text = buildString {
                        append(
                            when (item.type) {
                                LibraryItemType.Playlist -> "Playlist"
                                LibraryItemType.Album -> "Album"
                                LibraryItemType.Track -> "Song"
                            },
                        )
                        if (item.subtitle.isNotBlank()) {
                            append(" \u2022 ")
                            append(item.subtitle)
                        }
                        item.trackCount?.let {
                            append(" \u2022 $it tracks")
                        }
                    },
                    color = if (isNowPlaying) SpotifyGreen.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (item.type != LibraryItemType.Track) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
