package com.spotifyhub.ui.root

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotifyhub.ui.auth.AuthScreen
import com.spotifyhub.ui.auth.AuthViewModel
import com.spotifyhub.ui.nowplaying.NowPlayingScreen
import com.spotifyhub.ui.nowplaying.PlayerViewModel

@Composable
fun RootScreen(
    rootViewModel: RootViewModel,
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
) {
    val rootState = rootViewModel.uiState.collectAsStateWithLifecycle().value

    when (rootState.destination) {
        RootDestination.Auth -> AuthScreen(
            viewModel = authViewModel,
            isOffline = rootState.isOffline,
        )

        RootDestination.NowPlaying -> NowPlayingScreen(
            viewModel = playerViewModel,
            isOffline = rootState.isOffline,
        )
    }
}

