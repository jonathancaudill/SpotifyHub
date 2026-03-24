package com.spotifyhub.ui.search

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
import androidx.compose.foundation.lazy.items
import sv.lib.squircleshape.SquircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.search.model.SearchSection
import com.spotifyhub.ui.icons.AppIcons
import com.spotifyhub.ui.common.NowPlayingIndicator
import com.spotifyhub.ui.common.bounceOverscroll

private val BrowseBackground = Color(0xFF171A1F)
private val SearchBarBackground = Color(0xFF2A2A2A)
private val SpotifyGreen = Color(0xFF1ED760)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    currentTrackId: String?,
    isPlaybackActive: Boolean,
    onItemClick: (BrowseItem) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowseBackground)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        /* Search bar */
        SearchBar(
            query = uiState.query,
            onQueryChange = viewModel::updateQuery,
        )

        Spacer(modifier = Modifier.height(16.dp))

        /* Results */
        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }

            uiState.query.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Search for songs, artists, albums, or playlists",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Search is temporarily unavailable. ${uiState.error}",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }

            uiState.results != null && uiState.results!!.isEmpty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found for \"${uiState.query}\"",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            uiState.results != null -> {
                val results = uiState.results!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .bounceOverscroll(orientation = Orientation.Vertical),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    results.sectionOrder.forEach { section ->
                        when (section) {
                            SearchSection.Tracks -> {
                                item {
                                    SectionHeader("Songs")
                                }
                                items(results.tracks, key = { "track-${it.id}" }) { item ->
                                    SearchResultRow(
                                        item = item,
                                        isNowPlaying = isPlaybackActive && currentTrackId == item.id,
                                        onClick = { onItemClick(item) },
                                    )
                                }
                            }

                            SearchSection.Artists -> {
                                item {
                                    SectionHeader("Artists")
                                }
                                items(results.artists, key = { "artist-${it.id}" }) { item ->
                                    SearchResultRow(item = item, isNowPlaying = false, onClick = { onItemClick(item) })
                                }
                            }

                            SearchSection.Albums -> {
                                item {
                                    SectionHeader("Albums")
                                }
                                items(results.albums, key = { "album-${it.id}" }) { item ->
                                    SearchResultRow(item = item, isNowPlaying = false, onClick = { onItemClick(item) })
                                }
                            }

                            SearchSection.Playlists -> {
                                item {
                                    SectionHeader("Playlists")
                                }
                                items(results.playlists, key = { "playlist-${it.id}" }) { item ->
                                    SearchResultRow(item = item, isNowPlaying = false, onClick = { onItemClick(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(SquircleShape(22.dp))
            .background(SearchBarBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.search,
            contentDescription = "Search",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "What do you want to listen to?",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontSize = 15.sp,
                ),
                singleLine = true,
                cursorBrush = SolidColor(SpotifyGreen),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        ),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SearchResultRow(
    item: BrowseItem,
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        if (item.type == BrowseItemType.Artist) {
                            androidx.compose.foundation.shape.CircleShape
                        } else {
                            SquircleShape(6.dp)
                        },
                    ),
                contentScale = ContentScale.Crop,
            )
            if (item.type == BrowseItemType.Track && isNowPlaying) {
                NowPlayingIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .width(12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = if (isNowPlaying) SpotifyGreen else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = buildString {
                        append(
                            when (item.type) {
                                BrowseItemType.Track -> "Song"
                                BrowseItemType.Album -> "Album"
                                BrowseItemType.Artist -> "Artist"
                                BrowseItemType.Playlist -> "Playlist"
                            },
                        )
                        append(" \u2022 ")
                        append(item.subtitle)
                    },
                    color = if (isNowPlaying) SpotifyGreen.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
