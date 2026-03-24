package com.spotifyhub.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.playback.PlaybackRepository
import com.spotifyhub.playback.model.PlaybackSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PlayerUiState(
    val playback: PlaybackSnapshot?,
    val isCurrentItemSaved: Boolean?,
)

data class PlayerShellState(
    val artworkUrl: String?,
    val artworkKey: String?,
    val currentTrackId: String?,
    val isPlaybackActive: Boolean,
)

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {
    val uiState = combine(
        playbackRepository.playbackState,
        playbackRepository.currentItemSaved,
    ) { playback, isCurrentItemSaved ->
        PlayerUiState(
            playback = playback,
            isCurrentItemSaved = isCurrentItemSaved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(
            playback = null,
            isCurrentItemSaved = null,
        ),
    )

    val shellState = playbackRepository.playbackState
        .map { playback ->
            PlayerShellState(
                artworkUrl = playback?.item?.artworkUrl,
                artworkKey = playback?.item?.id,
                currentTrackId = playback?.item?.id,
                isPlaybackActive = playback?.isPlaying == true,
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerShellState(
                artworkUrl = null,
                artworkKey = null,
                currentTrackId = null,
                isPlaybackActive = false,
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
