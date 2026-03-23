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
    val isCurrentItemSaved: Boolean?,
    val isRefreshing: Boolean,
)

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {
    val uiState = combine(
        playbackRepository.playbackState,
        playbackRepository.currentItemSaved,
        playbackRepository.isRefreshing,
    ) { playback, isCurrentItemSaved, isRefreshing ->
        PlayerUiState(
            playback = playback,
            isCurrentItemSaved = isCurrentItemSaved,
            isRefreshing = isRefreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(
            playback = null,
            isCurrentItemSaved = null,
            isRefreshing = true,
        ),
    )

    fun refresh() {
        playbackRepository.refreshNow()
    }

    fun togglePlayback() {
        playbackRepository.togglePlayback()
    }

    fun skipNext() {
        playbackRepository.skipNext()
    }

    fun skipPrevious() {
        playbackRepository.skipPrevious()
    }

    fun toggleSaveCurrentItem() {
        playbackRepository.toggleSaveCurrentItem()
    }

    fun toggleShuffle() {
        playbackRepository.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playbackRepository.cycleRepeatMode()
    }

    fun adjustVolume(deltaPercent: Int) {
        playbackRepository.adjustVolume(deltaPercent)
    }

    fun seekTo(positionMs: Long) {
        playbackRepository.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        playbackRepository.seekBy(deltaMs)
    }
}
