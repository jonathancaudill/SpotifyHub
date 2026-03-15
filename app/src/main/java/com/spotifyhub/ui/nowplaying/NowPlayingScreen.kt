package com.spotifyhub.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.spotifyhub.R

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    isOffline: Boolean,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val playback = uiState.playback

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (playback?.item?.artworkUrl != null) {
                Card {
                    AsyncImage(
                        model = playback.item.artworkUrl,
                        contentDescription = playback.item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.now_playing_empty_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.now_playing_empty_body),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            playback?.item?.let { item ->
                Text(item.title, style = MaterialTheme.typography.headlineLarge)
                Text(item.artist, style = MaterialTheme.typography.titleMedium)
                Text(item.album, style = MaterialTheme.typography.bodyLarge)
            }

            playback?.device?.let { device ->
                Text(
                    text = "Playing on ${device.name}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (isOffline) {
                Text(
                    text = "Offline. Playback data will refresh when network returns.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        FloatingActionButton(
            onClick = viewModel::refresh,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh playback",
            )
        }
    }
}
