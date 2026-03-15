package com.spotifyhub.ui.root

import com.spotifyhub.auth.SessionState

enum class RootDestination {
    Auth,
    NowPlaying,
}

data class RootUiState(
    val sessionState: SessionState,
    val destination: RootDestination,
    val isOffline: Boolean,
)

