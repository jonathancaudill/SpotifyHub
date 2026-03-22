package com.spotifyhub.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.auth.SessionState
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.system.network.ConnectivityMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RootViewModel(
    private val authRepository: SpotifyAuthRepository,
    connectivityMonitor: ConnectivityMonitor,
) : ViewModel() {
    val uiState = combine(
        authRepository.sessionState,
        connectivityMonitor.isConnected,
    ) { sessionState, isConnected ->
        RootUiState(
            sessionState = sessionState,
            destination = if (sessionState is SessionState.Ready) {
                RootDestination.Main
            } else {
                RootDestination.Auth
            },
            isOffline = !isConnected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootUiState(
            sessionState = SessionState.Loading,
            destination = RootDestination.Auth,
            isOffline = false,
        ),
    )

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
        }
    }
}

