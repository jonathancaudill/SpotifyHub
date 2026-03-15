package com.spotifyhub.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.playback.PlaybackRepository
import com.spotifyhub.playback.model.PlaybackSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PlayerUiState(
    val playback: PlaybackSnapshot?,
    val isRefreshing: Boolean,
)

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {
    val uiState = combine(
        playbackRepository.playbackState,
        playbackRepository.isRefreshing,
    ) { playback, isRefreshing ->
        PlayerUiState(
            playback = playback,
            isRefreshing = isRefreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(
            playback = null,
            isRefreshing = true,
        ),
    )

    fun refresh() {
        playbackRepository.refreshNow()
    }
}

